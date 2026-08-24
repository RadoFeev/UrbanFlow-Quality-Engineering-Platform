package io.urbanflow.tracking.application;

/** Signals that no projected timeline exists for a tracking number. */
public class TrackingNotFoundException extends RuntimeException {

  /** Creates a lookup failure without echoing arbitrary input in the message. */
  public TrackingNotFoundException() {
    super("Tracking timeline was not found");
  }
}
