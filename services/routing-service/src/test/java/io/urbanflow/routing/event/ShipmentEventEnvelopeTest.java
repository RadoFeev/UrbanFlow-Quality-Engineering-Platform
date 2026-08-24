package io.urbanflow.routing.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShipmentEventEnvelopeTest {

  @Test
  void recognizesOnlySupportedConfirmedShipmentTrigger() {
    assertThat(
            event("ShipmentConfirmed", "CONFIRMED", "1.0", "shipment-service")
                .isAssignmentTrigger())
        .isTrue();
    assertThat(event("ShipmentCreated", "CREATED", "1.0", "shipment-service").isAssignmentTrigger())
        .isFalse();
    assertThat(
            event("ShipmentConfirmed", "CONFIRMED", "2.0", "shipment-service")
                .isAssignmentTrigger())
        .isFalse();
    assertThat(event("ShipmentConfirmed", "CONFIRMED", "1.0", "unknown").isAssignmentTrigger())
        .isFalse();
  }

  @Test
  void rejectsAggregatePayloadMismatch() {
    ShipmentEventEnvelope valid =
        event("ShipmentConfirmed", "CONFIRMED", "1.0", "shipment-service");
    assertThatThrownBy(
            () ->
                new ShipmentEventEnvelope(
                    valid.eventId(),
                    valid.eventType(),
                    UUID.randomUUID(),
                    valid.aggregateVersion(),
                    valid.occurredAt(),
                    valid.schemaVersion(),
                    valid.correlationId(),
                    valid.traceId(),
                    valid.producer(),
                    valid.payload()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("aggregate");
  }

  static ShipmentEventEnvelope event(
      String eventType, String status, String schemaVersion, String producer) {
    UUID shipmentId = UUID.fromString("7d38bce9-4f03-4269-b1db-a19fc9358679");
    return new ShipmentEventEnvelope(
        UUID.fromString("5df93d33-9eed-4a1f-95eb-362cdfdbd524"),
        eventType,
        shipmentId,
        1,
        Instant.parse("2026-08-24T12:00:00Z"),
        schemaVersion,
        "corr-routing-001",
        "0123456789abcdef0123456789abcdef",
        producer,
        new ShipmentEventPayload(shipmentId, "UF0123456789ABCDEF01", status, 1));
  }
}
