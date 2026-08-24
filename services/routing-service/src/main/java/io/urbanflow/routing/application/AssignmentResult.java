package io.urbanflow.routing.application;

/** Idempotent outcome of processing one shipment event. */
public enum AssignmentResult {
  ASSIGNED,
  DUPLICATE,
  IGNORED
}
