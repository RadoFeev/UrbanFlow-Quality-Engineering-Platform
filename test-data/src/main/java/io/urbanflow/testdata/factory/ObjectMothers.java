package io.urbanflow.testdata.factory;

import io.urbanflow.testdata.builder.AddressBuilder;
import io.urbanflow.testdata.builder.ParcelBuilder;
import io.urbanflow.testdata.builder.ShipmentBuilder;
import io.urbanflow.testdata.builder.UserBuilder;
import io.urbanflow.testdata.generation.UniqueEmailGenerator;
import io.urbanflow.testdata.model.AddressData;
import io.urbanflow.testdata.model.ParcelData;
import io.urbanflow.testdata.model.ShipmentData;
import io.urbanflow.testdata.model.UserData;

/** Named valid defaults that remain customizable through builders. */
public final class ObjectMothers {

  private ObjectMothers() {}

  /** Returns a valid synthetic customer. */
  public static UserData customer(String runId) {
    return new UserBuilder(new UniqueEmailGenerator(runId)).build();
  }

  /** Returns a valid synthetic address. */
  public static AddressData address() {
    return new AddressBuilder().build();
  }

  /** Returns a valid synthetic parcel. */
  public static ParcelData parcel() {
    return new ParcelBuilder().build();
  }

  /** Returns a valid complete shipment. */
  public static ShipmentData shipment() {
    return new ShipmentBuilder().build();
  }
}
