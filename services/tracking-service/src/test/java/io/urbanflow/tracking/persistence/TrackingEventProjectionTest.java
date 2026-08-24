package io.urbanflow.tracking.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.urbanflow.tracking.domain.TrackingStatus;
import io.urbanflow.tracking.event.ShipmentEventEnvelope;
import io.urbanflow.tracking.event.ShipmentEventPayload;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrackingEventProjectionTest {

  @Test
  void mapsEveryAuditableFieldFromSourceEnvelope() {
    UUID shipmentId = UUID.fromString("47b6cd83-10d1-43ea-950b-ec3d7bd16c4b");
    UUID eventId = UUID.fromString("baf3fb5e-8437-4e87-be93-9ff51d8cda2c");
    Instant occurredAt = Instant.parse("2026-08-24T14:00:00Z");
    Instant receivedAt = Instant.parse("2026-08-24T14:00:03Z");
    ShipmentEventEnvelope event =
        new ShipmentEventEnvelope(
            eventId,
            "ShipmentInTransit",
            shipmentId,
            7,
            occurredAt,
            "1.1",
            "corr-projection-001",
            "fedcba9876543210fedcba9876543210",
            "shipment-service",
            new ShipmentEventPayload(
                shipmentId, "UF1123456789ABCDEF01", TrackingStatus.IN_TRANSIT, 7));

    TrackingEventProjection projection = TrackingEventProjection.from(event, receivedAt);

    assertThat(projection.id()).isEqualTo(eventId);
    assertThat(projection.shipmentId()).isEqualTo(shipmentId);
    assertThat(projection.trackingNumber()).isEqualTo("UF1123456789ABCDEF01");
    assertThat(projection.status()).isEqualTo(TrackingStatus.IN_TRANSIT);
    assertThat(projection.aggregateVersion()).isEqualTo(7);
    assertThat(projection.eventType()).isEqualTo("ShipmentInTransit");
    assertThat(projection.occurredAt()).isEqualTo(occurredAt);
    assertThat(projection.receivedAt()).isEqualTo(receivedAt);
    assertThat(projection.correlationId()).isEqualTo("corr-projection-001");
    assertThat(projection.traceId()).isEqualTo("fedcba9876543210fedcba9876543210");
  }
}
