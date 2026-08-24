package io.urbanflow.testcore.browser;

import io.urbanflow.testcore.config.EnvironmentConfig;
import java.util.Locale;
import java.util.logging.Level;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;

/** Creates browser-specific capabilities without implicit waits or shared state. */
public final class BrowserOptionsFactory {

  /** Creates safe deterministic capabilities for the configured browser. */
  public MutableCapabilities create(EnvironmentConfig configuration) {
    return switch (configuration.browser().toLowerCase(Locale.ROOT)) {
      case "chrome" -> chrome(configuration.headless(), configuration.videoEnabled());
      case "firefox" -> firefox(configuration.headless(), configuration.videoEnabled());
      case "edge" -> edge(configuration.headless(), configuration.videoEnabled());
      default ->
          throw new IllegalArgumentException("unsupported browser: " + configuration.browser());
    };
  }

  private static ChromeOptions chrome(boolean headless, boolean videoEnabled) {
    ChromeOptions options = new ChromeOptions();
    options.addArguments(
        "--window-size=1440,1000",
        "--disable-dev-shm-usage",
        "--no-default-browser-check",
        "--disable-background-timer-throttling",
        "--disable-backgrounding-occluded-windows",
        "--disable-renderer-backgrounding");
    if (headless) {
      options.addArguments("--headless=new");
    }
    options.setCapability("se:recordVideo", videoEnabled);
    options.setCapability("goog:loggingPrefs", chromiumLogging());
    return options;
  }

  private static FirefoxOptions firefox(boolean headless, boolean videoEnabled) {
    FirefoxOptions options = new FirefoxOptions();
    options.addArguments("--width=1440", "--height=1000");
    if (headless) {
      options.addArguments("-headless");
    }
    options.setCapability("se:recordVideo", videoEnabled);
    return options;
  }

  private static EdgeOptions edge(boolean headless, boolean videoEnabled) {
    EdgeOptions options = new EdgeOptions();
    options.addArguments(
        "--window-size=1440,1000",
        "--disable-dev-shm-usage",
        "--disable-background-timer-throttling",
        "--disable-backgrounding-occluded-windows",
        "--disable-renderer-backgrounding");
    if (headless) {
      options.addArguments("--headless=new");
    }
    options.setCapability("se:recordVideo", videoEnabled);
    options.setCapability("goog:loggingPrefs", chromiumLogging());
    return options;
  }

  private static LoggingPreferences chromiumLogging() {
    LoggingPreferences preferences = new LoggingPreferences();
    preferences.enable(LogType.BROWSER, Level.ALL);
    preferences.enable(LogType.PERFORMANCE, Level.ALL);
    return preferences;
  }
}
