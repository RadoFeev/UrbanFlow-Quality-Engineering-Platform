package io.urbanflow.shipment.api;

import java.time.Instant;
import java.util.List;

/** Stable error response shared by shipment API failure paths. */
public record ApiError(
    String code,
    String message,
    int status,
    String path,
    Instant timestamp,
    String correlationId,
    String traceId,
    List<FieldViolation> violations) {

  /** Identifies one invalid request field without echoing its sensitive value. */
  public record FieldViolation(String field, String message) {}
}
