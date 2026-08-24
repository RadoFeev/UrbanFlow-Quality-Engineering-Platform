package io.urbanflow.tracking.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.urbanflow.tracking.domain.TrackingStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShipmentEventEnvelopeTest {

  @Test
  void recognizesSupportedMajorSchema() {
    assertThat(event("1.0").hasSupportedSchema()).isTrue();
    assertThat(event("1.9").hasSupportedSchema()).isTrue();
    assertThat(event("2.0").hasSupportedSchema()).isFalse();
  }

  @Test
  void rejectsBlankMandatoryMetadata() {
    ShipmentEventEnvelope valid = event("1.0");

    assertThatThrownBy(
            () ->
                new ShipmentEventEnvelope(
                    valid.eventId(),
                    " ",
                    valid.aggregateId(),
                    valid.aggregateVersion(),
                    valid.occurredAt(),
                    valid.schemaVersion(),
                    valid.correlationId(),
                    valid.traceId(),
                    valid.producer(),
                    valid.payload()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("eventType");
  }

  private static ShipmentEventEnvelope event(String schemaVersion) {
    UUID shipmentId = UUID.fromString("5ba431c1-f27a-4362-a836-df32dc54002a");
    return new ShipmentEventEnvelope(
        UUID.fromString("5e505d30-0e6f-417b-9a93-a5018968399a"),
        "ShipmentCreated",
        shipmentId,
        0,
        Instant.parse("2026-08-24T12:00:00Z"),
        schemaVersion,
        "corr-tracking-001",
        "0123456789abcdef0123456789abcdef",
        "shipment-service",
        new ShipmentEventPayload(shipmentId, "UF1123456789ABCDEF01", TrackingStatus.CREATED, 0));
  }
}
