package io.urbanflow.testcore.assertions;

import org.assertj.core.api.SoftAssertions;

/** Owns one AssertJ soft-assertion collector per test thread. */
public final class SoftAssertionManager {

  private final ThreadLocal<SoftAssertions> current = ThreadLocal.withInitial(SoftAssertions::new);

  /** Returns the current thread collector. */
  public SoftAssertions current() {
    return current.get();
  }

  /** Asserts collected results and removes the collector. */
  public void assertAllAndClear() {
    SoftAssertions assertions = current.get();
    current.remove();
    assertions.assertAll();
  }

  /** Clears the current collector without asserting, for aborted infrastructure runs. */
  public void clear() {
    current.remove();
  }
}
