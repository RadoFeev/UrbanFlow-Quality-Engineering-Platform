package io.urbanflow.testcore.browser;

import java.net.URI;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/** Creates remote browser sessions with validated Grid endpoints. */
public final class RemoteDriverProvider {

  /** Opens a new independent RemoteWebDriver session. */
  public WebDriver create(URI remoteUrl, MutableCapabilities capabilities) {
    if (remoteUrl == null || !remoteUrl.isAbsolute()) {
      throw new IllegalArgumentException("remoteUrl must be absolute");
    }
    try {
      return new RemoteWebDriver(remoteUrl.toURL(), capabilities);
    } catch (java.net.MalformedURLException exception) {
      throw new IllegalArgumentException("remoteUrl cannot be converted to a URL", exception);
    }
  }
}
