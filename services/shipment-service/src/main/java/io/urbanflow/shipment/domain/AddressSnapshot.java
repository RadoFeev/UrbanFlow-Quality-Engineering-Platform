package io.urbanflow.shipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/** Immutable-at-business-level address data stored with a shipment. */
@Embeddable
public class AddressSnapshot {

  @Column(name = "contact_name", nullable = false, length = 120)
  private String contactName;

  @Column(name = "line_1", nullable = false, length = 160)
  private String line1;

  @Column(name = "line_2", length = 160)
  private String line2;

  @Column(name = "city", nullable = false, length = 100)
  private String city;

  @Column(name = "postal_code", nullable = false, length = 20)
  private String postalCode;

  @Column(name = "country_code", nullable = false, length = 2)
  private String countryCode;

  @Column(name = "phone", nullable = false, length = 16)
  private String phone;

  /** Constructor reserved for persistence materialization. */
  protected AddressSnapshot() {}

  /** Creates a complete shipment address snapshot. */
  public AddressSnapshot(
      String contactName,
      String line1,
      String line2,
      String city,
      String postalCode,
      String countryCode,
      String phone) {
    this.contactName = requireText(contactName, "contactName");
    this.line1 = requireText(line1, "line1");
    this.line2 = line2;
    this.city = requireText(city, "city");
    this.postalCode = requireText(postalCode, "postalCode");
    this.countryCode = requireText(countryCode, "countryCode");
    this.phone = requireText(phone, "phone");
  }

  /** Returns the synthetic or customer-provided contact name. */
  public String contactName() {
    return contactName;
  }

  /** Returns the primary street-address line. */
  public String line1() {
    return line1;
  }

  /** Returns the optional secondary street-address line. */
  public String line2() {
    return line2;
  }

  /** Returns the city or locality. */
  public String city() {
    return city;
  }

  /** Returns the postal code. */
  public String postalCode() {
    return postalCode;
  }

  /** Returns the two-letter country code. */
  public String countryCode() {
    return countryCode;
  }

  /** Returns the international contact phone number. */
  public String phone() {
    return phone;
  }

  /** Returns a stable representation used by idempotency fingerprinting. */
  public String canonicalValue() {
    return String.join(
        "\u001f",
        contactName,
        line1,
        Objects.requireNonNullElse(line2, ""),
        city,
        postalCode,
        countryCode,
        phone);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
