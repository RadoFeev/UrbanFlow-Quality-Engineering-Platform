package io.urbanflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

  @Test
  void customerHasOnlyOwnedShipmentCapabilities() {
    assertThat(Role.CUSTOMER.permissions())
        .containsExactlyInAnyOrder(
            Permission.SHIPMENT_CREATE,
            Permission.SHIPMENT_READ_OWN,
            Permission.SHIPMENT_CANCEL_OWN);
  }

  @Test
  void courierCannotAdministerUsers() {
    assertThat(Role.COURIER.permissions())
        .contains(Permission.COURIER_ASSIGNMENTS_READ, Permission.SHIPMENT_STATUS_UPDATE)
        .doesNotContain(Permission.USER_MANAGE, Permission.ROLE_MANAGE);
  }

  @Test
  void operatorCanReadAuditWithoutManagingRoles() {
    assertThat(Role.OPERATOR.permissions())
        .contains(Permission.OPERATIONS_READ, Permission.AUDIT_READ)
        .doesNotContain(Permission.ROLE_MANAGE);
  }

  @Test
  void adminHasEveryPermission() {
    assertThat(Role.ADMIN.permissions()).containsExactlyInAnyOrder(Permission.values());
  }
}
