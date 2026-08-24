package io.urbanflow.tracking.event;

import io.urbanflow.tracking.domain.TrackingStatus;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Consumer-owned view of the version 1 shipment event payload. */
public record ShipmentEventPayload(
    UUID shipmentId, String trackingNumber, TrackingStatus status, long version) {

  private static final Pattern TRACKING_NUMBER = Pattern.compile("UF[0-9A-F]{18}");

  /** Validates identifiers and the ordering version at the consumer boundary. */
  public ShipmentEventPayload {
    Objects.requireNonNull(shipmentId, "shipmentId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    if (trackingNumber == null || !TRACKING_NUMBER.matcher(trackingNumber).matches()) {
      throw new IllegalArgumentException("trackingNumber has invalid format");
    }
    if (version < 0) {
      throw new IllegalArgumentException("version must not be negative");
    }
  }
}
