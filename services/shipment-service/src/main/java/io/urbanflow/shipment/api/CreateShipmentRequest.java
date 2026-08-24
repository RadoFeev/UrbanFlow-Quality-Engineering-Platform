package io.urbanflow.shipment.api;

import io.urbanflow.shipment.application.CreateShipmentCommand;
import io.urbanflow.shipment.domain.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Validated request body for shipment creation. */
public record CreateShipmentRequest(
    @NotNull UUID customerId,
    @NotNull @Valid AddressRequest pickupAddress,
    @NotNull @Valid AddressRequest deliveryAddress,
    @NotNull @Valid ParcelRequest parcel,
    @NotNull DeliveryType deliveryType) {

  /** Converts validated API data into an application command. */
  public CreateShipmentCommand toCommand() {
    return new CreateShipmentCommand(
        customerId,
        pickupAddress.toDomain(),
        deliveryAddress.toDomain(),
        parcel.toDomain(),
        deliveryType);
  }
}
