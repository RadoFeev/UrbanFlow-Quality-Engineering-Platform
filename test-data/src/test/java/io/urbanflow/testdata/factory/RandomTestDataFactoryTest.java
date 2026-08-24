package io.urbanflow.testdata.factory;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RandomTestDataFactoryTest {

  @Test
  void sameSeedAndRunReproduceTheSameShipment() {
    var first = new RandomTestDataFactory(42L, "run-42").shipment();
    var replay = new RandomTestDataFactory(42L, "run-42").shipment();
    assertThat(replay).isEqualTo(first);
  }

  @Test
  void successiveValuesRemainUniqueAndValid() {
    RandomTestDataFactory factory = new RandomTestDataFactory(9L, "run-09");
    var first = factory.customer();
    var second = factory.customer();
    assertThat(first.id()).isNotEqualTo(second.id());
    assertThat(first.email()).isNotEqualTo(second.email()).endsWith("@example.test");
    assertThat(factory.seed()).isEqualTo(9L);
  }

  @Test
  void generatedShipmentUsesSupportedPartitions() {
    var shipment = new RandomTestDataFactory(81L, "run-81").shipment();
    assertThat(shipment.pickup()).isNotEqualTo(shipment.delivery());
    assertThat(shipment.parcel().weightKg())
        .isBetween(new BigDecimal("0.1"), new BigDecimal("50.0"));
    assertThat(shipment.deliveryType()).isIn("STANDARD", "EXPRESS", "SAME_DAY");
    assertThat(shipment.recipientPhone()).matches("\\+35988\\d{8}");
  }
}
