package io.urbanflow.tracking.api;

import io.urbanflow.tracking.application.TrackingTimelineItem;
import io.urbanflow.tracking.domain.TrackingStatus;
import java.util.List;

/** Complete customer-facing timeline response with request observability context. */
public record TrackingTimelineResponse(
    String trackingNumber,
    TrackingStatus currentStatus,
    List<TrackingTimelineItem> events,
    String correlationId,
    String traceId) {

  /** Defensively copies events and rejects a response without a current status. */
  public TrackingTimelineResponse {
    events = List.copyOf(events);
    if (events.isEmpty()) {
      throw new IllegalArgumentException("events must not be empty");
    }
  }

  /** Creates a response from a non-empty deterministic event timeline. */
  public static TrackingTimelineResponse from(
      String trackingNumber,
      List<TrackingTimelineItem> events,
      String correlationId,
      String traceId) {
    return new TrackingTimelineResponse(
        trackingNumber, events.getLast().status(), events, correlationId, traceId);
  }
}
