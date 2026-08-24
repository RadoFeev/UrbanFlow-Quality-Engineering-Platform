package io.urbanflow.api.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

@Tag("api")
class OpenApiOperationContractTest {

  private static final Set<String> EXPECTED_OPERATIONS =
      Set.of(
          "registerCustomer",
          "login",
          "refreshSession",
          "logout",
          "requestPasswordReset",
          "confirmPasswordReset",
          "getCurrentUser",
          "changeUserRole",
          "createShipment",
          "getShipment",
          "trackShipment",
          "transitionShipment",
          "changeDeliveryAddress",
          "getTrackingTimeline",
          "getRoute",
          "listCourierAssignments",
          "startRoute",
          "completeRoute",
          "listCustomerNotifications",
          "searchWarehouses",
          "createWarehouse",
          "getWarehouse",
          "updateWarehouse",
          "listWarehouseHubs",
          "createWarehouseHub",
          "setHubActive",
          "listIncidents",
          "openIncident",
          "resolveIncident",
          "listFeatureFlags",
          "createFeatureFlag",
          "setFeatureFlag",
          "listAuditRecords");

  private final List<OpenApiCatalogue.Operation> operations = OpenApiCatalogue.operations();

  @Test
  void publishedOperationInventoryIsCompleteAndUnique() {
    assertThat(operations)
        .extracting(OpenApiCatalogue.Operation::operationId)
        .doesNotHaveDuplicates()
        .containsExactlyInAnyOrderElementsOf(EXPECTED_OPERATIONS);
  }

  @TestFactory
  Stream<DynamicTest> everyOperationHasStableIdentifier() {
    return operations.stream()
        .map(
            operation ->
                DynamicTest.dynamicTest(
                    operation.displayName() + " has operationId",
                    () -> assertThat(operation.operationId()).matches("[a-z][A-Za-z0-9]+")));
  }

  @TestFactory
  Stream<DynamicTest> everyOperationDocumentsSuccessResponse() {
    return operations.stream()
        .map(
            operation ->
                DynamicTest.dynamicTest(
                    operation.displayName() + " documents success",
                    () ->
                        assertThat(operation.responses().keySet())
                            .anyMatch(status -> String.valueOf(status).matches("2\\d\\d"))));
  }

  @TestFactory
  Stream<DynamicTest> everyOperationDocumentsFailureResponse() {
    return operations.stream()
        .map(
            operation ->
                DynamicTest.dynamicTest(
                    operation.displayName() + " documents failure",
                    () ->
                        assertThat(operation.responses().keySet())
                            .anyMatch(
                                status ->
                                    String.valueOf(status).equals("default")
                                        || String.valueOf(status).matches("[45]\\d\\d"))));
  }

  @TestFactory
  Stream<DynamicTest> everyOperationHasAnOwnershipTag() {
    return operations.stream()
        .map(
            operation ->
                DynamicTest.dynamicTest(
                    operation.displayName() + " is tagged",
                    () ->
                        assertThat((List<?>) operation.contract().get("tags"))
                            .isNotEmpty()
                            .doesNotContainNull()));
  }
}
