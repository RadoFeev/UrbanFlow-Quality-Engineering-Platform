package io.urbanflow.testcore.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.urbanflow.testcore.config.EnvironmentConfig;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

class BrowserLifecycleTest {

  @Test
  void createsHeadlessChromeCapabilities() {
    ChromeOptions options =
        (ChromeOptions) new BrowserOptionsFactory().create(config("chrome", true));
    assertThat(arguments(options, "goog:chromeOptions"))
        .contains("--headless=new", "--window-size=1440,1000");
  }

  @Test
  void createsVisibleFirefoxCapabilities() {
    FirefoxOptions options =
        (FirefoxOptions) new BrowserOptionsFactory().create(config("firefox", false));
    assertThat(arguments(options, "moz:firefoxOptions"))
        .contains("--width=1440", "--height=1000")
        .doesNotContain("-headless");
  }

  @Test
  void createsHeadlessEdgeCapabilities() {
    EdgeOptions options = (EdgeOptions) new BrowserOptionsFactory().create(config("edge", true));
    assertThat(arguments(options, "ms:edgeOptions")).contains("--headless=new");
  }

  @Test
  void driverManagerReusesOnlyCurrentThreadDriver() {
    WebDriver driver = mock(WebDriver.class);
    AtomicInteger creations = new AtomicInteger();
    DriverManager manager =
        new DriverManager(
            () -> {
              creations.incrementAndGet();
              return driver;
            });

    assertThat(manager.current()).isSameAs(manager.current());
    assertThat(creations).hasValue(1);
    assertThat(manager.hasCurrent()).isTrue();
    manager.close();

    verify(driver).quit();
    assertThat(manager.hasCurrent()).isFalse();
  }

  @Test
  void driverManagerIsolatesParallelThreads() {
    ConcurrentLinkedQueue<WebDriver> created = new ConcurrentLinkedQueue<>();
    DriverManager manager =
        new DriverManager(
            () -> {
              WebDriver driver = mock(WebDriver.class);
              created.add(driver);
              return driver;
            });

    IntStream.range(0, 8)
        .parallel()
        .forEach(
            ignored -> {
              manager.current();
              manager.close();
            });

    assertThat(created).hasSizeGreaterThan(1);
    created.forEach(driver -> verify(driver).quit());
  }

  @Test
  void closingUnusedManagerDoesNothing() {
    DriverManager manager = new DriverManager(() -> mock(WebDriver.class));
    manager.close();
    assertThat(manager.hasCurrent()).isFalse();
  }

  private static EnvironmentConfig config(String browser, boolean headless) {
    URI local = URI.create("http://localhost:8080");
    return new EnvironmentConfig(
        "local",
        local,
        local,
        local,
        local,
        local,
        local,
        local,
        local,
        local,
        browser,
        headless,
        null,
        2,
        Duration.ofSeconds(10),
        false,
        true);
  }

  @SuppressWarnings("unchecked")
  private static java.util.List<String> arguments(
      MutableCapabilities options, String capabilityName) {
    var capability = (java.util.Map<String, Object>) options.getCapability(capabilityName);
    return (java.util.List<String>) capability.get("args");
  }
}
