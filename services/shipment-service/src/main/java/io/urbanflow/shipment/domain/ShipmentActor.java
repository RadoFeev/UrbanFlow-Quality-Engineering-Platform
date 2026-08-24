package io.urbanflow.shipment.domain;

/** Identifies the actor requesting a shipment lifecycle transition. */
public enum ShipmentActor {
  CUSTOMER,
  COURIER,
  WAREHOUSE_OPERATOR,
  ROUTING_SERVICE,
  SYSTEM,
  OPERATIONS
}
