package io.urbanflow.testdata.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.urbanflow.testdata.factory.BoundaryDataSets;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TestDataModelTest {

  @Test
  void trimsAddressFields() {
    AddressData address = new AddressData(" Home ", " 100 Test Road ", " Sofia ", " 1000 ", " BG ");
    assertThat(address.label()).isEqualTo("Home");
    assertThat(address.street()).isEqualTo("100 Test Road");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\t"})
  void rejectsBlankAddressStreet(String street) {
    assertThatThrownBy(() -> new AddressData("Home", street, "Sofia", "1000", "BG"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.09", "50.01", "999"})
  void rejectsOutOfRangeParcelWeight(String weight) {
    assertThatThrownBy(() -> new ParcelData(new BigDecimal(weight), 10, 10, 10, "Parcel"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, 0, 201, Integer.MAX_VALUE})
  void rejectsOutOfRangeParcelDimension(int dimension) {
    assertThatThrownBy(() -> new ParcelData(new BigDecimal("1.0"), dimension, 10, 10, "Parcel"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsExactParcelBoundaries() {
    assertThat(new ParcelData(new BigDecimal("0.1"), 1, 1, 1, "x").weightKg())
        .isEqualByComparingTo("0.1");
    assertThat(new ParcelData(new BigDecimal("50.0"), 200, 200, 200, "x").lengthCm())
        .isEqualTo(200);
  }

  @ParameterizedTest
  @ValueSource(strings = {"123", "+00000000", "+359 88 000", "letters"})
  void rejectsInvalidShipmentPhone(String phone) {
    AddressData address = new AddressData("Test", "100 Road", "Sofia", "1000", "BG");
    ParcelData parcel = new ParcelData(new BigDecimal("1.0"), 10, 10, 10, "Parcel");
    assertThatThrownBy(
            () ->
                new ShipmentData(
                    UUID.randomUUID(), address, address, parcel, phone, "STANDARD", "key"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void boundaryDataSetsCoverBothSides() {
    assertThat(BoundaryDataSets.parcelWeights())
        .contains(
            new BigDecimal("0.09"),
            new BigDecimal("0.10"),
            new BigDecimal("50.00"),
            new BigDecimal("50.01"));
    assertThat(BoundaryDataSets.descriptionLengths()).containsExactly(0, 1, 159, 160, 161);
    assertThat(BoundaryDataSets.invalidPhones()).hasSizeGreaterThanOrEqualTo(5);
  }

  @Test
  void rejectsIncompleteUser() {
    assertThatThrownBy(
            () -> new UserData(UUID.randomUUID(), "Name", "invalid", "short", "CUSTOMER"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
