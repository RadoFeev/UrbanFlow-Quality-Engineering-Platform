package io.urbanflow.shipment.application;

/** Indicates that a shipment lookup did not match any persisted aggregate. */
public final class ShipmentNotFoundException extends RuntimeException {

  /** Creates a not-found error for an external identifier. */
  public ShipmentNotFoundException(String identifier) {
    super("Shipment not found: " + identifier);
  }
}
