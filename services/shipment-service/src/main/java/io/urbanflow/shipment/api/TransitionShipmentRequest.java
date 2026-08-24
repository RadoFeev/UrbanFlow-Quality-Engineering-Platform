package io.urbanflow.shipment.api;

import io.urbanflow.shipment.domain.ShipmentActor;
import io.urbanflow.shipment.domain.ShipmentStatus;
import io.urbanflow.shipment.domain.ShipmentTransitionCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Validated request body for a shipment lifecycle transition. */
public record TransitionShipmentRequest(
    @NotNull ShipmentStatus targetStatus,
    @NotNull ShipmentActor actor,
    @Size(max = 500) String reason,
    UUID deliveryProofId) {

  /** Converts validated API data into a domain transition command. */
  public ShipmentTransitionCommand toCommand() {
    return new ShipmentTransitionCommand(targetStatus, actor, reason, deliveryProofId);
  }
}
