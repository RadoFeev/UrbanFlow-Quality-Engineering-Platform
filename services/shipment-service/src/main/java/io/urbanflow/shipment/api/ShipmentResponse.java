package io.urbanflow.shipment.api;

import io.urbanflow.shipment.domain.AddressSnapshot;
import io.urbanflow.shipment.domain.DeliveryType;
import io.urbanflow.shipment.domain.ParcelSnapshot;
import io.urbanflow.shipment.domain.Shipment;
import io.urbanflow.shipment.domain.ShipmentStatus;
import java.time.Instant;
import java.util.UUID;

/** Public shipment representation returned by creation, query, and update endpoints. */
public record ShipmentResponse(
    UUID id,
    UUID customerId,
    String trackingNumber,
    ShipmentStatus status,
    DeliveryType deliveryType,
    AddressResponse pickupAddress,
    AddressResponse deliveryAddress,
    ParcelResponse parcel,
    long version,
    Instant createdAt,
    Instant updatedAt) {

  /** Maps a persisted shipment aggregate to its API representation. */
  public static ShipmentResponse from(Shipment shipment) {
    return new ShipmentResponse(
        shipment.id(),
        shipment.customerId(),
        shipment.trackingNumber(),
        shipment.status(),
        shipment.deliveryType(),
        AddressResponse.from(shipment.pickupAddress()),
        AddressResponse.from(shipment.deliveryAddress()),
        ParcelResponse.from(shipment.parcel()),
        shipment.version(),
        shipment.createdAt(),
        shipment.updatedAt());
  }

  /** API representation of a persisted address snapshot. */
  public record AddressResponse(
      String contactName,
      String line1,
      String line2,
      String city,
      String postalCode,
      String countryCode,
      String phone) {

    private static AddressResponse from(AddressSnapshot address) {
      return new AddressResponse(
          address.contactName(),
          address.line1(),
          address.line2(),
          address.city(),
          address.postalCode(),
          address.countryCode(),
          address.phone());
    }
  }

  /** API representation of a persisted parcel snapshot. */
  public record ParcelResponse(
      int weightGrams, int lengthCm, int widthCm, int heightCm, String description) {

    private static ParcelResponse from(ParcelSnapshot parcel) {
      return new ParcelResponse(
          parcel.weightGrams(),
          parcel.lengthCm(),
          parcel.widthCm(),
          parcel.heightCm(),
          parcel.description());
    }
  }
}
