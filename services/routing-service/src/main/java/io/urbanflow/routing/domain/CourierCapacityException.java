package io.urbanflow.routing.domain;

import java.util.UUID;

/** Signals exhausted or unavailable courier capacity. */
public class CourierCapacityException extends RuntimeException {

  /** Creates a capacity failure for the courier. */
  public CourierCapacityException(UUID courierId) {
    super("courier cannot accept another assignment: " + courierId);
  }

  /** Creates a capacity failure when no candidate exists. */
  public CourierCapacityException() {
    super("no courier capacity is currently available");
  }
}
