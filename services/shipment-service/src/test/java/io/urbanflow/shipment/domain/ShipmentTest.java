package io.urbanflow.shipment.domain;

import static io.urbanflow.shipment.domain.ShipmentActor.COURIER;
import static io.urbanflow.shipment.domain.ShipmentActor.SYSTEM;
import static io.urbanflow.shipment.domain.ShipmentStatus.CONFIRMED;
import static io.urbanflow.shipment.domain.ShipmentStatus.DELIVERED;
import static io.urbanflow.shipment.domain.ShipmentStatus.PICKED_UP;
import static io.urbanflow.shipment.domain.ShipmentStatus.PICKUP_IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShipmentTest {

  private static final Instant CREATED_AT = Instant.parse("2026-08-24T08:00:00Z");
  private static final UUID SHIPMENT_ID = UUID.fromString("aa7ad9d8-f943-42da-90c1-6291566b4740");
  private static final UUID CUSTOMER_ID = UUID.fromString("08b8e8d9-694f-4576-bc56-f4870afe9795");

  private final ShipmentTransitionPolicy policy = new ShipmentTransitionPolicy();

  @Test
  void createsCompleteShipmentInCreatedState() {
    Shipment shipment = shipment();

    assertThat(shipment.id()).isEqualTo(SHIPMENT_ID);
    assertThat(shipment.customerId()).isEqualTo(CUSTOMER_ID);
    assertThat(shipment.trackingNumber()).isEqualTo("UF0123456789ABCDEF01");
    assertThat(shipment.status()).isEqualTo(ShipmentStatus.CREATED);
    assertThat(shipment.version()).isZero();
    assertThat(shipment.createdAt()).isEqualTo(CREATED_AT);
    assertThat(shipment.updatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void appliesLegalTransitionAndUpdatesTimestamp() {
    Shipment shipment = shipment();
    Instant confirmedAt = CREATED_AT.plusSeconds(30);

    shipment.transition(
        0, new ShipmentTransitionCommand(CONFIRMED, SYSTEM, null, null), policy, confirmedAt);

    assertThat(shipment.status()).isEqualTo(CONFIRMED);
    assertThat(shipment.updatedAt()).isEqualTo(confirmedAt);
  }

  @Test
  void rejectsStaleVersionBeforeMutatingAggregate() {
    Shipment shipment = shipment();
    ShipmentTransitionCommand command =
        new ShipmentTransitionCommand(CONFIRMED, SYSTEM, null, null);

    assertThatThrownBy(() -> shipment.transition(4, command, policy, CREATED_AT.plusSeconds(30)))
        .isInstanceOf(ShipmentVersionConflictException.class)
        .hasMessageContaining("version 0")
        .hasMessageContaining("version 4");
    assertThat(shipment.status()).isEqualTo(ShipmentStatus.CREATED);
    assertThat(shipment.updatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void changesDeliveryAddressBeforePickup() {
    Shipment shipment = shipment();
    AddressSnapshot replacement =
        new AddressSnapshot(
            "Test Recipient", "42 Updated Street", null, "Sofia", "1000", "BG", "+359888000222");
    Instant changedAt = CREATED_AT.plusSeconds(60);

    shipment.changeDeliveryAddress(0, replacement, changedAt);

    assertThat(shipment.deliveryAddress()).isSameAs(replacement);
    assertThat(shipment.updatedAt()).isEqualTo(changedAt);
  }

  @Test
  void rejectsDeliveryAddressChangeAfterPickupStarts() {
    Shipment shipment = shipment();
    shipment.transition(
        0,
        new ShipmentTransitionCommand(CONFIRMED, SYSTEM, null, null),
        policy,
        CREATED_AT.plusSeconds(10));
    shipment.transition(
        0,
        new ShipmentTransitionCommand(
            ShipmentStatus.COURIER_ASSIGNED, ShipmentActor.ROUTING_SERVICE, null, null),
        policy,
        CREATED_AT.plusSeconds(20));
    shipment.transition(
        0,
        new ShipmentTransitionCommand(PICKUP_IN_PROGRESS, COURIER, null, null),
        policy,
        CREATED_AT.plusSeconds(30));
    AddressSnapshot original = shipment.deliveryAddress();

    assertThatThrownBy(
            () -> shipment.changeDeliveryAddress(0, pickupAddress(), CREATED_AT.plusSeconds(40)))
        .isInstanceOf(DeliveryAddressChangeNotAllowedException.class)
        .hasMessageContaining(PICKUP_IN_PROGRESS.name());
    assertThat(shipment.deliveryAddress()).isSameAs(original);
  }

  @Test
  void reachesDeliveredOnlyWithProof() {
    Shipment shipment = shipment();
    shipment.transition(
        0,
        new ShipmentTransitionCommand(CONFIRMED, SYSTEM, null, null),
        policy,
        CREATED_AT.plusSeconds(1));
    shipment.transition(
        0,
        new ShipmentTransitionCommand(
            ShipmentStatus.COURIER_ASSIGNED, ShipmentActor.ROUTING_SERVICE, null, null),
        policy,
        CREATED_AT.plusSeconds(2));
    shipment.transition(
        0,
        new ShipmentTransitionCommand(PICKUP_IN_PROGRESS, COURIER, null, null),
        policy,
        CREATED_AT.plusSeconds(3));
    shipment.transition(
        0,
        new ShipmentTransitionCommand(PICKED_UP, COURIER, null, null),
        policy,
        CREATED_AT.plusSeconds(4));
    shipment.transition(
        0,
        new ShipmentTransitionCommand(
            ShipmentStatus.AT_ORIGIN_HUB, ShipmentActor.WAREHOUSE_OPERATOR, null, null),
        policy,
        CREATED_AT.plusSeconds(5));
    shipment.transition(
        0,
        new ShipmentTransitionCommand(
            ShipmentStatus.IN_TRANSIT, ShipmentActor.WAREHOUSE_OPERATOR, null, null),
        policy,
        CREATED_AT.plusSeconds(6));
    shipment.transition(
        0,
        new ShipmentTransitionCommand(
            ShipmentStatus.AT_DESTINATION_HUB, ShipmentActor.WAREHOUSE_OPERATOR, null, null),
        policy,
        CREATED_AT.plusSeconds(7));
    shipment.transition(
        0,
        new ShipmentTransitionCommand(ShipmentStatus.OUT_FOR_DELIVERY, COURIER, null, null),
        policy,
        CREATED_AT.plusSeconds(8));

    shipment.transition(
        0,
        new ShipmentTransitionCommand(DELIVERED, COURIER, null, UUID.randomUUID()),
        policy,
        CREATED_AT.plusSeconds(9));

    assertThat(shipment.status()).isEqualTo(DELIVERED);
    assertThat(shipment.status().isTerminal()).isTrue();
  }

  private static Shipment shipment() {
    return Shipment.create(
        SHIPMENT_ID,
        CUSTOMER_ID,
        "UF0123456789ABCDEF01",
        DeliveryType.EXPRESS,
        pickupAddress(),
        deliveryAddress(),
        new ParcelSnapshot(1_250, 30, 20, 10, "Synthetic test parcel"),
        CREATED_AT);
  }

  private static AddressSnapshot pickupAddress() {
    return new AddressSnapshot(
        "Test Sender", "10 Example Street", null, "Sofia", "1000", "BG", "+359888000111");
  }

  private static AddressSnapshot deliveryAddress() {
    return new AddressSnapshot(
        "Test Recipient", "20 Sample Avenue", "Floor 2", "Plovdiv", "4000", "BG", "+359888000222");
  }
}
