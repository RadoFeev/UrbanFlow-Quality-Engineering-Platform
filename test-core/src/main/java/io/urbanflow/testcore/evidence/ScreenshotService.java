package io.urbanflow.testcore.evidence;

import java.nio.charset.StandardCharsets;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/** Captures browser screenshot and page source without writing shared files. */
public final class ScreenshotService {

  /** Captures a PNG from a screenshot-capable driver. */
  public byte[] screenshot(WebDriver driver) {
    if (!(driver instanceof TakesScreenshot source)) {
      throw new IllegalArgumentException("driver does not support screenshots");
    }
    return source.getScreenshotAs(OutputType.BYTES);
  }

  /** Captures current page source as UTF-8. */
  public byte[] pageSource(WebDriver driver) {
    return driver.getPageSource().getBytes(StandardCharsets.UTF_8);
  }
}
