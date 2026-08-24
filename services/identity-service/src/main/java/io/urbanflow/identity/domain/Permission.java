package io.urbanflow.identity.domain;

/** Fine-grained permissions embedded in authenticated identities. */
public enum Permission {
  SHIPMENT_CREATE,
  SHIPMENT_READ_OWN,
  SHIPMENT_CANCEL_OWN,
  COURIER_ASSIGNMENTS_READ,
  SHIPMENT_STATUS_UPDATE,
  OPERATIONS_READ,
  USER_MANAGE,
  ROLE_MANAGE,
  FEATURE_FLAG_MANAGE,
  AUDIT_READ
}
