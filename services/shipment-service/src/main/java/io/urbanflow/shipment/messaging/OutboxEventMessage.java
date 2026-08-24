package io.urbanflow.shipment.messaging;

import io.urbanflow.shipment.persistence.OutboxEvent;
import java.time.Instant;
import java.util.UUID;

/** Immutable event data detached from the short database claim transaction. */
public record OutboxEventMessage(
    UUID eventId,
    String eventType,
    UUID aggregateId,
    long aggregateVersion,
    String schemaVersion,
    String payload,
    String correlationId,
    String traceId,
    Instant occurredAt,
    int publicationAttempts) {

  /** Creates a publication message from a claimed persistence entity. */
  public static OutboxEventMessage from(OutboxEvent event) {
    return new OutboxEventMessage(
        event.id(),
        event.eventType(),
        event.aggregateId(),
        event.aggregateVersion(),
        event.schemaVersion(),
        event.payload(),
        event.correlationId(),
        event.traceId(),
        event.occurredAt(),
        event.publicationAttempts());
  }
}
