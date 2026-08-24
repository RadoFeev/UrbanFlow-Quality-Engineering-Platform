package io.urbanflow.tracking.application;

import io.urbanflow.tracking.domain.TrackingStatus;
import io.urbanflow.tracking.persistence.TrackingEventProjection;
import java.time.Instant;
import java.util.UUID;

/** Immutable customer-facing item in a shipment tracking timeline. */
public record TrackingTimelineItem(
    UUID eventId,
    TrackingStatus status,
    long version,
    Instant occurredAt,
    String correlationId,
    String traceId) {

  /** Maps a persisted projection without exposing persistence internals. */
  public static TrackingTimelineItem from(TrackingEventProjection event) {
    return new TrackingTimelineItem(
        event.id(),
        event.status(),
        event.aggregateVersion(),
        event.occurredAt(),
        event.correlationId(),
        event.traceId());
  }
}
