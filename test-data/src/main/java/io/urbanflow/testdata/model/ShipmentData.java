package io.urbanflow.testdata.model;

import java.util.UUID;

/** Complete synthetic shipment creation payload. */
public record ShipmentData(
    UUID customerId,
    AddressData pickup,
    AddressData delivery,
    ParcelData parcel,
    String recipientPhone,
    String deliveryType,
    String idempotencyKey) {

  /** Validates required shipment fields. */
  public ShipmentData {
    if (customerId == null || pickup == null || delivery == null || parcel == null) {
      throw new IllegalArgumentException("shipment ownership, addresses, and parcel are required");
    }
    if (recipientPhone == null || !recipientPhone.matches("\\+?[1-9]\\d{7,14}")) {
      throw new IllegalArgumentException("recipient phone format is invalid");
    }
    if (!java.util.Set.of("STANDARD", "EXPRESS", "SAME_DAY").contains(deliveryType)) {
      throw new IllegalArgumentException("delivery type is invalid");
    }
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("idempotency key is required");
    }
  }
}
