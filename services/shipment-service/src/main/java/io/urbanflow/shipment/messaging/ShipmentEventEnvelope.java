package io.urbanflow.shipment.messaging;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Stable versioned event envelope emitted by the shipment service. */
public record ShipmentEventEnvelope(
    UUID eventId,
    String eventType,
    UUID aggregateId,
    long aggregateVersion,
    String occurredAt,
    String schemaVersion,
    String correlationId,
    String traceId,
    String producer,
    JsonNode payload) {}
