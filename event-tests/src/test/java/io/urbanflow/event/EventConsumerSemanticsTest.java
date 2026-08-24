package io.urbanflow.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.urbanflow.event.support.EventFixtures;
import io.urbanflow.event.support.EventFixtures.EventEnvelope;
import io.urbanflow.event.support.EventProjectionHarness;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Consumer behavior coverage beyond the serialized event contract. */
@Tag("event")
@Epic("Event-driven delivery platform")
@Feature("Consumer resilience and consistency")
class EventConsumerSemanticsTest {

  @Test
  void duplicateEventIsProcessedOnce() {
    EventProjectionHarness harness = new EventProjectionHarness();
    EventEnvelope event = EventFixtures.event("ShipmentCreated");

    assertThat(harness.process(event)).isTrue();
    assertThat(harness.process(event)).isFalse();
    assertThat(harness.timeline(event.aggregateId())).hasSize(1);
  }

  @Test
  void outOfOrderDeliveryProjectsByAggregateVersion() {
    EventProjectionHarness harness = new EventProjectionHarness();
    EventEnvelope first = version(EventFixtures.event("ShipmentCreated"), 1);
    EventEnvelope third = version(first, 3);
    EventEnvelope second = version(first, 2);

    harness.process(third);
    harness.process(first);
    harness.process(second);

    assertThat(harness.timeline(first.aggregateId()))
        .extracting(EventEnvelope::aggregateVersion)
        .containsExactly(1L, 2L, 3L);
  }

  @Test
  void transientFailureIsRetriedThenProcessed() {
    EventProjectionHarness harness = new EventProjectionHarness();
    EventEnvelope event = EventFixtures.event("ShipmentConfirmed");

    assertThat(harness.processWithFailures(event, 2)).isTrue();
    assertThat(harness.attempts(event.eventId())).isEqualTo(3);
    assertThat(harness.deadLetters()).isEmpty();
  }

  @Test
  void exhaustedRetryMovesEventToDeadLetter() {
    EventProjectionHarness harness = new EventProjectionHarness();
    EventEnvelope event = EventFixtures.event("NotificationRequested");

    assertThat(harness.processWithFailures(event, 3)).isFalse();
    assertThat(harness.deadLetters()).singleElement().asString().startsWith("RETRY_EXHAUSTED:");
  }

  @Test
  void malformedEventMovesToDeadLetter() {
    EventProjectionHarness harness = new EventProjectionHarness();

    harness.processRaw("not-json");

    assertThat(harness.deadLetters()).containsExactly("MALFORMED_EVENT");
  }

  @Test
  void unsupportedSchemaMovesToDeadLetter() {
    EventProjectionHarness harness = new EventProjectionHarness();
    EventEnvelope event = schema(EventFixtures.event("ShipmentCreated"), "2.0");

    assertThat(harness.process(event)).isFalse();
    assertThat(harness.deadLetters()).singleElement().asString().startsWith("UNSUPPORTED_SCHEMA:");
  }

  @Test
  void delayedProcessingBecomesEventuallyConsistent() {
    EventProjectionHarness harness = new EventProjectionHarness();
    EventEnvelope event = EventFixtures.event("CourierAssigned");

    harness.processLater(event, Duration.ofMillis(25));

    await()
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(() -> assertThat(harness.timeline(event.aggregateId())).hasSize(1));
  }

  @Test
  void consumerRestartRetainsIdempotencyCheckpoint() {
    EventEnvelope event = EventFixtures.event("ShipmentPickedUp");
    EventProjectionHarness firstProcess = new EventProjectionHarness();
    firstProcess.process(event);
    EventProjectionHarness restarted = new EventProjectionHarness();

    restarted.restore(firstProcess.checkpoint());

    assertThat(restarted.process(event)).isFalse();
  }

  @Test
  void differentEventIdsAtSameVersionRemainIdempotentByEvent() {
    EventProjectionHarness harness = new EventProjectionHarness();
    EventEnvelope event = EventFixtures.event("ShipmentConfirmed");
    EventEnvelope redelivery =
        new EventEnvelope(
            java.util.UUID.randomUUID(),
            event.eventType(),
            event.aggregateId(),
            event.aggregateVersion(),
            event.occurredAt(),
            event.schemaVersion(),
            event.correlationId(),
            event.traceId(),
            event.producer(),
            event.payload());

    assertThat(harness.process(event)).isTrue();
    assertThat(harness.process(redelivery)).isTrue();
    assertThat(harness.timeline(event.aggregateId())).hasSize(1);
  }

  @Test
  void aggregateStreamsRemainIndependent() {
    EventProjectionHarness harness = new EventProjectionHarness();
    EventEnvelope left = EventFixtures.event("ShipmentCreated");
    EventEnvelope right = EventFixtures.event("ShipmentCreated");

    harness.process(left);
    harness.process(right);

    assertThat(harness.timeline(left.aggregateId())).containsExactly(left);
    assertThat(harness.timeline(right.aggregateId())).containsExactly(right);
  }

  private static EventEnvelope version(EventEnvelope source, long version) {
    return new EventEnvelope(
        version == source.aggregateVersion() ? source.eventId() : java.util.UUID.randomUUID(),
        source.eventType(),
        source.aggregateId(),
        version,
        source.occurredAt(),
        source.schemaVersion(),
        source.correlationId(),
        source.traceId(),
        source.producer(),
        Map.copyOf(source.payload()));
  }

  private static EventEnvelope schema(EventEnvelope source, String schema) {
    return new EventEnvelope(
        source.eventId(),
        source.eventType(),
        source.aggregateId(),
        source.aggregateVersion(),
        source.occurredAt(),
        schema,
        source.correlationId(),
        source.traceId(),
        source.producer(),
        source.payload());
  }
}
