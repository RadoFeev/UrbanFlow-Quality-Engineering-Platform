package io.urbanflow.routing.event;

import io.micrometer.core.instrument.MeterRegistry;
import io.urbanflow.routing.config.RoutingOutboxProperties;
import io.urbanflow.routing.persistence.RoutingOutboxEvent;
import io.urbanflow.routing.persistence.RoutingOutboxRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Publishes routing outbox events with ordering keys, headers, acknowledgments, and retry. */
@Component
public class RoutingOutboxPublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(RoutingOutboxPublisher.class);

  private final RoutingOutboxRepository outbox;
  private final KafkaTemplate<String, String> kafka;
  private final ObjectMapper objectMapper;
  private final RoutingOutboxProperties properties;
  private final Clock clock;
  private final MeterRegistry meters;
  private final AtomicBoolean running = new AtomicBoolean();

  /** Creates the routing event publication worker. */
  public RoutingOutboxPublisher(
      RoutingOutboxRepository outbox,
      KafkaTemplate<String, String> kafka,
      ObjectMapper objectMapper,
      RoutingOutboxProperties properties,
      Clock clock,
      MeterRegistry meters) {
    this.outbox = outbox;
    this.kafka = kafka;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.clock = clock;
    this.meters = meters;
  }

  /** Publishes one due bounded batch while suppressing local schedule overlap. */
  @Scheduled(fixedDelayString = "${urbanflow.routing.outbox.poll-delay:PT1S}")
  public int publishDue() {
    if (!running.compareAndSet(false, true)) {
      return 0;
    }
    try {
      return outbox
          .findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(
              clock.instant())
          .stream()
          .mapToInt(this::publishOne)
          .sum();
    } finally {
      running.set(false);
    }
  }

  private int publishOne(RoutingOutboxEvent event) {
    Instant now = clock.instant();
    try {
      ProducerRecord<String, String> record =
          new ProducerRecord<>(properties.topic(), event.shipmentId().toString(), envelope(event));
      addHeader(record, "eventId", event.id().toString());
      addHeader(record, "correlationId", event.correlationId());
      addHeader(record, "traceId", event.traceId());
      addHeader(record, "schemaVersion", "1.0");
      kafka.send(record).get(properties.acknowledgmentTimeout().toMillis(), TimeUnit.MILLISECONDS);
      event.markPublished(now);
      outbox.save(event);
      meters.counter("urbanflow.routing.outbox", "result", "published").increment();
      return 1;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      fail(event, now, exception);
      return 0;
    } catch (Exception exception) {
      fail(event, now, exception);
      return 0;
    }
  }

  private String envelope(RoutingOutboxEvent event) throws Exception {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("eventId", event.id());
    envelope.put("eventType", event.eventType());
    envelope.put("aggregateId", event.shipmentId());
    envelope.put("occurredAt", event.occurredAt());
    envelope.put("schemaVersion", "1.0");
    envelope.put("correlationId", event.correlationId());
    envelope.put("traceId", event.traceId());
    envelope.put("producer", "routing-service");
    envelope.put("payload", objectMapper.readTree(event.payload()));
    return objectMapper.writeValueAsString(envelope);
  }

  private void fail(RoutingOutboxEvent event, Instant now, Exception exception) {
    event.markFailed(now);
    outbox.save(event);
    meters.counter("urbanflow.routing.outbox", "result", "failed").increment();
    LOGGER.warn(
        "Routing event publication failed eventId={} attempt={} errorType={}",
        event.id(),
        event.attempts(),
        exception.getClass().getSimpleName());
  }

  private static void addHeader(ProducerRecord<String, String> record, String name, String value) {
    record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
  }
}
