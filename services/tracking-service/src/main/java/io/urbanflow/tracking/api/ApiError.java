package io.urbanflow.tracking.api;

import java.time.Instant;

/** Stable tracking API error response without reflected request values. */
public record ApiError(
    String code,
    String message,
    int status,
    String path,
    Instant timestamp,
    String correlationId,
    String traceId) {}
