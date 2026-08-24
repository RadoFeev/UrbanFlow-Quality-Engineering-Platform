package io.urbanflow.routing.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Routing-owned representation of the stable shipment event envelope. */
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

  /** Validates mandatory event metadata and aggregate consistency. */
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

  /** Returns whether this event is a supported assignment trigger. */
  public boolean isAssignmentTrigger() {
    return "ShipmentConfirmed".equals(eventType)
        && "CONFIRMED".equals(payload.status())
        && schemaVersion.startsWith("1.")
        && "shipment-service".equals(producer);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }
}
