package io.urbanflow.shipment.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.urbanflow.shipment.domain.AddressSnapshot;
import io.urbanflow.shipment.domain.DeliveryType;
import io.urbanflow.shipment.domain.ParcelSnapshot;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateShipmentCommandTest {

  @Test
  void createsStableFingerprintForEquivalentInput() {
    CreateShipmentCommand first = command("Synthetic parcel");
    CreateShipmentCommand retry = command("Synthetic parcel");

    assertThat(first.fingerprint())
        .hasSize(64)
        .isEqualTo(retry.fingerprint())
        .matches("[0-9a-f]{64}");
  }

  @Test
  void changesFingerprintWhenBusinessInputChanges() {
    CreateShipmentCommand original = command("Synthetic parcel");
    CreateShipmentCommand changed = command("Different parcel");

    assertThat(original.fingerprint()).isNotEqualTo(changed.fingerprint());
  }

  private static CreateShipmentCommand command(String description) {
    AddressSnapshot pickup =
        new AddressSnapshot(
            "Test Sender", "10 Example Street", null, "Sofia", "1000", "BG", "+359888000111");
    AddressSnapshot delivery =
        new AddressSnapshot(
            "Test Recipient", "20 Sample Avenue", null, "Plovdiv", "4000", "BG", "+359888000222");
    return new CreateShipmentCommand(
        UUID.fromString("08b8e8d9-694f-4576-bc56-f4870afe9795"),
        pickup,
        delivery,
        new ParcelSnapshot(1000, 20, 20, 20, description),
        DeliveryType.STANDARD);
  }
}
