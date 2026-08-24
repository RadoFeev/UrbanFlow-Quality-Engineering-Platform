package io.urbanflow.shipment.messaging;

import java.time.Duration;

/** Calculates deterministic exponential delays capped by an operational maximum. */
public final class OutboxRetryPolicy {

  private final Duration initialDelay;
  private final Duration maximumDelay;

  /** Creates a retry policy from validated publication settings. */
  public OutboxRetryPolicy(OutboxPublicationProperties properties) {
    initialDelay = properties.initialRetryDelay();
    maximumDelay = properties.maxRetryDelay();
  }

  /** Returns the delay after the supplied completed attempt number. */
  public Duration delayAfter(int completedAttempts) {
    if (completedAttempts <= 0) {
      throw new IllegalArgumentException("completedAttempts must be positive");
    }
    Duration delay = initialDelay;
    for (int attempt = 1; attempt < completedAttempts; attempt++) {
      if (delay.compareTo(maximumDelay.dividedBy(2)) > 0) {
        return maximumDelay;
      }
      delay = delay.multipliedBy(2);
    }
    return delay.compareTo(maximumDelay) > 0 ? maximumDelay : delay;
  }
}
