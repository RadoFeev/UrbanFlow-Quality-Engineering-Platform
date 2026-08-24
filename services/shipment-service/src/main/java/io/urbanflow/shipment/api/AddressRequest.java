package io.urbanflow.shipment.api;

import io.urbanflow.shipment.domain.AddressSnapshot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Validated API representation of a shipment address. */
public record AddressRequest(
    @NotBlank @Size(max = 120) String contactName,
    @NotBlank @Size(max = 160) String line1,
    @Size(max = 160) String line2,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 20) String postalCode,
    @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode,
    @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{7,14}") String phone) {

  /** Converts validated API data into the persisted domain snapshot. */
  public AddressSnapshot toDomain() {
    return new AddressSnapshot(contactName, line1, line2, city, postalCode, countryCode, phone);
  }
}
