package io.urbanflow.notification.event;

import java.util.Objects;
import java.util.UUID;

/** Notification-owned subset of shipment event data. */
public record ShipmentEventPayload(
    UUID shipmentId, UUID customerId, String trackingNumber, String status, long version) {

  /** Validates identity and public tracking fields. */
  public ShipmentEventPayload {
    Objects.requireNonNull(shipmentId, "shipmentId must not be null");
    Objects.requireNonNull(customerId, "customerId must not be null");
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
