package io.urbanflow.testdata.factory;

import java.math.BigDecimal;
import java.util.List;

/** Reusable valid and invalid equivalence partitions for service and UI tests. */
public final class BoundaryDataSets {

  private BoundaryDataSets() {}

  /** Returns weight values at and immediately around accepted boundaries. */
  public static List<BigDecimal> parcelWeights() {
    return List.of(
        new BigDecimal("0.09"),
        new BigDecimal("0.10"),
        new BigDecimal("0.11"),
        new BigDecimal("49.99"),
        new BigDecimal("50.00"),
        new BigDecimal("50.01"));
  }

  /** Returns representative invalid phone partitions. */
  public static List<String> invalidPhones() {
    return List.of("", "123", "+00000000", "+359 88 000", "letters", "+3598800000000000");
  }

  /** Returns text sizes around the 160-character description boundary. */
  public static List<Integer> descriptionLengths() {
    return List.of(0, 1, 159, 160, 161);
  }
}
