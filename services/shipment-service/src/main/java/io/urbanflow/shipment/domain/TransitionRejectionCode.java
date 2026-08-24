package io.urbanflow.shipment.domain;

/** Stable error codes returned when a requested shipment transition is rejected. */
public enum TransitionRejectionCode {
  ILLEGAL_STATE_TRANSITION,
  ACTOR_NOT_AUTHORIZED,
  REASON_REQUIRED,
  DELIVERY_PROOF_REQUIRED
}
