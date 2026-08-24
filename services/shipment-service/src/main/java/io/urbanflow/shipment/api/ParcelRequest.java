package io.urbanflow.shipment.api;

import io.urbanflow.shipment.domain.ParcelSnapshot;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Validated API representation of parcel measurements and description. */
public record ParcelRequest(
    @Min(1) @Max(50_000) int weightGrams,
    @Min(1) @Max(200) int lengthCm,
    @Min(1) @Max(200) int widthCm,
    @Min(1) @Max(200) int heightCm,
    @NotBlank @Size(max = 240) String description) {

  /** Converts validated API data into the persisted domain snapshot. */
  public ParcelSnapshot toDomain() {
    return new ParcelSnapshot(weightGrams, lengthCm, widthCm, heightCm, description);
  }
}
