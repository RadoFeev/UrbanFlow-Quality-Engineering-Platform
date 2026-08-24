package io.urbanflow.shipment.domain;

/** Indicates that lifecycle, actor, or evidence rules rejected a transition. */
public final class InvalidShipmentTransitionException extends RuntimeException {

  private final TransitionRejectionCode code;
  private final ShipmentStatus currentStatus;
  private final ShipmentStatus targetStatus;
  private final ShipmentActor actor;

  /** Creates a transition rejection with stable diagnostic fields. */
  public InvalidShipmentTransitionException(
      TransitionRejectionCode code,
      ShipmentStatus currentStatus,
      ShipmentStatus targetStatus,
      ShipmentActor actor,
      String message) {
    super(message);
    this.code = code;
    this.currentStatus = currentStatus;
    this.targetStatus = targetStatus;
    this.actor = actor;
  }

  /** Returns the stable rejection code. */
  public TransitionRejectionCode code() {
    return code;
  }

  /** Returns the status observed before validation. */
  public ShipmentStatus currentStatus() {
    return currentStatus;
  }

  /** Returns the requested target status. */
  public ShipmentStatus targetStatus() {
    return targetStatus;
  }

  /** Returns the actor that requested the rejected transition. */
  public ShipmentActor actor() {
    return actor;
  }
}
