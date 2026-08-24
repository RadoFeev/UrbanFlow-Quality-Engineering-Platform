package io.urbanflow.tracking.api;

import io.urbanflow.tracking.application.TrackingQueryService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the public shipment timeline without leaking service persistence details. */
@Validated
@RestController
@RequestMapping("/api/v1/tracking")
public class TrackingController {

  private final TrackingQueryService queryService;

  TrackingController(TrackingQueryService queryService) {
    this.queryService = queryService;
  }

  /** Returns an ordered timeline for a syntactically valid tracking number. */
  @GetMapping("/{trackingNumber}")
  public ResponseEntity<TrackingTimelineResponse> timeline(
      @PathVariable @Pattern(regexp = "UF[0-9A-F]{18}") String trackingNumber,
      @RequestAttribute(RequestTracingFilter.CORRELATION_ATTRIBUTE) String correlationId,
      @RequestAttribute(RequestTracingFilter.TRACE_ATTRIBUTE) String traceId) {
    TrackingTimelineResponse response =
        TrackingTimelineResponse.from(
            trackingNumber, queryService.timeline(trackingNumber), correlationId, traceId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Vary", RequestTracingFilter.CORRELATION_HEADER)
        .body(response);
  }
}
