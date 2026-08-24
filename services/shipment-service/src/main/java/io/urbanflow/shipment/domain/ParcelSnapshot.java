package io.urbanflow.shipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Parcel measurements and description captured when a shipment is created. */
@Embeddable
public class ParcelSnapshot {

  @Column(name = "weight_grams", nullable = false)
  private int weightGrams;

  @Column(name = "length_cm", nullable = false)
  private int lengthCm;

  @Column(name = "width_cm", nullable = false)
  private int widthCm;

  @Column(name = "height_cm", nullable = false)
  private int heightCm;

  @Column(name = "description", nullable = false, length = 240)
  private String description;

  /** Constructor reserved for persistence materialization. */
  protected ParcelSnapshot() {}

  /** Creates validated positive measurements for a parcel. */
  public ParcelSnapshot(
      int weightGrams, int lengthCm, int widthCm, int heightCm, String description) {
    this.weightGrams = requirePositive(weightGrams, "weightGrams");
    this.lengthCm = requirePositive(lengthCm, "lengthCm");
    this.widthCm = requirePositive(widthCm, "widthCm");
    this.heightCm = requirePositive(heightCm, "heightCm");
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description must not be blank");
    }
    this.description = description;
  }

  /** Returns the parcel weight in grams. */
  public int weightGrams() {
    return weightGrams;
  }

  /** Returns the parcel length in centimetres. */
  public int lengthCm() {
    return lengthCm;
  }

  /** Returns the parcel width in centimetres. */
  public int widthCm() {
    return widthCm;
  }

  /** Returns the parcel height in centimetres. */
  public int heightCm() {
    return heightCm;
  }

  /** Returns the safe parcel description. */
  public String description() {
    return description;
  }

  /** Returns a stable representation used by idempotency fingerprinting. */
  public String canonicalValue() {
    return "%d\u001f%d\u001f%d\u001f%d\u001f%s"
        .formatted(weightGrams, lengthCm, widthCm, heightCm, description);
  }

  private static int requirePositive(int value, String field) {
    if (value <= 0) {
      throw new IllegalArgumentException(field + " must be positive");
    }
    return value;
  }
}
