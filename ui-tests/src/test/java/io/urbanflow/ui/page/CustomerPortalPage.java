package io.urbanflow.ui.page;

import io.qameta.allure.Step;
import io.urbanflow.ui.component.ShipmentFormComponent;
import io.urbanflow.ui.component.ToastComponent;
import io.urbanflow.ui.support.BasePage;
import io.urbanflow.ui.support.PortalStateFixtures;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

/** Fluent page object for authentication, shipment, tracking, profile, and support journeys. */
public final class CustomerPortalPage extends BasePage<CustomerPortalPage> {

  private final Duration timeout;

  /** Creates the customer portal page. */
  public CustomerPortalPage(WebDriver driver, URI baseUrl, Duration timeout) {
    super(driver, baseUrl, timeout);
    this.timeout = timeout;
  }

  /** Registers a synthetic customer. */
  @Step("Register customer {email}")
  public CustomerPortalPage register(String name, String email, String password) {
    type(By.id("register-name"), name);
    type(By.id("register-email"), email);
    type(By.id("register-password"), password);
    click("register-submit");
    return this;
  }

  /** Signs in through the public form. */
  @Step("Sign in customer {email}")
  public CustomerPortalPage login(String email, String password) {
    type("login-email", email);
    type("login-password", password);
    click("login-submit");
    return this;
  }

  /** Opens a top-level customer view by stable navigation locator. */
  public CustomerPortalPage view(String view) {
    click("nav-" + view);
    return this;
  }

  /** Opens the reset form and submits an email. */
  public CustomerPortalPage reset(String email) {
    click("forgot-password");
    type("reset-email", email);
    click("reset-submit");
    return this;
  }

  /** Seeds deterministic customer state. */
  public CustomerPortalPage state(Map<String, Object> state) {
    return seed(PortalStateFixtures.CUSTOMER_STORE, state);
  }

  /** Returns a component object for shipment creation. */
  public ShipmentFormComponent shipmentForm() {
    return new ShipmentFormComponent(driver, timeout);
  }

  /** Returns the shared toast component. */
  public ToastComponent toast() {
    return new ToastComponent(driver, timeout);
  }

  /** Changes the UI language. */
  public CustomerPortalPage language(String language) {
    new Select(wait.visible(testId("language-select"))).selectByValue(language);
    return this;
  }

  /** Toggles color theme. */
  public CustomerPortalPage toggleTheme() {
    click("theme-toggle");
    return this;
  }

  /** Tracks the supplied public number. */
  public CustomerPortalPage track(String trackingNumber) {
    view("tracking");
    type("tracking-number", trackingNumber);
    click("track-submit");
    return this;
  }

  /** Opens the first shipment action. */
  public CustomerPortalPage firstShipmentAction(String action) {
    By locator =
        By.cssSelector("[data-testid='shipment-card'] button[data-action='" + action + "']");
    wait.clickable(locator).click();
    return this;
  }

  /** Filters owned shipments by status. */
  public CustomerPortalPage filterShipments(String status) {
    selectText("shipment-status-filter", status);
    return this;
  }

  /** Updates the browser prompt used by the pre-pickup address workflow. */
  public CustomerPortalPage acceptAddressPrompt(String address) {
    driver.switchTo().alert().sendKeys(address);
    driver.switchTo().alert().accept();
    return this;
  }

  /** Updates profile display name. */
  public CustomerPortalPage saveProfile(String name) {
    view("profile");
    type("profile-name", name);
    click("profile-save");
    return this;
  }

  /** Adds an address-book entry. */
  public CustomerPortalPage addAddress(String label, String address) {
    view("profile");
    type("address-label", label);
    type("address-value", address);
    click("address-add");
    return this;
  }

  /** Removes the first address-book entry. */
  public CustomerPortalPage removeFirstAddress() {
    wait.clickable(By.cssSelector("#address-list [data-address-id]")).click();
    return this;
  }

  /** Submits an incident for the first owned shipment. */
  public CustomerPortalPage reportIncident(String type, String description) {
    view("support");
    selectText("incident-type", type);
    type("incident-description", description);
    click("incident-submit");
    return this;
  }

  /** Returns whether a view is active and visible. */
  public boolean viewActive(String view) {
    return driver.findElement(By.id(view)).getAttribute("class").contains("active");
  }

  /** Waits for a top-level view to become the visible user outcome. */
  public CustomerPortalPage awaitView(String view) {
    wait.classPresent(By.id(view), "active");
    return this;
  }

  /** Returns the number of rendered shipment cards. */
  public int shipmentCards() {
    return count(By.cssSelector("[data-testid='shipment-card']"));
  }

  /** Returns the complete rendered owned-shipment list text. */
  public String shipmentList() {
    return text(By.id("shipment-list"));
  }

  /** Returns the number of rendered tracking events. */
  public int timelineEvents() {
    return count(By.cssSelector("[data-testid='tracking-result'] article"));
  }

  /** Returns the number of address-book entries. */
  public int addresses() {
    return count(By.cssSelector("#address-list [data-address-id]"));
  }

  /** Returns the root theme marker. */
  public String theme() {
    return driver.findElement(By.tagName("html")).getAttribute("data-theme");
  }

  /** Returns the page language. */
  public String pageLanguage() {
    return driver.findElement(By.tagName("html")).getAttribute("lang");
  }

  /** Returns a generic DOM element's text by ID. */
  public String idText(String id) {
    return text(By.id(id));
  }

  /** Returns an element's value by stable test ID. */
  public String fieldValue(String id) {
    return value(id);
  }

  /** Clicks the sign-out action. */
  public CustomerPortalPage logout() {
    click("logout-button");
    return this;
  }

  @Override
  protected CustomerPortalPage self() {
    return this;
  }
}
