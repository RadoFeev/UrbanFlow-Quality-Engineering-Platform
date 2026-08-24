package io.urbanflow.shipment.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Validated request body for changing a shipment delivery address. */
public record ChangeDeliveryAddressRequest(@NotNull @Valid AddressRequest deliveryAddress) {}
