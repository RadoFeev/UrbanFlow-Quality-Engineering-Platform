package io.urbanflow.shipment.domain;

import java.util.Objects;
import java.util.UUID;

/** Carries the actor and evidence for one requested shipment state change. */
public record ShipmentTransitionCommand(
    ShipmentStatus targetStatus, ShipmentActor actor, String reason, UUID deliveryProofId) {

  /** Creates a validated transition command. */
  public ShipmentTransitionCommand {
    Objects.requireNonNull(targetStatus, "targetStatus must not be null");
    Objects.requireNonNull(actor, "actor must not be null");
  }

  /** Returns whether a non-blank reason was supplied. */
  public boolean hasReason() {
    return reason != null && !reason.isBlank();
  }
}
