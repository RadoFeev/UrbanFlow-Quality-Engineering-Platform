package io.urbanflow.shipment.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.urbanflow.shipment.domain.AddressSnapshot;
import io.urbanflow.shipment.domain.DeliveryType;
import io.urbanflow.shipment.domain.ParcelSnapshot;
import io.urbanflow.shipment.domain.Shipment;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

  private static final Instant OCCURRED_AT = Instant.parse("2026-08-24T10:00:00Z");
  private static final String WORKER = "shipment-worker-1";

  @Test
  void createsUnpublishedEventReadyForImmediateAttempt() {
    OutboxEvent event = event();

    assertThat(event.eventType()).isEqualTo("ShipmentCreated");
    assertThat(event.aggregateId()).isEqualTo(shipment().id());
    assertThat(event.schemaVersion()).isEqualTo("1.0");
    assertThat(event.payload()).contains("\"status\":\"CREATED\"");
    assertThat(event.publicationAttempts()).isZero();
    assertThat(event.nextAttemptAt()).isEqualTo(OCCURRED_AT);
    assertThat(event.publishedAt()).isNull();
    assertThat(event.claimedBy()).isNull();
  }

  @Test
  void marksAcknowledgedPublicationAndReleasesLease() {
    OutboxEvent event = event();
    event.claim(WORKER, OCCURRED_AT.plusSeconds(1));

    event.markPublished(WORKER, OCCURRED_AT.plusSeconds(2));

    assertThat(event.publishedAt()).isEqualTo(OCCURRED_AT.plusSeconds(2));
    assertThat(event.publicationAttempts()).isOne();
    assertThat(event.claimedAt()).isNull();
    assertThat(event.claimedBy()).isNull();
    assertThat(event.lastError()).isNull();
  }

  @Test
  void recordsFailureAndSchedulesRetryWithoutChangingEnvelope() {
    OutboxEvent event = event();
    UUID eventId = event.id();
    event.claim(WORKER, OCCURRED_AT.plusSeconds(1));

    event.markFailed(
        WORKER,
        OCCURRED_AT.plusSeconds(2),
        OCCURRED_AT.plusSeconds(7),
        "TimeoutException: broker acknowledgment timed out");

    assertThat(event.id()).isEqualTo(eventId);
    assertThat(event.publicationAttempts()).isOne();
    assertThat(event.nextAttemptAt()).isEqualTo(OCCURRED_AT.plusSeconds(7));
    assertThat(event.lastError()).startsWith("TimeoutException");
    assertThat(event.claimedAt()).isNull();
    assertThat(event.claimedBy()).isNull();
    assertThat(event.publishedAt()).isNull();
  }

  @Test
  void rejectsCompletionByWorkerThatDoesNotOwnLease() {
    OutboxEvent event = event();
    event.claim(WORKER, OCCURRED_AT.plusSeconds(1));

    assertThatThrownBy(() -> event.markPublished("shipment-worker-2", OCCURRED_AT.plusSeconds(2)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not owned");
    assertThat(event.publishedAt()).isNull();
    assertThat(event.publicationAttempts()).isZero();
  }

  @Test
  void rejectsRetryBoundaryBeforeFailureTime() {
    OutboxEvent event = event();
    event.claim(WORKER, OCCURRED_AT.plusSeconds(1));

    assertThatThrownBy(
            () ->
                event.markFailed(
                    WORKER, OCCURRED_AT.plusSeconds(5), OCCURRED_AT.plusSeconds(4), "Failure"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("retryAt");
    assertThat(event.publicationAttempts()).isZero();
  }

  private static OutboxEvent event() {
    return OutboxEvent.shipmentEvent(
        shipment(),
        "ShipmentCreated",
        "corr-outbox-001",
        "0123456789abcdef0123456789abcdef",
        OCCURRED_AT);
  }

  private static Shipment shipment() {
    return Shipment.create(
        UUID.fromString("fca568cc-4ac6-45f3-8f89-aed1d250d803"),
        UUID.fromString("664e07d7-c911-4cc0-bfbf-e82d4e9896cc"),
        "UF1123456789ABCDEF01",
        DeliveryType.EXPRESS,
        new AddressSnapshot(
            "Sender", "1 Origin Street", null, "Sofia", "1000", "BG", "+359888100001"),
        new AddressSnapshot(
            "Recipient", "2 Destination Street", null, "Varna", "9000", "BG", "+359888100002"),
        new ParcelSnapshot(900, 20, 15, 10, "Outbox test parcel"),
        OCCURRED_AT);
  }
}
