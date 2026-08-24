package io.urbanflow.notification.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Notification-owned representation of the shipment event envelope. */
public record ShipmentEventEnvelope(
    UUID eventId,
    String eventType,
    UUID aggregateId,
    long aggregateVersion,
    Instant occurredAt,
    String schemaVersion,
    String correlationId,
    String traceId,
    String producer,
    ShipmentEventPayload payload) {

  /** Validates mandatory metadata and payload ordering identity. */
  public ShipmentEventEnvelope {
    Objects.requireNonNull(eventId, "eventId must not be null");
    eventType = requireText(eventType, "eventType");
    Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    schemaVersion = requireText(schemaVersion, "schemaVersion");
    correlationId = requireText(correlationId, "correlationId");
    traceId = requireText(traceId, "traceId");
    producer = requireText(producer, "producer");
    Objects.requireNonNull(payload, "payload must not be null");
    if (!aggregateId.equals(payload.shipmentId()) || aggregateVersion != payload.version()) {
      throw new IllegalArgumentException("aggregate metadata does not match payload");
    }
  }

  /** Returns whether the producer and schema are supported. */
  public boolean isSupported() {
    return producer.equals("shipment-service") && schemaVersion.startsWith("1.");
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }
}
