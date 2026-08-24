package io.urbanflow.testdata.builder;

import io.urbanflow.testdata.model.ParcelData;
import java.math.BigDecimal;

/** Fluent builder for parcel boundaries. */
public final class ParcelBuilder {

  private BigDecimal weight = new BigDecimal("2.5");
  private int length = 30;
  private int width = 20;
  private int height = 15;
  private String description = "Synthetic test parcel";

  /** Changes parcel weight. */
  public ParcelBuilder withWeight(String value) {
    weight = new BigDecimal(value);
    return this;
  }

  /** Changes all dimensions. */
  public ParcelBuilder withDimensions(int lengthValue, int widthValue, int heightValue) {
    length = lengthValue;
    width = widthValue;
    height = heightValue;
    return this;
  }

  /** Changes safe contents description. */
  public ParcelBuilder withDescription(String value) {
    description = value;
    return this;
  }

  /** Builds an immutable parcel. */
  public ParcelData build() {
    return new ParcelData(weight, length, width, height, description);
  }
}
