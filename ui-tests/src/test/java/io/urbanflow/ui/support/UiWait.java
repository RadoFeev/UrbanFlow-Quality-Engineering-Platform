package io.urbanflow.ui.support;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Central explicit-wait policy and UrbanFlow-specific expected conditions. */
public final class UiWait {

  private final WebDriver driver;
  private final WebDriverWait wait;

  /** Creates a polling wait with ignored transient DOM lookup failures. */
  public UiWait(WebDriver driver, Duration timeout) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, timeout);
  }

  /** Waits for a visible element. */
  public WebElement visible(By locator) {
    return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
  }

  /** Waits for an element that accepts interaction. */
  public WebElement clickable(By locator) {
    return wait.until(ExpectedConditions.elementToBeClickable(locator));
  }

  /** Waits for text to appear in an element. */
  public void text(By locator, String expected) {
    wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, expected));
  }

  /**
   * Waits until an element contains any non-blank text, including after a transient animation ends.
   */
  public void nonBlankText(By locator) {
    wait.until(
        ignored -> {
          String value = driver.findElement(locator).getText();
          return value != null && !value.isBlank();
        });
  }

  /** Waits for an exact element count and returns the matched elements. */
  public List<WebElement> count(By locator, int expected) {
    return wait.until(
        ignored -> {
          List<WebElement> elements = driver.findElements(locator);
          return elements.size() == expected ? elements : null;
        });
  }

  /** Waits for a CSS class to be absent from the target. */
  public void classAbsent(By locator, String className) {
    wait.until(cssClassAbsent(locator, className));
  }

  /** Waits for an exact CSS class token to be present. */
  public void classPresent(By locator, String className) {
    wait.until(
        ignored -> {
          String classes = driver.findElement(locator).getAttribute("class");
          return classes != null && java.util.Set.of(classes.split("\\s+")).contains(className);
        });
  }

  /** Custom condition for asynchronous class-driven rendering. */
  public static ExpectedCondition<Boolean> cssClassAbsent(By locator, String className) {
    return driver -> {
      String classes = driver.findElement(locator).getAttribute("class");
      return classes == null || !java.util.Set.of(classes.split("\\s+")).contains(className);
    };
  }
}
