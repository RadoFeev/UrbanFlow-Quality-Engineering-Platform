package io.urbanflow.testdata.builder;

import io.urbanflow.testdata.model.AddressData;
import io.urbanflow.testdata.model.ParcelData;
import io.urbanflow.testdata.model.ShipmentData;
import java.util.UUID;

/** Fluent builder for complete shipment requests. */
public final class ShipmentBuilder {

  private UUID customerId = UUID.randomUUID();
  private AddressData pickup = new AddressBuilder().withLabel("Pickup").build();
  private AddressData delivery =
      new AddressBuilder().withLabel("Delivery").withStreet("200 Test Avenue").build();
  private ParcelData parcel = new ParcelBuilder().build();
  private String phone = "+359880000001";
  private String deliveryType = "STANDARD";
  private String idempotencyKey = UUID.randomUUID().toString();

  /** Changes customer ownership. */
  public ShipmentBuilder forCustomer(UUID value) {
    customerId = value;
    return this;
  }

  /** Changes pickup address. */
  public ShipmentBuilder from(AddressData value) {
    pickup = value;
    return this;
  }

  /** Changes delivery address. */
  public ShipmentBuilder to(AddressData value) {
    delivery = value;
    return this;
  }

  /** Changes parcel. */
  public ShipmentBuilder withParcel(ParcelData value) {
    parcel = value;
    return this;
  }

  /** Changes recipient phone. */
  public ShipmentBuilder withPhone(String value) {
    phone = value;
    return this;
  }

  /** Changes delivery type. */
  public ShipmentBuilder withDeliveryType(String value) {
    deliveryType = value;
    return this;
  }

  /** Changes idempotency key. */
  public ShipmentBuilder withIdempotencyKey(String value) {
    idempotencyKey = value;
    return this;
  }

  /** Builds an immutable shipment request. */
  public ShipmentData build() {
    return new ShipmentData(
        customerId, pickup, delivery, parcel, phone, deliveryType, idempotencyKey);
  }
}
