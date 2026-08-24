package io.urbanflow.ui;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.urbanflow.testcore.context.TestContext;
import io.urbanflow.testcore.junit.UrbanFlowTest;
import io.urbanflow.ui.page.CourierPortalPage;
import io.urbanflow.ui.support.PortalServerExtension;
import io.urbanflow.ui.support.PortalStateFixtures;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Browser acceptance coverage for courier route execution and offline delivery evidence. */
@UrbanFlowTest
@Tag("ui")
@Epic("Courier route portal")
@Feature("Route and delivery execution")
@ExtendWith(PortalServerExtension.class)
class CourierPortalUiTest {

  @Test
  void courierCanSignInForShift(TestContext context) {
    CourierPortalPage page = fresh(context);

    page.login("123456", "ShiftCode25!");

    assertThat(page.assignments()).isEqualTo(3);
    assertThat(page.loginVisible()).isFalse();
  }

  @ParameterizedTest(name = "courier login rejects id={0}, code={1}")
  @CsvSource({"12345,ShiftCode25!", "ABCDEF,ShiftCode25!", "123456,short"})
  void courierLoginRejectsInvalidShiftCredentials(
      String courierId, String accessCode, TestContext context) {
    CourierPortalPage page = fresh(context);

    page.login(courierId, accessCode);

    assertThat(page.text("courier-login-message"))
        .isEqualTo("Enter a six-digit courier ID and a valid shift code.");
    assertThat(page.loginVisible()).isTrue();
  }

  @Test
  void assignmentUpdatesAreDisabledBeforeRouteStarts(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("COURIER_ASSIGNED", false, false));

    assertThat(page.firstUpdateEnabled()).isFalse();
    assertThat(page.text("network-status")).isEqualTo("Online");
  }

  @Test
  void courierCanStartAssignedRoute(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("COURIER_ASSIGNED", false, false));

    page.routeAction();

    assertThat(page.firstUpdateEnabled()).isTrue();
    assertThat(page.toast().activeText()).isEqualTo("Route started.");
  }

  @Test
  void routeCannotCompleteWhileStopsRemain(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("OUT_FOR_DELIVERY", true, false));

    page.routeAction();

    assertThat(page.toast().activeText())
        .isEqualTo("Complete all assignments before ending the route.");
    assertThat(page.firstUpdateEnabled()).isTrue();
  }

  @Test
  void fullyDeliveredRouteCanComplete(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("DELIVERED", true, false));

    page.routeAction();

    assertThat(page.toast().activeText()).isEqualTo("Route completed.");
    assertThat(page.firstUpdateEnabled()).isFalse();
  }

  @Test
  void assignmentFilterShowsMatchingStops(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("PICKED_UP", true, false));

    page.filter("PICKED_UP");

    assertThat(page.assignments()).isEqualTo(3);
    assertThat(page.firstAssignmentText()).contains("PICKED UP");
  }

  @Test
  void courierCanAdvanceValidStatusTransition(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("COURIER_ASSIGNED", true, false));

    page.openFirstUpdate().nextStatus("PICKUP_IN_PROGRESS").saveUpdate();

    assertThat(page.firstAssignmentText()).contains("PICKUP IN PROGRESS");
    assertThat(page.toast().activeText()).isEqualTo("Delivery status updated.");
  }

  @Test
  void unsuccessfulDeliveryRequiresReason(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("OUT_FOR_DELIVERY", true, false));

    page.openFirstUpdate().nextStatus("DELIVERY_ATTEMPTED").saveUpdate();

    assertThat(page.updateMessage()).isEqualTo("A reason is required for an unsuccessful attempt.");
  }

  @Test
  void unsuccessfulDeliveryPersistsReasonedStatus(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("OUT_FOR_DELIVERY", true, false));

    page.openFirstUpdate()
        .nextStatus("DELIVERY_ATTEMPTED")
        .reason("Recipient unavailable at the synthetic test address")
        .saveUpdate();

    assertThat(page.firstAssignmentText()).contains("DELIVERY ATTEMPTED");
    assertThat(page.storage(PortalStateFixtures.COURIER_STORE)).contains("Recipient unavailable");
  }

  @Test
  void completedDeliveryRequiresProof(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("OUT_FOR_DELIVERY", true, false));

    page.openFirstUpdate().nextStatus("DELIVERED").saveUpdate();

    assertThat(page.updateMessage()).isEqualTo("Delivery proof is required.");
  }

  @Test
  void completedDeliveryRejectsUnsupportedProofType(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("OUT_FOR_DELIVERY", true, false));

    page.openFirstUpdate()
        .nextStatus("DELIVERED")
        .proof(resource("invalid-proof.txt"))
        .saveUpdate();

    assertThat(page.updateMessage()).contains("JPEG, PNG, or PDF up to 5 MB");
  }

  @Test
  void completedDeliveryAcceptsPdfProof(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("OUT_FOR_DELIVERY", true, false));

    page.openFirstUpdate()
        .nextStatus("DELIVERED")
        .proof(resource("delivery-proof.pdf"))
        .saveUpdate();

    assertThat(page.firstAssignmentText()).contains("DELIVERED");
    assertThat(page.storage(PortalStateFixtures.COURIER_STORE)).contains("delivery-proof.pdf");
  }

  @Test
  void offlineUpdateQueuesAndSynchronizesAfterReconnect(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("COURIER_ASSIGNED", true, true));

    page.openFirstUpdate().nextStatus("PICKUP_IN_PROGRESS").saveUpdate();
    assertThat(page.text("offline-queue-count")).isEqualTo("1 queued");
    assertThat(page.toast().activeText()).contains("offline queue");
    page.offline(false);

    assertThat(page.text("network-status")).isEqualTo("Online");
    assertThat(page.text("offline-queue-count")).isEqualTo("0 queued");
    assertThat(page.toast().activeText()).isEqualTo("1 updates synchronized.");
  }

  @Test
  void courierCanSignOutWithoutLeakingWorkspace(TestContext context) {
    CourierPortalPage page =
        courier(context, PortalStateFixtures.courier("COURIER_ASSIGNED", false, false));

    page.logout();

    assertThat(page.loginVisible()).isTrue();
    assertThat(page.storage(PortalStateFixtures.COURIER_STORE)).contains("\"session\":null");
  }

  private static CourierPortalPage fresh(TestContext context) {
    return new CourierPortalPage(
            context.drivers().current(),
            context.environment().courierPortalUrl(),
            context.environment().timeout())
        .openFresh();
  }

  private static CourierPortalPage courier(
      TestContext context, java.util.Map<String, Object> state) {
    return fresh(context).state(state);
  }

  private static Path resource(String name) {
    return Path.of(
        System.getProperty("workspace.root"), "ui-tests", "src", "test", "resources", name);
  }
}
