package io.urbanflow.shipment.domain;

import java.util.UUID;

/** Indicates that an address change was requested after the lifecycle cutoff. */
public final class DeliveryAddressChangeNotAllowedException extends RuntimeException {

  private final UUID shipmentId;
  private final ShipmentStatus status;

  /** Creates an address-change rejection for the current status. */
  public DeliveryAddressChangeNotAllowedException(UUID shipmentId, ShipmentStatus status) {
    super(
        "Delivery address cannot be changed while shipment %s is %s".formatted(shipmentId, status));
    this.shipmentId = shipmentId;
    this.status = status;
  }

  /** Returns the shipment whose address update was rejected. */
  public UUID shipmentId() {
    return shipmentId;
  }

  /** Returns the status that prevents the address update. */
  public ShipmentStatus status() {
    return status;
  }
}
