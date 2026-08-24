package io.urbanflow.routing.api;

import java.time.Instant;

/** Stable routing error response. */
public record ApiError(String code, String message, int status, String path, Instant timestamp) {}
