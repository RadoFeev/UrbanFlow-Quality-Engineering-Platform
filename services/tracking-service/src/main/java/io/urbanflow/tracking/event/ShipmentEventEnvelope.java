package io.urbanflow.tracking.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Consumer-owned representation of the stable shipment event envelope. */
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

  /** Validates mandatory envelope metadata before projection. */
  public ShipmentEventEnvelope {
    Objects.requireNonNull(eventId, "eventId must not be null");
    eventType = requireText(eventType, "eventType");
    Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    if (aggregateVersion < 0) {
      throw new IllegalArgumentException("aggregateVersion must not be negative");
    }
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    schemaVersion = requireText(schemaVersion, "schemaVersion");
    correlationId = requireText(correlationId, "correlationId");
    traceId = requireText(traceId, "traceId");
    producer = requireText(producer, "producer");
    Objects.requireNonNull(payload, "payload must not be null");
  }

  /** Returns whether the consumer supports the envelope major schema. */
  public boolean hasSupportedSchema() {
    return schemaVersion.startsWith("1.");
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }
}
