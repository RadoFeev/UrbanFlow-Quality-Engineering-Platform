package io.urbanflow.testcore.context;

import io.urbanflow.testcore.browser.DriverManager;
import io.urbanflow.testcore.config.EnvironmentConfig;
import io.urbanflow.testcore.data.TestDataCleaner;
import io.urbanflow.testcore.observability.CorrelationIdManager;
import io.urbanflow.testcore.observability.TraceIdManager;
import java.util.Objects;

/** Owns all mutable resources for one test execution. */
public final class TestContext implements AutoCloseable {

  private final EnvironmentConfig environment;
  private final ScenarioContext scenario;
  private final TestDataCleaner cleaner;
  private final DriverManager drivers;
  private final CorrelationIdManager correlations;
  private final TraceIdManager traces;

  /** Creates an independently owned test context. */
  public TestContext(
      EnvironmentConfig environment,
      ScenarioContext scenario,
      TestDataCleaner cleaner,
      DriverManager drivers,
      CorrelationIdManager correlations,
      TraceIdManager traces) {
    this.environment = Objects.requireNonNull(environment);
    this.scenario = Objects.requireNonNull(scenario);
    this.cleaner = Objects.requireNonNull(cleaner);
    this.drivers = Objects.requireNonNull(drivers);
    this.correlations = Objects.requireNonNull(correlations);
    this.traces = Objects.requireNonNull(traces);
  }

  /** Returns immutable environment configuration. */
  public EnvironmentConfig environment() {
    return environment;
  }

  /** Returns scenario-owned typed values. */
  public ScenarioContext scenario() {
    return scenario;
  }

  /** Returns automatic cleanup registry. */
  public TestDataCleaner cleaner() {
    return cleaner;
  }

  /** Returns the thread-confined driver lifecycle. */
  public DriverManager drivers() {
    return drivers;
  }

  /** Returns current correlation identifier. */
  public String correlationId() {
    return correlations.current();
  }

  /** Returns current trace identifier. */
  public String traceId() {
    return traces.current();
  }

  /** Releases cleanup, browser, and thread-local resources without skipping later steps. */
  @Override
  public void close() {
    RuntimeException failure = null;
    Runnable[] operations = {
      cleaner::close, drivers::close, scenario::clear, correlations::clear, traces::clear
    };
    for (Runnable operation : operations) {
      try {
        operation.run();
      } catch (RuntimeException exception) {
        if (failure == null) {
          failure = new IllegalStateException("test context cleanup failed");
        }
        failure.addSuppressed(exception);
      }
    }
    if (failure != null) {
      throw failure;
    }
  }
}
