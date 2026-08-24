package io.urbanflow.ui.component;

import io.urbanflow.ui.support.UiWait;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Reusable transient-notification component shared by all three portals. */
public final class ToastComponent {

  private final WebDriver driver;
  private final UiWait wait;

  /** Creates a component bound to the current portal page. */
  public ToastComponent(WebDriver driver, Duration timeout) {
    this.driver = driver;
    this.wait = new UiWait(driver, timeout);
  }

  /** Waits for and returns the active notification. */
  public String activeText() {
    By locator = By.id("toast");
    wait.nonBlankText(locator);
    return driver.findElement(locator).getText();
  }
}
