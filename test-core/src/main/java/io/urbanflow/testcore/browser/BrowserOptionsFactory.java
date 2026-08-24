package io.urbanflow.testcore.browser;

import io.urbanflow.testcore.config.EnvironmentConfig;
import java.util.Locale;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

/** Creates browser-specific capabilities without implicit waits or shared state. */
public final class BrowserOptionsFactory {

  /** Creates safe deterministic capabilities for the configured browser. */
  public MutableCapabilities create(EnvironmentConfig configuration) {
    return switch (configuration.browser().toLowerCase(Locale.ROOT)) {
      case "chrome" -> chrome(configuration.headless());
      case "firefox" -> firefox(configuration.headless());
      case "edge" -> edge(configuration.headless());
      default ->
          throw new IllegalArgumentException("unsupported browser: " + configuration.browser());
    };
  }

  private static ChromeOptions chrome(boolean headless) {
    ChromeOptions options = new ChromeOptions();
    options.addArguments(
        "--window-size=1440,1000", "--disable-dev-shm-usage", "--no-default-browser-check");
    if (headless) {
      options.addArguments("--headless=new");
    }
    options.setCapability("se:recordVideo", false);
    return options;
  }

  private static FirefoxOptions firefox(boolean headless) {
    FirefoxOptions options = new FirefoxOptions();
    options.addArguments("--width=1440", "--height=1000");
    if (headless) {
      options.addArguments("-headless");
    }
    options.setCapability("se:recordVideo", false);
    return options;
  }

  private static EdgeOptions edge(boolean headless) {
    EdgeOptions options = new EdgeOptions();
    options.addArguments("--window-size=1440,1000", "--disable-dev-shm-usage");
    if (headless) {
      options.addArguments("--headless=new");
    }
    options.setCapability("se:recordVideo", false);
    return options;
  }
}
