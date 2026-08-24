package io.urbanflow.testcore.observability;

import java.util.UUID;

/** Thread-confined correlation identifier lifecycle. */
public final class CorrelationIdManager {

  private final ThreadLocal<String> current = new ThreadLocal<>();

  /** Returns the current identifier, creating one for this thread when absent. */
  public String current() {
    String value = current.get();
    if (value == null) {
      value = UUID.randomUUID().toString();
      current.set(value);
    }
    return value;
  }

  /** Replaces the current identifier with a validated value. */
  public void set(String value) {
    if (value == null || !value.matches("[A-Za-z0-9._-]{8,64}")) {
      throw new IllegalArgumentException("correlation ID format is invalid");
    }
    current.set(value);
  }

  /** Removes the current thread value after a test. */
  public void clear() {
    current.remove();
  }
}
