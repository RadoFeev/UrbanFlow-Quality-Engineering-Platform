package io.urbanflow.testcore.browser;

import io.urbanflow.testcore.config.EnvironmentConfig;
import java.time.Duration;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/** Opens locally managed or Grid browser sessions with explicit timeout policy. */
public final class DriverFactory {

  private final BrowserOptionsFactory optionsFactory;
  private final RemoteDriverProvider remoteProvider;

  /** Creates the browser factory. */
  public DriverFactory(BrowserOptionsFactory optionsFactory, RemoteDriverProvider remoteProvider) {
    this.optionsFactory = optionsFactory;
    this.remoteProvider = remoteProvider;
  }

  /** Opens a fresh browser session for a single test thread. */
  public WebDriver create(EnvironmentConfig configuration) {
    MutableCapabilities options = optionsFactory.create(configuration);
    WebDriver driver =
        configuration.remoteUrl() == null
            ? local(configuration.browser(), options)
            : remoteProvider.create(configuration.remoteUrl(), options);
    driver.manage().timeouts().implicitlyWait(Duration.ZERO);
    driver.manage().timeouts().pageLoadTimeout(configuration.timeout().multipliedBy(2));
    driver.manage().timeouts().scriptTimeout(configuration.timeout());
    return driver;
  }

  private static WebDriver local(String browser, MutableCapabilities options) {
    return switch (browser) {
      case "chrome" -> new ChromeDriver((ChromeOptions) options);
      case "firefox" -> new FirefoxDriver((FirefoxOptions) options);
      case "edge" -> new EdgeDriver((EdgeOptions) options);
      default -> throw new IllegalArgumentException("unsupported browser: " + browser);
    };
  }
}
