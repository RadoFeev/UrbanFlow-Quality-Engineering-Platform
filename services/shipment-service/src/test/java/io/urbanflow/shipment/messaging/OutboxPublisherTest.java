package io.urbanflow.shipment.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

  private static final Instant NOW = Instant.parse("2026-08-24T11:00:00Z");
  private static final UUID EVENT_ID = UUID.fromString("793b1e6c-15d5-4058-a363-c9a6c573a276");
  private static final UUID AGGREGATE_ID = UUID.fromString("582cc681-3791-4866-bea1-2058ddf59c5a");
  private static final String TOPIC = "urbanflow.shipment.events.v1";
  private static final String WORKER = "shipment-worker-test";

  @Mock private OutboxStateService stateService;
  @Mock private KafkaTemplate<String, String> kafkaTemplate;

  private ObjectMapper objectMapper;
  private SimpleMeterRegistry meterRegistry;
  private OutboxPublisher publisher;

  @BeforeEach
  void setUp() {
    objectMapper = JsonMapper.builder().build();
    meterRegistry = new SimpleMeterRegistry();
    publisher =
        new OutboxPublisher(
            stateService,
            kafkaTemplate,
            objectMapper,
            properties(),
            Clock.fixed(NOW, ZoneOffset.UTC),
            WORKER,
            meterRegistry);
  }

  @Test
  void publishesStableEnvelopeWithAggregateKeyAndMarksAcknowledged() throws Exception {
    when(stateService.claimBatch(WORKER, NOW, Duration.ofSeconds(30), 50))
        .thenReturn(List.of(message(EVENT_ID, AGGREGATE_ID, 0, validPayload())));
    when(kafkaTemplate.send(anyProducerRecord())).thenReturn(successfulSend());

    int attempted = publisher.publishAvailable();

    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.captor();
    verify(kafkaTemplate).send(recordCaptor.capture());
    ProducerRecord<String, String> record = recordCaptor.getValue();
    JsonNode json = objectMapper.readTree(record.value());
    assertThat(attempted).isOne();
    assertThat(record.topic()).isEqualTo(TOPIC);
    assertThat(record.key()).isEqualTo(AGGREGATE_ID.toString());
    assertThat(header(record, "event-id")).isEqualTo(EVENT_ID.toString());
    assertThat(header(record, "event-type")).isEqualTo("ShipmentCreated");
    assertThat(header(record, "schema-version")).isEqualTo("1.0");
    assertThat(header(record, "correlation-id")).isEqualTo("corr-outbox-003");
    assertThat(header(record, "trace-id")).isEqualTo("fedcba9876543210fedcba9876543210");
    assertThat(json.get("eventId").asString()).isEqualTo(EVENT_ID.toString());
    assertThat(json.get("eventType").asString()).isEqualTo("ShipmentCreated");
    assertThat(json.get("aggregateId").asString()).isEqualTo(AGGREGATE_ID.toString());
    assertThat(json.get("aggregateVersion").asLong()).isEqualTo(4L);
    assertThat(json.get("occurredAt").asString()).isEqualTo(NOW.minusSeconds(5).toString());
    assertThat(json.get("schemaVersion").asString()).isEqualTo("1.0");
    assertThat(json.get("producer").asString()).isEqualTo("shipment-service");
    assertThat(json.get("payload").get("status").asString()).isEqualTo("CREATED");
    verify(stateService).markPublished(EVENT_ID, WORKER, NOW);
    assertThat(publicationCount("published")).isEqualTo(1.0);
    assertThat(publicationCount("failed")).isZero();
  }

  @Test
  void schedulesCappedRetryWhenBrokerAcknowledgmentFails() {
    OutboxEventMessage message = message(EVENT_ID, AGGREGATE_ID, 3, validPayload());
    when(stateService.claimBatch(WORKER, NOW, Duration.ofSeconds(30), 50))
        .thenReturn(List.of(message));
    when(kafkaTemplate.send(anyProducerRecord()))
        .thenReturn(failedSend(new IllegalStateException("broker unavailable")));

    assertThat(publisher.publishAvailable()).isOne();

    verify(stateService)
        .markFailed(
            eq(EVENT_ID), eq(WORKER), eq(NOW), eq(NOW.plusSeconds(8)), eq("IllegalStateException"));
    verify(stateService, never()).markPublished(EVENT_ID, WORKER, NOW);
    assertThat(publicationCount("failed")).isEqualTo(1.0);
  }

  @Test
  void treatsMalformedStoredPayloadAsRetryableWithoutCallingKafka() {
    when(stateService.claimBatch(WORKER, NOW, Duration.ofSeconds(30), 50))
        .thenReturn(List.of(message(EVENT_ID, AGGREGATE_ID, 0, "not-json")));

    assertThat(publisher.publishAvailable()).isOne();

    verify(kafkaTemplate, never()).send(anyProducerRecord());
    verify(stateService)
        .markFailed(eq(EVENT_ID), eq(WORKER), eq(NOW), eq(NOW.plusSeconds(1)), anyString());
  }

  @Test
  void continuesBatchAfterOneEventFails() {
    UUID secondEventId = UUID.fromString("c2d50b76-5100-4931-968f-71c3f3a1c9f5");
    UUID secondAggregateId = UUID.fromString("139ed2fc-a46f-4711-8857-1dc1674c07c0");
    when(stateService.claimBatch(WORKER, NOW, Duration.ofSeconds(30), 50))
        .thenReturn(
            List.of(
                message(EVENT_ID, AGGREGATE_ID, 0, "not-json"),
                message(secondEventId, secondAggregateId, 0, validPayload())));
    when(kafkaTemplate.send(anyProducerRecord())).thenReturn(successfulSend());

    assertThat(publisher.publishAvailable()).isEqualTo(2);

    verify(stateService)
        .markFailed(eq(EVENT_ID), eq(WORKER), eq(NOW), eq(NOW.plusSeconds(1)), anyString());
    verify(stateService).markPublished(secondEventId, WORKER, NOW);
    assertThat(publicationCount("published")).isEqualTo(1.0);
    assertThat(publicationCount("failed")).isEqualTo(1.0);
  }

  @Test
  void dispatchesCompleteBatchBeforeWaitingForAcknowledgments() throws Exception {
    UUID secondEventId = UUID.fromString("bf8aa942-bc37-457a-b74c-4453937d077a");
    UUID secondAggregateId = UUID.fromString("9eff5314-cdb3-4457-89cd-8db8e001a293");
    CompletableFuture<SendResult<String, String>> firstAcknowledgment = new CompletableFuture<>();
    CountDownLatch secondDispatched = new CountDownLatch(1);
    when(stateService.claimBatch(WORKER, NOW, Duration.ofSeconds(30), 50))
        .thenReturn(
            List.of(
                message(EVENT_ID, AGGREGATE_ID, 0, validPayload()),
                message(secondEventId, secondAggregateId, 0, validPayload())));
    when(kafkaTemplate.send(anyProducerRecord()))
        .thenAnswer(
            invocation -> {
              ProducerRecord<String, String> record = invocation.getArgument(0);
              String key = record.key();
              if (AGGREGATE_ID.toString().equals(key)) {
                return firstAcknowledgment;
              }
              secondDispatched.countDown();
              return successfulSend();
            });

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      final var polling = executor.submit(publisher::publishAvailable);
      boolean dispatchedBeforeFirstAck = secondDispatched.await(5, TimeUnit.SECONDS);
      firstAcknowledgment.complete(null);

      assertThat(dispatchedBeforeFirstAck).isTrue();
      assertThat(polling.get(5, TimeUnit.SECONDS)).isEqualTo(2);
    }
    verify(stateService).markPublished(EVENT_ID, WORKER, NOW);
    verify(stateService).markPublished(secondEventId, WORKER, NOW);
  }

  @Test
  void acceptsCompletedAcknowledgmentAfterLocalDeadlineElapsed() {
    OutboxPublisher nanosecondDeadlinePublisher =
        new OutboxPublisher(
            stateService,
            kafkaTemplate,
            objectMapper,
            properties(Duration.ofNanos(1)),
            Clock.fixed(NOW, ZoneOffset.UTC),
            WORKER,
            meterRegistry);
    when(stateService.claimBatch(WORKER, NOW, Duration.ofSeconds(30), 50))
        .thenReturn(List.of(message(EVENT_ID, AGGREGATE_ID, 0, validPayload())));
    when(kafkaTemplate.send(anyProducerRecord())).thenReturn(successfulSend());

    assertThat(nanosecondDeadlinePublisher.publishAvailable()).isOne();

    verify(stateService).markPublished(EVENT_ID, WORKER, NOW);
  }

  @Test
  void retriesWhenKafkaAckSucceedsButDatabaseCompletionFails() {
    when(stateService.claimBatch(WORKER, NOW, Duration.ofSeconds(30), 50))
        .thenReturn(List.of(message(EVENT_ID, AGGREGATE_ID, 0, validPayload())));
    when(kafkaTemplate.send(anyProducerRecord())).thenReturn(successfulSend());
    org.mockito.Mockito.doThrow(new IllegalStateException("database unavailable"))
        .when(stateService)
        .markPublished(EVENT_ID, WORKER, NOW);

    assertThat(publisher.publishAvailable()).isOne();

    verify(stateService)
        .markFailed(EVENT_ID, WORKER, NOW, NOW.plusSeconds(1), "IllegalStateException");
    assertThat(publicationCount("failed")).isEqualTo(1.0);
  }

  @Test
  void preventsOverlappingPollsWithinOneProcess() throws Exception {
    CountDownLatch enteredClaim = new CountDownLatch(1);
    CountDownLatch releaseClaim = new CountDownLatch(1);
    when(stateService.claimBatch(WORKER, NOW, Duration.ofSeconds(30), 50))
        .thenAnswer(
            invocation -> {
              enteredClaim.countDown();
              if (!releaseClaim.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test claim was not released");
              }
              return List.of();
            });

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      final var firstPoll = executor.submit(publisher::publishAvailable);
      assertThat(enteredClaim.await(5, TimeUnit.SECONDS)).isTrue();

      assertThat(publisher.publishAvailable()).isZero();

      releaseClaim.countDown();
      assertThat(firstPoll.get(5, TimeUnit.SECONDS)).isZero();
    }
  }

  @Test
  void releasesLocalGuardWhenClaimTransactionFails() {
    when(stateService.claimBatch(WORKER, NOW, Duration.ofSeconds(30), 50))
        .thenThrow(new IllegalStateException("database unavailable"))
        .thenReturn(List.of());

    assertThatThrownBy(publisher::publishAvailable)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("database unavailable");
    assertThat(publisher.publishAvailable()).isZero();
  }

  private double publicationCount(String result) {
    return meterRegistry
        .get("urbanflow.outbox.publications")
        .tag("result", result)
        .counter()
        .count();
  }

  private static String header(ProducerRecord<String, String> record, String name) {
    return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
  }

  private static ProducerRecord<String, String> anyProducerRecord() {
    return org.mockito.ArgumentMatchers.any();
  }

  private static OutboxPublicationProperties properties() {
    return properties(Duration.ofSeconds(10));
  }

  private static OutboxPublicationProperties properties(Duration publishTimeout) {
    return new OutboxPublicationProperties(
        true,
        TOPIC,
        50,
        Duration.ofSeconds(1),
        Duration.ofSeconds(30),
        publishTimeout,
        Duration.ofSeconds(1),
        Duration.ofSeconds(10));
  }

  private static OutboxEventMessage message(
      UUID eventId, UUID aggregateId, int attempts, String payload) {
    return new OutboxEventMessage(
        eventId,
        "ShipmentCreated",
        aggregateId,
        4,
        "1.0",
        payload,
        "corr-outbox-003",
        "fedcba9876543210fedcba9876543210",
        NOW.minusSeconds(5),
        attempts);
  }

  private static String validPayload() {
    return "{\"shipmentId\":\"" + AGGREGATE_ID + "\",\"status\":\"CREATED\",\"version\":4}";
  }

  private static CompletableFuture<SendResult<String, String>> successfulSend() {
    return CompletableFuture.completedFuture(null);
  }

  private static CompletableFuture<SendResult<String, String>> failedSend(Exception exception) {
    return CompletableFuture.failedFuture(exception);
  }
}
