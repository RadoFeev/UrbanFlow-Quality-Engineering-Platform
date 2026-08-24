package io.urbanflow.testcore.resilience;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/** Awaitility-based eventual-consistency polling without fixed-duration sleeps. */
public final class PollingService {

  /** Polls until a supplied value satisfies the predicate or the timeout expires. */
  public <T> T until(
      String description,
      Duration timeout,
      Duration interval,
      Callable<T> supplier,
      Predicate<T> condition) {
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("poll description must not be blank");
    }
    if (timeout == null || interval == null || timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("poll timeout and interval must be positive");
    }
    return await(description).pollInterval(interval).atMost(timeout).until(supplier, condition);
  }
}
