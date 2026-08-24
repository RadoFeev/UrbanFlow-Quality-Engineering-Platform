package io.urbanflow.testcore.junit;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/** Adds stable failure and success classification labels to test reports. */
public final class TestOutcomeListener implements TestWatcher {

  private final FailureClassifier classifier = new FailureClassifier();

  @Override
  public void testSuccessful(ExtensionContext context) {
    Allure.label("outcome", "passed");
  }

  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    Allure.label("outcome", "failed");
    Allure.label(
        "failureCategory", classifier.classify(cause).name().toLowerCase(java.util.Locale.ROOT));
  }

  @Override
  public void testAborted(ExtensionContext context, Throwable cause) {
    Allure.label("outcome", "aborted");
  }
}
