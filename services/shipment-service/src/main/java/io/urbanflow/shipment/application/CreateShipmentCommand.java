package io.urbanflow.shipment.application;

import io.urbanflow.shipment.domain.AddressSnapshot;
import io.urbanflow.shipment.domain.DeliveryType;
import io.urbanflow.shipment.domain.ParcelSnapshot;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/** Defines all immutable input required to create a shipment. */
public record CreateShipmentCommand(
    UUID customerId,
    AddressSnapshot pickupAddress,
    AddressSnapshot deliveryAddress,
    ParcelSnapshot parcel,
    DeliveryType deliveryType) {

  /** Creates a complete shipment command. */
  public CreateShipmentCommand {
    Objects.requireNonNull(customerId, "customerId must not be null");
    Objects.requireNonNull(pickupAddress, "pickupAddress must not be null");
    Objects.requireNonNull(deliveryAddress, "deliveryAddress must not be null");
    Objects.requireNonNull(parcel, "parcel must not be null");
    Objects.requireNonNull(deliveryType, "deliveryType must not be null");
  }

  /** Returns a stable SHA-256 fingerprint for idempotency conflict detection. */
  public String fingerprint() {
    String canonical =
        String.join(
            "\u001e",
            customerId.toString(),
            pickupAddress.canonicalValue(),
            deliveryAddress.canonicalValue(),
            parcel.canonicalValue(),
            deliveryType.name());
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
    }
  }
}
