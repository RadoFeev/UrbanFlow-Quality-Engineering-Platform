package io.urbanflow.testdata.generation;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Generates unique deterministic UrbanFlow tracking numbers for tests. */
public final class UniqueTrackingNumberGenerator {

  private final String runCode;
  private final AtomicLong sequence = new AtomicLong();

  /** Creates a generator from a four-character run code. */
  public UniqueTrackingNumberGenerator(String runCode) {
    String normalized = runCode == null ? "" : runCode.toUpperCase(Locale.ROOT);
    if (!normalized.matches("[A-Z0-9]{4}")) {
      throw new IllegalArgumentException("runCode must contain exactly four letters or digits");
    }
    this.runCode = normalized;
  }

  /** Returns the next 8-character tracking suffix. */
  public String next() {
    long value = sequence.incrementAndGet();
    if (value > 9_999) {
      throw new IllegalStateException("tracking sequence exhausted for run code");
    }
    return "UF-" + runCode + String.format(Locale.ROOT, "%04d", value);
  }
}
