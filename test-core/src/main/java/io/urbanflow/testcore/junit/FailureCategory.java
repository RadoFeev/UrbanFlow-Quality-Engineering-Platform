package io.urbanflow.testcore.junit;

/** Stable failure taxonomy used by reports and flaky analysis. */
public enum FailureCategory {
  INFRASTRUCTURE,
  PRODUCT_DEFECT,
  ASSERTION,
  TIMEOUT,
  UNKNOWN
}
