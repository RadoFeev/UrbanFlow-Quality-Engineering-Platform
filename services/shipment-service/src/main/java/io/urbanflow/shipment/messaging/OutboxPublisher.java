package io.urbanflow.shipment.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Publishes leased outbox events to Kafka with at-least-once delivery semantics. */
@Service
@ConditionalOnProperty(
    prefix = "urbanflow.outbox",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OutboxPublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisher.class);
  private static final String PRODUCER = "shipment-service";

  private final OutboxStateService stateService;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final OutboxPublicationProperties properties;
  private final OutboxRetryPolicy retryPolicy;
  private final Clock clock;
  private final String workerId;
  private final AtomicBoolean polling = new AtomicBoolean();
  private final Counter publishedCounter;
  private final Counter failedCounter;
  private final Timer publicationTimer;

  OutboxPublisher(
      OutboxStateService stateService,
      KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper objectMapper,
      OutboxPublicationProperties properties,
      Clock clock,
      @Qualifier("outboxWorkerId") String workerId,
      MeterRegistry meterRegistry) {
    this.stateService = stateService;
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.retryPolicy = new OutboxRetryPolicy(properties);
    this.clock = clock;
    this.workerId = workerId;
    publishedCounter =
        Counter.builder("urbanflow.outbox.publications")
            .tag("result", "published")
            .register(meterRegistry);
    failedCounter =
        Counter.builder("urbanflow.outbox.publications")
            .tag("result", "failed")
            .register(meterRegistry);
    publicationTimer =
        Timer.builder("urbanflow.outbox.publication.duration").register(meterRegistry);
  }

  @Scheduled(fixedDelayString = "${urbanflow.outbox.poll-interval:1s}")
  void publishScheduled() {
    publishAvailable();
  }

  /** Claims and publishes one available batch, returning its attempted event count. */
  public int publishAvailable() {
    if (!polling.compareAndSet(false, true)) {
      return 0;
    }
    try {
      Instant claimTime = clock.instant();
      List<OutboxEventMessage> messages =
          stateService.claimBatch(
              workerId, claimTime, properties.claimLease(), properties.batchSize());
      List<PendingPublication> pending = new ArrayList<>(messages.size());
      for (OutboxEventMessage message : messages) {
        PendingPublication publication = dispatch(message);
        if (publication != null) {
          pending.add(publication);
        }
      }
      boolean interrupted = false;
      for (PendingPublication publication : pending) {
        interrupted |= complete(publication);
      }
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
      return messages.size();
    } finally {
      polling.set(false);
    }
  }

  private PendingPublication dispatch(OutboxEventMessage message) {
    Timer.Sample sample = Timer.start();
    try {
      String envelope = serialize(message);
      ProducerRecord<String, String> record = producerRecord(message, envelope);
      CompletableFuture<SendResult<String, String>> result = kafkaTemplate.send(record);
      long deadline = System.nanoTime() + properties.publishTimeout().toNanos();
      return new PendingPublication(message, result, deadline, sample);
    } catch (Exception exception) {
      recordFailure(message, exception);
      sample.stop(publicationTimer);
      return null;
    }
  }

  private boolean complete(PendingPublication publication) {
    try {
      long remainingNanos = publication.deadlineNanos() - System.nanoTime();
      if (remainingNanos <= 0 && !publication.result().isDone()) {
        throw new TimeoutException("Kafka acknowledgment deadline elapsed");
      }
      publication.result().get(Math.max(1, remainingNanos), TimeUnit.NANOSECONDS);
      stateService.markPublished(publication.message().eventId(), workerId, clock.instant());
      publishedCounter.increment();
      return false;
    } catch (InterruptedException exception) {
      recordFailure(publication.message(), exception);
      return true;
    } catch (Exception exception) {
      recordFailure(publication.message(), exception);
      return false;
    } finally {
      publication.sample().stop(publicationTimer);
    }
  }

  private String serialize(OutboxEventMessage message) throws Exception {
    JsonNode payload = objectMapper.readTree(message.payload());
    ShipmentEventEnvelope envelope =
        new ShipmentEventEnvelope(
            message.eventId(),
            message.eventType(),
            message.aggregateId(),
            message.aggregateVersion(),
            message.occurredAt().toString(),
            message.schemaVersion(),
            message.correlationId(),
            message.traceId(),
            PRODUCER,
            payload);
    return objectMapper.writeValueAsString(envelope);
  }

  private ProducerRecord<String, String> producerRecord(
      OutboxEventMessage message, String envelope) {
    ProducerRecord<String, String> record =
        new ProducerRecord<>(properties.topic(), message.aggregateId().toString(), envelope);
    addHeader(record, "event-id", message.eventId().toString());
    addHeader(record, "event-type", message.eventType());
    addHeader(record, "schema-version", message.schemaVersion());
    addHeader(record, "correlation-id", message.correlationId());
    addHeader(record, "trace-id", message.traceId());
    return record;
  }

  private static void addHeader(ProducerRecord<String, String> record, String name, String value) {
    record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
  }

  private void recordFailure(OutboxEventMessage message, Exception exception) {
    int completedAttempts = message.publicationAttempts() + 1;
    Duration retryDelay = retryPolicy.delayAfter(completedAttempts);
    Instant failedAt = clock.instant();
    Instant retryAt = failedAt.plus(retryDelay);
    String failureSummary = failureSummary(exception);
    try {
      stateService.markFailed(message.eventId(), workerId, failedAt, retryAt, failureSummary);
    } catch (RuntimeException stateFailure) {
      LOGGER.error(
          "Failed to release outbox lease eventId={} workerId={} errorType={}",
          message.eventId(),
          workerId,
          stateFailure.getClass().getSimpleName());
    }
    failedCounter.increment();
    LOGGER.warn(
        "Outbox publication failed eventId={} attempt={} retryAt={} errorType={}",
        message.eventId(),
        completedAttempts,
        retryAt,
        failureSummary);
  }

  private static String failureSummary(Exception exception) {
    Throwable root = exception;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    return root.getClass().getSimpleName();
  }

  private record PendingPublication(
      OutboxEventMessage message,
      CompletableFuture<SendResult<String, String>> result,
      long deadlineNanos,
      Timer.Sample sample) {}
}
