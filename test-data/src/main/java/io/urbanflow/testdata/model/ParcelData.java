package io.urbanflow.testdata.model;

import java.math.BigDecimal;

/** Synthetic parcel dimensions and contents. */
public record ParcelData(
    BigDecimal weightKg, int lengthCm, int widthCm, int heightCm, String description) {

  /** Validates the normal business parcel boundary. */
  public ParcelData {
    if (weightKg == null
        || weightKg.compareTo(new BigDecimal("0.1")) < 0
        || weightKg.compareTo(new BigDecimal("50.0")) > 0) {
      throw new IllegalArgumentException("weightKg must be between 0.1 and 50.0");
    }
    if (lengthCm < 1
        || lengthCm > 200
        || widthCm < 1
        || widthCm > 200
        || heightCm < 1
        || heightCm > 200) {
      throw new IllegalArgumentException("parcel dimensions must be between 1 and 200 cm");
    }
    if (description == null || description.isBlank() || description.length() > 160) {
      throw new IllegalArgumentException("description must contain 1 to 160 characters");
    }
    description = description.trim();
  }
}
