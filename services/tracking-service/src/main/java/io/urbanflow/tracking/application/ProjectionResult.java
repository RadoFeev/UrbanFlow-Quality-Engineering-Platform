package io.urbanflow.tracking.application;

/** Outcome of processing one shipment event at the idempotent projection boundary. */
public enum ProjectionResult {
  PROJECTED,
  DUPLICATE,
  STALE,
  IGNORED
}
