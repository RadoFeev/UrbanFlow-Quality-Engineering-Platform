package io.urbanflow.identity.api;

import java.time.Instant;

/** Stable error envelope shared by identity HTTP failure paths. */
public record ApiError(
    String code,
    String message,
    int status,
    String path,
    String correlationId,
    String traceId,
    Instant timestamp) {}
