package io.urbanflow.shipment.application;

/** Generates unique non-personal tracking numbers for new shipments. */
public interface TrackingNumberGenerator {

  /** Returns the next tracking number candidate. */
  String next();
}
