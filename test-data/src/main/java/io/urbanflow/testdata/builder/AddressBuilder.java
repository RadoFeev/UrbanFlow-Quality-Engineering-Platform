package io.urbanflow.testdata.builder;

import io.urbanflow.testdata.model.AddressData;

/** Fluent builder for synthetic address variants. */
public final class AddressBuilder {

  private String label = "Test pickup";
  private String street = "100 Test Route";
  private String city = "Sofia";
  private String postalCode = "1000";
  private String country = "BG";

  /** Changes the address label. */
  public AddressBuilder withLabel(String value) {
    label = value;
    return this;
  }

  /** Changes the street. */
  public AddressBuilder withStreet(String value) {
    street = value;
    return this;
  }

  /** Changes the city. */
  public AddressBuilder withCity(String value) {
    city = value;
    return this;
  }

  /** Changes the postal code. */
  public AddressBuilder withPostalCode(String value) {
    postalCode = value;
    return this;
  }

  /** Builds an immutable address. */
  public AddressData build() {
    return new AddressData(label, street, city, postalCode, country);
  }
}
