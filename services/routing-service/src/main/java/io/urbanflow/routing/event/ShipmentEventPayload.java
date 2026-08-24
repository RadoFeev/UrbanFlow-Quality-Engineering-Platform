package io.urbanflow.routing.event;

import java.util.Objects;
import java.util.UUID;

/** Routing-owned subset of shipment event data. */
public record ShipmentEventPayload(
    UUID shipmentId, String trackingNumber, String status, long version) {

  /** Validates the routing input boundary. */
  public ShipmentEventPayload {
    Objects.requireNonNull(shipmentId, "shipmentId must not be null");
    if (trackingNumber == null || !trackingNumber.matches("UF[0-9A-F]{18}")) {
      throw new IllegalArgumentException("trackingNumber has invalid format");
    }
    if (status == null || status.isBlank()) {
      throw new IllegalArgumentException("status must not be blank");
    }
    if (version < 0) {
      throw new IllegalArgumentException("version must not be negative");
    }
  }
}
