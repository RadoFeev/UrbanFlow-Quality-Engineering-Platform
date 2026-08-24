package io.urbanflow.testdata.model;

/** Synthetic delivery address without real personal data. */
public record AddressData(
    String label, String street, String city, String postalCode, String country) {

  /** Validates required synthetic address fields. */
  public AddressData {
    label = require(label, "label");
    street = require(street, "street");
    city = require(city, "city");
    postalCode = require(postalCode, "postalCode");
    country = require(country, "country");
  }

  private static String require(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }
}
