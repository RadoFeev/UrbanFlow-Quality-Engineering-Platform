package io.urbanflow.ui.page;

import io.qameta.allure.Step;
import io.urbanflow.ui.component.PaginationComponent;
import io.urbanflow.ui.component.ToastComponent;
import io.urbanflow.ui.support.BasePage;
import io.urbanflow.ui.support.PortalStateFixtures;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Fluent page object for operational dashboards, permissions, and versioned commands. */
public final class OperationsDashboardPage extends BasePage<OperationsDashboardPage> {

  private final Duration timeout;

  /** Creates the operations dashboard page. */
  public OperationsDashboardPage(WebDriver driver, URI baseUrl, Duration timeout) {
    super(driver, baseUrl, timeout);
    this.timeout = timeout;
  }

  /** Signs in with an explicit workspace role. */
  @Step("Sign in operations user {operatorId} as {role}")
  public OperationsDashboardPage login(String operatorId, String accessCode, String role) {
    type("operator-id", operatorId);
    type("operator-code", accessCode);
    selectText("workspace-role", role);
    click("ops-login-submit");
    return this;
  }

  /** Seeds a browser-owned operations session. */
  public OperationsDashboardPage state(Map<String, Object> state) {
    return seed(PortalStateFixtures.OPERATIONS_STORE, state);
  }

  /** Opens an operational panel. */
  public OperationsDashboardPage panel(String panel) {
    click("ops-nav-" + panel);
    return this;
  }

  /** Searches the shipment table. */
  public OperationsDashboardPage searchShipments(String query) {
    type("ops-shipment-search", query);
    return this;
  }

  /** Filters shipments by status. */
  public OperationsDashboardPage filterShipments(String status) {
    selectText("ops-status-filter", status);
    return this;
  }

  /** Sorts the shipment table using its stable value. */
  public OperationsDashboardPage sortShipments(String sort) {
    select("shipment-sort", sort);
    return this;
  }

  /** Opens the first manual status command. */
  public OperationsDashboardPage openFirstManualStatus() {
    wait.clickable(By.cssSelector("#shipments-body [data-action='manual-status']")).click();
    wait.visible(By.id("status-dialog"));
    return this;
  }

  /** Configures and submits the open manual status command. */
  public OperationsDashboardPage manualStatus(
      String status, String reason, boolean simulateConflict) {
    selectText("manual-status", status);
    type("manual-reason", reason);
    WebElement conflict = wait.visible(testId("simulate-conflict"));
    if (conflict.isSelected() != simulateConflict) {
      conflict.click();
    }
    click("manual-status-submit");
    return this;
  }

  /** Exports the currently filtered shipment table. */
  public OperationsDashboardPage exportCsv() {
    click("export-csv");
    return this;
  }

  /** Changes the first user's selected role and saves it. */
  public OperationsDashboardPage changeFirstUserRole(String role) {
    WebElement row = driver.findElement(By.cssSelector("#users-body tr"));
    new org.openqa.selenium.support.ui.Select(row.findElement(By.cssSelector("[data-role]")))
        .selectByVisibleText(role);
    row.findElement(By.cssSelector("[data-action='save-role']")).click();
    return this;
  }

  /** Toggles the first feature flag. */
  public OperationsDashboardPage toggleFirstFlag() {
    wait.clickable(By.cssSelector("#flag-list [data-action='toggle-flag']")).click();
    return this;
  }

  /** Resolves the first open incident. */
  public OperationsDashboardPage resolveFirstIncident() {
    wait.clickable(By.cssSelector("#incident-list [data-action='resolve']")).click();
    return this;
  }

  /** Searches audit history. */
  public OperationsDashboardPage searchAudit(String query) {
    type("audit-search", query);
    return this;
  }

  /** Returns the pagination component. */
  public PaginationComponent pagination() {
    return new PaginationComponent(driver, timeout);
  }

  /** Returns the shared toast component. */
  public ToastComponent toast() {
    return new ToastComponent(driver, timeout);
  }

  /** Returns shipment table row count. */
  public int shipmentRows() {
    return count(By.cssSelector("#shipments-body tr"));
  }

  /** Returns the shipment table text. */
  public String shipmentTable() {
    return text(By.id("shipments-body"));
  }

  /** Returns the first visible shipment tracking number. */
  public String firstShipmentTracking() {
    return text(By.cssSelector("#shipments-body tr:first-child td:first-child"));
  }

  /** Returns the complete incident-list text. */
  public String incidentList() {
    return text(By.id("incident-list"));
  }

  /** Returns the complete feature-flag list text. */
  public String flagList() {
    return text(By.id("flag-list"));
  }

  /** Returns a generic DOM element's text by ID. */
  public String idText(String id) {
    return text(By.id(id));
  }

  /** Returns the users table text. */
  public String usersTable() {
    return text(By.id("users-body"));
  }

  /** Returns the audit table text. */
  public String auditTable() {
    return text(By.id("audit-body"));
  }

  /** Returns the manual status dialog's error text. */
  public String manualMessage() {
    return text(By.id("manual-message"));
  }

  /** Returns whether an admin-only navigation item is marked disabled. */
  public String adminNavigationDisabled(String panel) {
    return attribute("ops-nav-" + panel, "aria-disabled");
  }

  /** Returns whether the first row's manual action is enabled. */
  public boolean firstManualActionEnabled() {
    return driver
        .findElement(By.cssSelector("#shipments-body [data-action='manual-status']"))
        .isEnabled();
  }

  /** Returns whether the first user role selector is enabled. */
  public boolean firstUserRoleEnabled() {
    return driver.findElement(By.cssSelector("#users-body [data-role]")).isEnabled();
  }

  /** Returns the number of network resources matching a selector. */
  public int networkResources(String listId) {
    return count(By.cssSelector("#" + listId + " .resource"));
  }

  /** Returns the active panel marker. */
  public boolean panelActive(String panel) {
    return driver.findElement(By.id(panel)).getAttribute("class").contains("active");
  }

  /** Signs out the operations user. */
  public OperationsDashboardPage logout() {
    click("ops-logout");
    return this;
  }

  @Override
  protected OperationsDashboardPage self() {
    return this;
  }
}
