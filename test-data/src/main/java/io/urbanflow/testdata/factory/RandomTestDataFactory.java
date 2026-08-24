package io.urbanflow.testdata.factory;

import io.urbanflow.testdata.builder.AddressBuilder;
import io.urbanflow.testdata.builder.ParcelBuilder;
import io.urbanflow.testdata.builder.ShipmentBuilder;
import io.urbanflow.testdata.builder.UserBuilder;
import io.urbanflow.testdata.generation.DeterministicRandom;
import io.urbanflow.testdata.generation.UniqueEmailGenerator;
import io.urbanflow.testdata.model.AddressData;
import io.urbanflow.testdata.model.ParcelData;
import io.urbanflow.testdata.model.ShipmentData;
import io.urbanflow.testdata.model.UserData;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Produces valid varied test data from a recorded deterministic seed. */
public final class RandomTestDataFactory {

  private static final String[] CITIES = {"Sofia", "Plovdiv", "Varna", "Burgas", "Ruse"};
  private static final String[] DELIVERY_TYPES = {"STANDARD", "EXPRESS", "SAME_DAY"};
  private final long seed;
  private final DeterministicRandom random;
  private final UniqueEmailGenerator emails;

  /** Creates a run-scoped factory whose seed can be recorded in test evidence. */
  public RandomTestDataFactory(long seed, String runId) {
    this.seed = seed;
    this.random = new DeterministicRandom(seed);
    this.emails = new UniqueEmailGenerator(runId);
  }

  /** Returns the seed required to replay generated values. */
  public long seed() {
    return seed;
  }

  /** Produces a valid synthetic customer with no real personal data. */
  public synchronized UserData customer() {
    String token = random.alphanumeric(8);
    return new UserBuilder(emails)
        .withId(stableUuid("user-" + token))
        .withName("Synthetic Customer " + token)
        .build();
  }

  /** Produces a valid synthetic Bulgarian address. */
  public synchronized AddressData address(String label) {
    int cityIndex = random.integer(0, CITIES.length);
    return new AddressBuilder()
        .withLabel(label)
        .withStreet(random.integer(1, 999) + " Synthetic Route")
        .withCity(CITIES[cityIndex])
        .withPostalCode(String.valueOf(random.integer(1000, 9999)))
        .build();
  }

  /** Produces a parcel inside the supported weight and dimension boundaries. */
  public synchronized ParcelData parcel() {
    String weight = "%d.%d".formatted(random.integer(1, 49), random.integer(0, 10));
    return new ParcelBuilder()
        .withWeight(weight)
        .withDimensions(random.integer(1, 200), random.integer(1, 200), random.integer(1, 200))
        .withDescription("Synthetic parcel " + random.alphanumeric(10))
        .build();
  }

  /** Produces a complete valid shipment whose components vary together. */
  public synchronized ShipmentData shipment() {
    String token = random.alphanumeric(12);
    return new ShipmentBuilder()
        .forCustomer(stableUuid("customer-" + token))
        .from(address("Pickup"))
        .to(address("Delivery"))
        .withParcel(parcel())
        .withPhone("+35988" + random.integer(10_000_000, 99_999_999))
        .withDeliveryType(DELIVERY_TYPES[random.integer(0, DELIVERY_TYPES.length)])
        .withIdempotencyKey("test-" + token)
        .build();
  }

  private UUID stableUuid(String value) {
    return UUID.nameUUIDFromBytes((seed + ":" + value).getBytes(StandardCharsets.UTF_8));
  }
}
