package io.urbanflow.testdata.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.urbanflow.testdata.factory.ObjectMothers;
import io.urbanflow.testdata.generation.UniqueEmailGenerator;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuilderAndMotherTest {

  @Test
  void addressBuilderOverridesSelectedFields() {
    var address =
        new AddressBuilder()
            .withLabel("Destination")
            .withStreet("200 Synthetic Avenue")
            .withCity("Varna")
            .withPostalCode("9000")
            .build();
    assertThat(address.label()).isEqualTo("Destination");
    assertThat(address.city()).isEqualTo("Varna");
    assertThat(address.postalCode()).isEqualTo("9000");
  }

  @Test
  void parcelBuilderCreatesBoundaryVariant() {
    var parcel =
        new ParcelBuilder()
            .withWeight("50.0")
            .withDimensions(200, 1, 100)
            .withDescription("Boundary parcel")
            .build();
    assertThat(parcel.weightKg()).isEqualByComparingTo("50.0");
    assertThat(parcel.lengthCm()).isEqualTo(200);
  }

  @Test
  void userBuilderCreatesUniqueSyntheticCustomer() {
    UniqueEmailGenerator emails = new UniqueEmailGenerator("suite-1");
    var first = new UserBuilder(emails).withName("Synthetic One").build();
    var second = new UserBuilder(emails).build();
    assertThat(first.name()).isEqualTo("Synthetic One");
    assertThat(first.email()).isNotEqualTo(second.email());
  }

  @Test
  void userBuilderSupportsExplicitRoleAndEmail() {
    var user =
        new UserBuilder(new UniqueEmailGenerator("suite-1"))
            .withEmail("courier@example.test")
            .withPassword("Courier-Test-42!")
            .withRole("COURIER")
            .build();
    assertThat(user.role()).isEqualTo("COURIER");
    assertThat(user.email()).isEqualTo("courier@example.test");
  }

  @Test
  void shipmentBuilderComposesIndependentObjects() {
    UUID customerId = UUID.randomUUID();
    var shipment =
        new ShipmentBuilder()
            .forCustomer(customerId)
            .from(new AddressBuilder().withLabel("Origin").build())
            .to(new AddressBuilder().withLabel("Destination").build())
            .withParcel(new ParcelBuilder().withWeight("0.1").build())
            .withPhone("+359880000099")
            .withDeliveryType("EXPRESS")
            .withIdempotencyKey("case-42")
            .build();
    assertThat(shipment.customerId()).isEqualTo(customerId);
    assertThat(shipment.deliveryType()).isEqualTo("EXPRESS");
    assertThat(shipment.idempotencyKey()).isEqualTo("case-42");
  }

  @Test
  void objectMothersReturnValidIndependentDefaults() {
    assertThat(ObjectMothers.customer("run-1").email()).endsWith("@example.test");
    assertThat(ObjectMothers.address().city()).isEqualTo("Sofia");
    assertThat(ObjectMothers.parcel().description()).isNotBlank();
    assertThat(ObjectMothers.shipment().pickup()).isNotSameAs(ObjectMothers.shipment().pickup());
  }
}
