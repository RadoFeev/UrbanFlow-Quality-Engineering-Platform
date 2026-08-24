package io.urbanflow.notification.application;

/** Idempotent shipment-event processing outcome. */
public enum ProcessingResult {
  CREATED,
  DUPLICATE,
  IGNORED
}
