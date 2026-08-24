package io.urbanflow.warehouse.api;

import java.time.Instant;

/** Stable warehouse HTTP error envelope. */
public record ApiError(String code, String message, int status, String path, Instant timestamp) {}
