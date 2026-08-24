package io.urbanflow.ui.page;

import io.qameta.allure.Step;
import io.urbanflow.ui.component.ToastComponent;
import io.urbanflow.ui.support.BasePage;
import io.urbanflow.ui.support.PortalStateFixtures;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Fluent page object for courier route, status, proof, and offline synchronization flows. */
public final class CourierPortalPage extends BasePage<CourierPortalPage> {

  private final Duration timeout;

  /** Creates the courier portal page. */
  public CourierPortalPage(WebDriver driver, URI baseUrl, Duration timeout) {
    super(driver, baseUrl, timeout);
    this.timeout = timeout;
  }

  /** Signs in with shift credentials. */
  @Step("Sign in courier {courierId}")
  public CourierPortalPage login(String courierId, String accessCode) {
    type("courier-id", courierId);
    type("access-code", accessCode);
    click("courier-login-submit");
    return this;
  }

  /** Seeds deterministic courier state. */
  public CourierPortalPage state(Map<String, Object> state) {
    return seed(PortalStateFixtures.COURIER_STORE, state);
  }

  /** Starts or completes the route through its current action. */
  public CourierPortalPage routeAction() {
    wait.clickable(By.id("route-toggle")).click();
    return this;
  }

  /** Filters assignment cards by status. */
  public CourierPortalPage filter(String status) {
    selectText("assignment-filter", status);
    return this;
  }

  /** Opens the first assignment status dialog. */
  public CourierPortalPage openFirstUpdate() {
    wait.clickable(By.cssSelector("[data-testid='assignment-card'] [data-action='update']"))
        .click();
    wait.visible(testId("status-dialog"));
    return this;
  }

  /** Selects a target status in the open update dialog. */
  public CourierPortalPage nextStatus(String status) {
    selectText("next-status", status);
    return this;
  }

  /** Adds the required unsuccessful-attempt reason. */
  public CourierPortalPage reason(String reason) {
    type("failure-reason", reason);
    return this;
  }

  /** Uploads delivery proof through the browser file input. */
  public CourierPortalPage proof(Path file) {
    wait.visible(testId("delivery-proof")).sendKeys(file.toAbsolutePath().toString());
    return this;
  }

  /** Saves the open status update. */
  public CourierPortalPage saveUpdate() {
    click("save-status-update");
    return this;
  }

  /** Enables or disables offline simulation. */
  public CourierPortalPage offline(boolean offline) {
    WebElement toggle = wait.visible(testId("offline-toggle"));
    if (toggle.isSelected() != offline) {
      toggle.click();
    }
    return this;
  }

  /** Synchronizes queued changes. */
  public CourierPortalPage synchronize() {
    click("sync-button");
    return this;
  }

  /** Signs out the courier. */
  public CourierPortalPage logout() {
    click("courier-logout");
    return this;
  }

  /** Returns the shared toast component. */
  public ToastComponent toast() {
    return new ToastComponent(driver, timeout);
  }

  /** Returns rendered assignment count. */
  public int assignments() {
    return count(By.cssSelector("[data-testid='assignment-card']"));
  }

  /** Returns whether the first assignment action is enabled. */
  public boolean firstUpdateEnabled() {
    return driver
        .findElement(By.cssSelector("[data-testid='assignment-card'] [data-action='update']"))
        .isEnabled();
  }

  /** Returns the first rendered assignment card's complete text. */
  public String firstAssignmentText() {
    return text(By.cssSelector("[data-testid='assignment-card']"));
  }

  /** Returns the update dialog message. */
  public String updateMessage() {
    return text(By.id("update-message"));
  }

  /** Returns the login-panel visibility state. */
  public boolean loginVisible() {
    return driver.findElement(By.id("login-panel")).isDisplayed();
  }

  @Override
  protected CourierPortalPage self() {
    return this;
  }
}
