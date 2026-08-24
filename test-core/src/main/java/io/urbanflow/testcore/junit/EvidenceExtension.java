package io.urbanflow.testcore.junit;

import io.urbanflow.testcore.context.TestContext;
import io.urbanflow.testcore.evidence.AllureAttachmentService;
import io.urbanflow.testcore.evidence.LogAttachmentService;
import io.urbanflow.testcore.evidence.ScreenshotService;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Automatically attaches browser, correlation, and trace evidence after a failed test. */
public final class EvidenceExtension implements AfterTestExecutionCallback {

  private final ScreenshotService screenshots = new ScreenshotService();
  private final LogAttachmentService logs = new LogAttachmentService();
  private final AllureAttachmentService attachments = new AllureAttachmentService();

  @Override
  public void afterTestExecution(ExtensionContext extensionContext) {
    if (extensionContext.getExecutionException().isEmpty()) {
      return;
    }
    TestContext context = TestContextExtension.context(extensionContext);
    if (context == null) {
      return;
    }
    attachments.text("correlation-id", context.correlationId());
    attachments.text("trace-id", context.traceId());
    if (context.drivers().hasCurrent()) {
      var driver = context.drivers().current();
      capture(
          "failure-screenshot",
          () -> attachments.png("failure-screenshot", screenshots.screenshot(driver)));
      capture(
          "page-source",
          () -> attachments.text("page-source", attachments.utf8(screenshots.pageSource(driver))));
      capture(
          "browser-console",
          () -> attachments.text("browser-console", attachments.utf8(logs.browserConsole(driver))));
    }
  }

  private void capture(String name, Runnable operation) {
    try {
      operation.run();
    } catch (RuntimeException exception) {
      attachments.text(name + "-capture-error", exception.getClass().getSimpleName());
    }
  }
}
