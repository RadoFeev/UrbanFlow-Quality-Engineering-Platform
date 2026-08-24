package io.urbanflow.testcore.browser;

import java.util.Objects;
import java.util.function.Supplier;
import org.openqa.selenium.WebDriver;

/** Owns one WebDriver per test thread and prevents cross-test session sharing. */
public final class DriverManager implements AutoCloseable {

  private final Supplier<WebDriver> factory;
  private final ThreadLocal<WebDriver> current = new ThreadLocal<>();

  /** Creates the lifecycle manager from a fresh-driver supplier. */
  public DriverManager(Supplier<WebDriver> factory) {
    this.factory = Objects.requireNonNull(factory, "factory must not be null");
  }

  /** Returns the current thread driver, creating it lazily. */
  public WebDriver current() {
    WebDriver driver = current.get();
    if (driver == null) {
      driver = Objects.requireNonNull(factory.get(), "factory returned null driver");
      current.set(driver);
    }
    return driver;
  }

  /** Returns whether the current thread owns a browser session. */
  public boolean hasCurrent() {
    return current.get() != null;
  }

  /** Quits and removes only the current thread browser session. */
  @Override
  public void close() {
    WebDriver driver = current.get();
    current.remove();
    if (driver != null) {
      driver.quit();
    }
  }
}
