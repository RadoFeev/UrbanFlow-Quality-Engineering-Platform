package io.urbanflow.ui;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.urbanflow.testcore.context.TestContext;
import io.urbanflow.testcore.junit.UrbanFlowTest;
import io.urbanflow.ui.page.OperationsDashboardPage;
import io.urbanflow.ui.support.PortalServerExtension;
import io.urbanflow.ui.support.PortalStateFixtures;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Browser acceptance coverage for operational visibility, RBAC, and versioned commands. */
@UrbanFlowTest
@Tag("ui")
@Epic("Operations dashboard")
@Feature("Operations control plane")
@ExtendWith(PortalServerExtension.class)
class OperationsDashboardUiTest {

  @Test
  void operatorCanSignInToTheDashboard(TestContext context) {
    OperationsDashboardPage page = fresh(context);

    page.login("QA-0001", "Operations!25", "OPERATOR");

    assertThat(page.panelActive("overview")).isTrue();
    assertThat(page.text("metric-active")).isEqualTo("12");
  }

  @ParameterizedTest(name = "operations login rejects id={0}, code={1}")
  @CsvSource({"Q-0001,Operations!25", "quality,Operations!25", "QA-0001,short"})
  void operationsLoginRejectsInvalidCredentials(
      String operatorId, String accessCode, TestContext context) {
    OperationsDashboardPage page = fresh(context);

    page.login(operatorId, accessCode, "OPERATOR");

    assertThat(page.text("ops-login-message"))
        .isEqualTo("Enter a valid operator ID and access code.");
    assertThat(page.panelActive("login")).isTrue();
  }

  @Test
  void overviewRendersLiveOperationalMetrics(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR");

    assertThat(page.text("metric-active")).isEqualTo("12");
    assertThat(page.idText("metric-incidents")).isEqualTo("2");
    assertThat(page.idText("metric-couriers")).isEqualTo("2");
    assertThat(page.text("problem-deliveries")).contains("Destination hub processing delay");
  }

  @Test
  void nonAdministratorCannotOpenAdminPanels(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR");

    page.panel("users");

    assertThat(page.adminNavigationDisabled("users")).isEqualTo("true");
    assertThat(page.panelActive("overview")).isTrue();
    assertThat(page.toast().activeText()).isEqualTo("Administrator permission is required.");
  }

  @Test
  void viewerCannotExecuteShipmentCommands(TestContext context) {
    OperationsDashboardPage page = operations(context, "VIEWER");

    page.panel("shipments");

    assertThat(page.firstManualActionEnabled()).isFalse();
  }

  @Test
  void shipmentSearchFindsTrackingNumber(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");

    page.searchShipments("UF-OP000014");

    assertThat(page.shipmentRows()).isOne();
    assertThat(page.shipmentTable()).contains("UF-OP000014");
  }

  @Test
  void shipmentSearchFindsRouteCity(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");

    page.searchShipments("Burgas");

    assertThat(page.shipmentRows()).isGreaterThan(1);
    assertThat(page.shipmentTable()).contains("Burgas");
  }

  @Test
  void shipmentStatusFilterReducesResultSet(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");

    page.filterShipments("DELIVERED");

    assertThat(page.shipmentRows()).isEqualTo(2);
    assertThat(page.shipmentTable()).contains("DELIVERED").doesNotContain("IN_TRANSIT");
  }

  @Test
  void shipmentsCanBeSortedByTrackingNumber(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");

    page.sortShipments("tracking-asc");

    assertThat(page.firstShipmentTracking()).isEqualTo("UF-OP000001");
  }

  @Test
  void shipmentTableSupportsBoundedPagination(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");

    assertThat(page.pagination().status()).isEqualTo("Page 1 of 3");
    page.pagination().next();
    assertThat(page.pagination().status()).isEqualTo("Page 2 of 3");
    page.pagination().previous();

    assertThat(page.pagination().status()).isEqualTo("Page 1 of 3");
  }

  @Test
  void currentShipmentViewCanBeExported(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");

    page.filterShipments("DELIVERED").exportCsv();

    assertThat(page.toast().activeText()).isEqualTo("CSV export prepared.");
    assertThat(page.storage(PortalStateFixtures.OPERATIONS_STORE))
        .contains("SHIPMENTS_EXPORTED", "2 records");
  }

  @Test
  void manualStatusChangeRequiresReason(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");

    page.openFirstManualStatus().manualStatus("IN_TRANSIT", "", false);

    assertThat(page.manualMessage()).isEqualTo("A reason is required for a manual change.");
  }

  @Test
  void optimisticLockConflictPreservesShipmentState(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");

    page.openFirstManualStatus().manualStatus("IN_TRANSIT", "Synthetic concurrent update", true);

    assertThat(page.manualMessage()).startsWith("Version conflict: expected");
  }

  @Test
  void operatorCannotCancelShipment(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");

    page.openFirstManualStatus().manualStatus("CANCELLED", "Requested by test actor", false);

    assertThat(page.manualMessage())
        .isEqualTo("Administrator permission is required to cancel a shipment.");
  }

  @Test
  void operatorCanApplyVersionedShipmentStatus(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");

    page.openFirstManualStatus()
        .manualStatus("IN_TRANSIT", "Verified by automated operations scenario", false);

    assertThat(page.toast().activeText()).isEqualTo("Shipment status updated.");
    assertThat(page.storage(PortalStateFixtures.OPERATIONS_STORE))
        .contains("SHIPMENT_STATUS_CHANGED", "reason=Verified by automated operations scenario");
  }

  @Test
  void administratorCanChangeUserRole(TestContext context) {
    OperationsDashboardPage page = operations(context, "ADMIN").panel("users");

    page.changeFirstUserRole("ADMIN");

    assertThat(page.usersTable()).contains("ADMIN", "v1");
    assertThat(page.idText("users-message")).isEqualTo("Role updated.");
  }

  @Test
  void administratorCanToggleVersionedFeatureFlag(TestContext context) {
    OperationsDashboardPage page = operations(context, "ADMIN").panel("flags");

    page.toggleFirstFlag();

    assertThat(page.flagList()).contains("routing.v2", "Enabled", "v1");
    assertThat(page.toast().activeText()).isEqualTo("Feature flag updated.");
  }

  @Test
  void networkPanelShowsCouriersAndHubs(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("network");

    assertThat(page.networkResources("courier-list")).isEqualTo(3);
    assertThat(page.networkResources("hub-list")).isEqualTo(3);
  }

  @Test
  void operatorCanResolveAnOpenIncident(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("incidents");

    page.resolveFirstIncident();

    assertThat(page.incidentList()).contains("OPEN · v1").doesNotContain("OPEN · v0");
    assertThat(page.toast().activeText()).isEqualTo("Incident resolved.");
  }

  @Test
  void auditSearchFindsVersionedCommand(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR").panel("shipments");
    page.openFirstManualStatus().manualStatus("IN_TRANSIT", "Audit search fixture", false);

    page.panel("audit").searchAudit("shipment_status");

    assertThat(page.auditTable()).contains("SHIPMENT_STATUS_CHANGED", "Audit search fixture");
  }

  @Test
  void operationsLogoutRecordsSessionEnd(TestContext context) {
    OperationsDashboardPage page = operations(context, "OPERATOR");

    page.logout();

    assertThat(page.panelActive("login")).isTrue();
    assertThat(page.storage(PortalStateFixtures.OPERATIONS_STORE))
        .contains("SESSION_ENDED", "\"session\":null");
  }

  private static OperationsDashboardPage fresh(TestContext context) {
    return new OperationsDashboardPage(
            context.drivers().current(),
            context.environment().operationsDashboardUrl(),
            context.environment().timeout())
        .openFresh();
  }

  private static OperationsDashboardPage operations(TestContext context, String role) {
    return fresh(context).state(PortalStateFixtures.operationsSession(role));
  }
}
