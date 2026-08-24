package io.urbanflow.testcore.junit;

import io.urbanflow.testcore.resilience.InfrastructureFailure;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

/** Classifies failures without converting product defects into retryable outcomes. */
public final class FailureClassifier {

  /** Classifies a failure and its cause chain. */
  public FailureCategory classify(Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof InfrastructureFailure || current instanceof ConnectException) {
        return FailureCategory.INFRASTRUCTURE;
      }
      if (current instanceof TimeoutException
          || current instanceof java.net.http.HttpTimeoutException
          || current instanceof org.openqa.selenium.TimeoutException) {
        return FailureCategory.TIMEOUT;
      }
      if (current instanceof AssertionError) {
        return FailureCategory.ASSERTION;
      }
    }
    return failure instanceof RuntimeException
        ? FailureCategory.PRODUCT_DEFECT
        : FailureCategory.UNKNOWN;
  }
}
