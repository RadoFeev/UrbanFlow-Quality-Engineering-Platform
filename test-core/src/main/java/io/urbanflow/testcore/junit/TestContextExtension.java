package io.urbanflow.testcore.junit;

import io.urbanflow.testcore.browser.BrowserOptionsFactory;
import io.urbanflow.testcore.browser.DriverFactory;
import io.urbanflow.testcore.browser.DriverManager;
import io.urbanflow.testcore.browser.RemoteDriverProvider;
import io.urbanflow.testcore.config.ConfigurationManager;
import io.urbanflow.testcore.context.ScenarioContext;
import io.urbanflow.testcore.context.TestContext;
import io.urbanflow.testcore.data.TestDataCleaner;
import io.urbanflow.testcore.observability.CorrelationIdManager;
import io.urbanflow.testcore.observability.TraceIdManager;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/** Creates and closes one isolated TestContext for every JUnit invocation. */
public final class TestContextExtension
    implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(TestContextExtension.class);
  private static final String KEY = "test-context";

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    var environment = ConfigurationManager.system().resolve();
    DriverFactory driverFactory =
        new DriverFactory(new BrowserOptionsFactory(), new RemoteDriverProvider());
    TestContext context =
        new TestContext(
            environment,
            new ScenarioContext(),
            new TestDataCleaner(),
            new DriverManager(() -> driverFactory.create(environment)),
            new CorrelationIdManager(),
            new TraceIdManager());
    extensionContext.getStore(NAMESPACE).put(KEY, context);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    TestContext context = extensionContext.getStore(NAMESPACE).remove(KEY, TestContext.class);
    if (context != null) {
      context.close();
    }
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(TestContext.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    TestContext context = context(extensionContext);
    if (context == null) {
      throw new ParameterResolutionException("TestContext has not been initialized");
    }
    return context;
  }

  static TestContext context(ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE).get(KEY, TestContext.class);
  }
}
