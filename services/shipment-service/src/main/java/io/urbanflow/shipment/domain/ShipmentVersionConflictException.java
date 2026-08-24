package io.urbanflow.shipment.domain;

import java.util.UUID;

/** Indicates that a caller attempted to update a stale shipment version. */
public final class ShipmentVersionConflictException extends RuntimeException {

  private final UUID shipmentId;
  private final long expectedVersion;
  private final long actualVersion;

  /** Creates an optimistic-lock conflict with expected and actual versions. */
  public ShipmentVersionConflictException(
      UUID shipmentId, long expectedVersion, long actualVersion) {
    super(
        "Shipment %s has version %d, not requested version %d"
            .formatted(shipmentId, actualVersion, expectedVersion));
    this.shipmentId = shipmentId;
    this.expectedVersion = expectedVersion;
    this.actualVersion = actualVersion;
  }

  /** Returns the concurrently modified shipment identifier. */
  public UUID shipmentId() {
    return shipmentId;
  }

  /** Returns the version supplied by the caller. */
  public long expectedVersion() {
    return expectedVersion;
  }

  /** Returns the version observed by the service. */
  public long actualVersion() {
    return actualVersion;
  }
}
