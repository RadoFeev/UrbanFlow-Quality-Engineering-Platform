package io.urbanflow.ui.component;

import io.urbanflow.ui.support.UiWait;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Reusable previous/next pagination control for operational result sets. */
public final class PaginationComponent {

  private final WebDriver driver;
  private final UiWait wait;

  /** Creates the pagination component. */
  public PaginationComponent(WebDriver driver, Duration timeout) {
    this.driver = driver;
    this.wait = new UiWait(driver, timeout);
  }

  /** Opens the next page. */
  public PaginationComponent next() {
    wait.clickable(By.cssSelector("[data-testid='next-page']")).click();
    return this;
  }

  /** Opens the previous page. */
  public PaginationComponent previous() {
    wait.clickable(By.cssSelector("[data-testid='previous-page']")).click();
    return this;
  }

  /** Returns the stable page status text. */
  public String status() {
    return driver.findElement(By.cssSelector("[data-testid='page-status']")).getText();
  }
}
