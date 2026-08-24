package io.urbanflow.identity.domain;

import java.util.Set;

/** Supported platform roles and their immutable permission sets. */
public enum Role {
  CUSTOMER(
      Permission.SHIPMENT_CREATE, Permission.SHIPMENT_READ_OWN, Permission.SHIPMENT_CANCEL_OWN),
  COURIER(Permission.COURIER_ASSIGNMENTS_READ, Permission.SHIPMENT_STATUS_UPDATE),
  OPERATOR(Permission.OPERATIONS_READ, Permission.SHIPMENT_STATUS_UPDATE, Permission.AUDIT_READ),
  ADMIN(Permission.values());

  private final Set<Permission> permissions;

  Role(Permission... permissions) {
    this.permissions = Set.of(permissions);
  }

  /** Returns an immutable permission view. */
  public Set<Permission> permissions() {
    return permissions;
  }
}
