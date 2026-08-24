package io.urbanflow.shipment.domain;

/** Enumerates every supported state in the UrbanFlow shipment lifecycle. */
public enum ShipmentStatus {
  CREATED,
  CONFIRMED,
  COURIER_ASSIGNED,
  PICKUP_IN_PROGRESS,
  PICKED_UP,
  AT_ORIGIN_HUB,
  IN_TRANSIT,
  AT_DESTINATION_HUB,
  OUT_FOR_DELIVERY,
  DELIVERY_ATTEMPTED,
  DELIVERED,
  CANCELLED,
  RETURN_IN_PROGRESS,
  RETURNED;

  /** Returns whether this status ends the shipment lifecycle. */
  public boolean isTerminal() {
    return this == DELIVERED || this == CANCELLED || this == RETURNED;
  }

  /** Returns whether the customer may still change the delivery address. */
  public boolean allowsDeliveryAddressChange() {
    return this == CREATED || this == CONFIRMED || this == COURIER_ASSIGNED;
  }
}
