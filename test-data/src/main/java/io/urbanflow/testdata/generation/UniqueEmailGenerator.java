package io.urbanflow.testdata.generation;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Generates unique reserved-domain email addresses safe for parallel tests. */
public final class UniqueEmailGenerator {

  private final String runId;
  private final AtomicLong sequence = new AtomicLong();

  /** Creates a generator for a stable run identifier. */
  public UniqueEmailGenerator(String runId) {
    if (runId == null || !runId.toLowerCase(Locale.ROOT).matches("[a-z0-9-]{3,30}")) {
      throw new IllegalArgumentException("runId must contain 3 to 30 lowercase-safe characters");
    }
    this.runId = runId.toLowerCase(Locale.ROOT);
  }

  /** Returns a unique address under the reserved example.test domain. */
  public String next(String purpose) {
    String prefix =
        purpose == null ? "test" : purpose.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    if (prefix.isBlank()) {
      prefix = "test";
    }
    return prefix + "+" + runId + "-" + sequence.incrementAndGet() + "@example.test";
  }
}
