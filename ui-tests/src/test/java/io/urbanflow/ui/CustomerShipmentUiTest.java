package io.urbanflow.ui;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.urbanflow.testcore.context.TestContext;
import io.urbanflow.testcore.junit.UrbanFlowTest;
import io.urbanflow.ui.component.ShipmentFormComponent;
import io.urbanflow.ui.component.ShipmentFormComponent.ShipmentInput;
import io.urbanflow.ui.page.CustomerPortalPage;
import io.urbanflow.ui.support.PortalServerExtension;
import io.urbanflow.ui.support.PortalStateFixtures;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Data-driven browser coverage for customer shipment, tracking, and support journeys. */
@UrbanFlowTest
@Tag("ui")
@Epic("Customer delivery portal")
@Feature("Shipment self-service")
@ExtendWith(PortalServerExtension.class)
class CustomerShipmentUiTest {

  @Test
  void customerCanCreateValidShipment(TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer()).view("create");

    page.shipmentForm().fill(ShipmentInput.valid()).submit();

    assertThat(page.shipmentForm().message()).matches("Shipment UF-[A-Z0-9]{8} was created\\.");
    assertThat(page.storage(PortalStateFixtures.CUSTOMER_STORE)).contains("\"status\":\"CREATED\"");
  }

  @ParameterizedTest(name = "shipment requires {0} address")
  @ValueSource(strings = {"pickup", "delivery"})
  void shipmentRequiresBothAddresses(String field, TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer()).view("create");

    page.shipmentForm().fill(changed(ShipmentInput.valid(), field, " ")).submit();

    assertThat(page.shipmentForm().message())
        .isEqualTo("Pickup and delivery addresses are required.");
  }

  @ParameterizedTest(name = "shipment rejects phone {0}")
  @ValueSource(strings = {"123", "+0123456789", "+359 888 123", "phone-number"})
  void shipmentRejectsInvalidInternationalPhone(String phone, TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer()).view("create");

    page.shipmentForm().fill(changed(ShipmentInput.valid(), "phone", phone)).submit();

    assertThat(page.shipmentForm().message())
        .isEqualTo("Enter a valid international phone number.");
  }

  @ParameterizedTest(name = "shipment rejects weight {0}")
  @ValueSource(strings = {"0", "0.09", "50.1", "999"})
  void shipmentRejectsWeightOutsideSupportedRange(String weight, TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer()).view("create");

    page.shipmentForm().fill(changed(ShipmentInput.valid(), "weight", weight)).submit();

    assertThat(page.shipmentForm().message()).isEqualTo("Weight must be between 0.1 and 50 kg.");
  }

  @ParameterizedTest(name = "shipment rejects {0} dimension {1}")
  @CsvSource({"length,0", "length,201", "width,-1", "width,250", "height,0", "height,999"})
  void shipmentRejectsInvalidDimensions(String field, String value, TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer()).view("create");

    page.shipmentForm().fill(changed(ShipmentInput.valid(), field, value)).submit();

    assertThat(page.shipmentForm().message())
        .isEqualTo("Every dimension must be between 1 and 200 cm.");
  }

  @ParameterizedTest(name = "delivery estimate supports {0}")
  @ValueSource(strings = {"STANDARD", "EXPRESS", "SAME_DAY"})
  void everyDeliveryServiceRendersAnEstimate(String service, TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer()).view("create");
    ShipmentFormComponent form = page.shipmentForm();

    form.deliveryType(service);

    assertThat(form.value("delivery-type")).isEqualTo(service);
    assertThat(form.estimate()).startsWith("Estimated delivery:").doesNotEndWith("—");
  }

  @Test
  void descriptionIsBoundedByTheBrowserContract(TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer()).view("create");
    String oversized = "quality".repeat(40);

    page.shipmentForm().fill(changed(ShipmentInput.valid(), "description", oversized));

    assertThat(page.shipmentForm().value("parcel-description")).hasSize(160);
  }

  @Test
  void shipmentFormResetsAfterSuccessfulSubmission(TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer()).view("create");

    page.shipmentForm().fill(ShipmentInput.valid()).submit();

    assertThat(page.shipmentForm().value("pickup-address")).isEmpty();
    assertThat(page.shipmentForm().value("parcel-weight")).isEmpty();
  }

  @Test
  void publicTrackingRejectsMalformedNumbers(TestContext context) {
    CustomerPortalPage page =
        customer(context, PortalStateFixtures.customerWithShipment("CREATED"));

    page.track("invalid");

    assertThat(page.text("tracking-message")).isEqualTo("Tracking number format is invalid.");
  }

  @Test
  void publicTrackingDoesNotExposeUnknownShipments(TestContext context) {
    CustomerPortalPage page =
        customer(context, PortalStateFixtures.customerWithShipment("CREATED"));

    page.track("UF-00000000");

    assertThat(page.text("tracking-message"))
        .isEqualTo("No shipment was found for this tracking number.");
  }

  @Test
  void publicTrackingShowsCurrentStatusAndHistory(TestContext context) {
    CustomerPortalPage page =
        customer(context, PortalStateFixtures.customerWithShipment("IN_TRANSIT"));

    page.track("uf-a1b2c3d4");

    assertThat(page.text("tracking-message")).isEqualTo("Current status: IN_TRANSIT");
    assertThat(page.timelineEvents()).isEqualTo(2);
  }

  @Test
  void ownedShipmentCanOpenItsTimeline(TestContext context) {
    CustomerPortalPage page =
        customer(context, PortalStateFixtures.customerWithShipment("CONFIRMED"));

    page.view("shipments").firstShipmentAction("track");

    assertThat(page.viewActive("tracking")).isTrue();
    assertThat(page.timelineEvents()).isEqualTo(2);
  }

  @Test
  void prePickupShipmentCanBeCancelled(TestContext context) {
    CustomerPortalPage page =
        customer(context, PortalStateFixtures.customerWithShipment("CREATED"));

    page.view("shipments").firstShipmentAction("cancel");

    assertThat(page.shipmentList()).contains("CANCELLED");
    assertThat(page.toast().activeText()).isEqualTo("Shipment cancelled.");
  }

  @Test
  void prePickupDeliveryAddressCanBeChanged(TestContext context) {
    CustomerPortalPage page =
        customer(context, PortalStateFixtures.customerWithShipment("CONFIRMED"));

    page.view("shipments")
        .firstShipmentAction("change-address")
        .acceptAddressPrompt("3 Updated Quality Boulevard");

    assertThat(page.shipmentList()).contains("3 Updated Quality Boulevard");
    assertThat(page.toast().activeText()).isEqualTo("Delivery address updated.");
  }

  @Test
  void shipmentStatusFilterCanReturnAnEmptyView(TestContext context) {
    CustomerPortalPage page =
        customer(context, PortalStateFixtures.customerWithShipment("CREATED"));

    page.view("shipments").filterShipments("DELIVERED");

    assertThat(page.shipmentCards()).isZero();
    assertThat(page.shipmentList()).contains("No shipments match");
  }

  @Test
  void customerCanUpdateProfileName(TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer());

    page.saveProfile("Updated Synthetic Customer");

    assertThat(page.idText("profile-message")).isEqualTo("Profile updated.");
    assertThat(page.storage(PortalStateFixtures.CUSTOMER_STORE))
        .contains("Updated Synthetic Customer");
  }

  @Test
  void customerCanManageAddressBook(TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer());

    page.addAddress("Test office", "42 Automation Street");
    assertThat(page.addresses()).isOne();
    page.removeFirstAddress();

    assertThat(page.addresses()).isZero();
  }

  @Test
  void customerCanReportShipmentIncident(TestContext context) {
    CustomerPortalPage page =
        customer(context, PortalStateFixtures.customerWithShipment("IN_TRANSIT"));

    page.reportIncident("DAMAGE", "Synthetic parcel was damaged during automated handling.");

    assertThat(page.idText("incident-message")).isEqualTo("Your report was submitted.");
    assertThat(page.storage(PortalStateFixtures.CUSTOMER_STORE)).contains("\"type\":\"DAMAGE\"");
  }

  @Test
  void incidentRequiresOwnedShipmentAndDescription(TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer());

    page.reportIncident("OTHER", "");

    assertThat(page.idText("incident-message"))
        .isEqualTo("Select a shipment and describe the issue.");
  }

  @Test
  void userContentIsRenderedAsTextInsteadOfMarkup(TestContext context) {
    CustomerPortalPage page = customer(context, PortalStateFixtures.customer()).view("create");
    ShipmentInput input =
        changed(ShipmentInput.valid(), "pickup", "<script>window.compromised=true</script>");

    page.shipmentForm().fill(input).submit();
    page.view("shipments");

    assertThat(page.shipmentList()).contains("<script>window.compromised=true</script>");
    assertThat(page.storage(PortalStateFixtures.CUSTOMER_STORE)).contains("<script>");
  }

  private static CustomerPortalPage customer(TestContext context, Map<String, Object> state) {
    return new CustomerPortalPage(
            context.drivers().current(),
            context.environment().customerPortalUrl(),
            context.environment().timeout())
        .openFresh()
        .state(state);
  }

  private static ShipmentInput changed(ShipmentInput input, String field, String value) {
    return new ShipmentInput(
        field.equals("pickup") ? value : input.pickup(),
        field.equals("delivery") ? value : input.delivery(),
        field.equals("phone") ? value : input.phone(),
        input.deliveryType(),
        field.equals("weight") ? value : input.weight(),
        field.equals("length") ? value : input.length(),
        field.equals("width") ? value : input.width(),
        field.equals("height") ? value : input.height(),
        field.equals("description") ? value : input.description());
  }
}
