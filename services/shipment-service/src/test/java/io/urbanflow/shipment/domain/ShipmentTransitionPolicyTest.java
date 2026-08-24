package io.urbanflow.shipment.domain;

import static io.urbanflow.shipment.domain.ShipmentActor.COURIER;
import static io.urbanflow.shipment.domain.ShipmentActor.CUSTOMER;
import static io.urbanflow.shipment.domain.ShipmentActor.OPERATIONS;
import static io.urbanflow.shipment.domain.ShipmentActor.ROUTING_SERVICE;
import static io.urbanflow.shipment.domain.ShipmentActor.SYSTEM;
import static io.urbanflow.shipment.domain.ShipmentActor.WAREHOUSE_OPERATOR;
import static io.urbanflow.shipment.domain.ShipmentStatus.AT_DESTINATION_HUB;
import static io.urbanflow.shipment.domain.ShipmentStatus.AT_ORIGIN_HUB;
import static io.urbanflow.shipment.domain.ShipmentStatus.CANCELLED;
import static io.urbanflow.shipment.domain.ShipmentStatus.CONFIRMED;
import static io.urbanflow.shipment.domain.ShipmentStatus.COURIER_ASSIGNED;
import static io.urbanflow.shipment.domain.ShipmentStatus.CREATED;
import static io.urbanflow.shipment.domain.ShipmentStatus.DELIVERED;
import static io.urbanflow.shipment.domain.ShipmentStatus.DELIVERY_ATTEMPTED;
import static io.urbanflow.shipment.domain.ShipmentStatus.IN_TRANSIT;
import static io.urbanflow.shipment.domain.ShipmentStatus.OUT_FOR_DELIVERY;
import static io.urbanflow.shipment.domain.ShipmentStatus.PICKED_UP;
import static io.urbanflow.shipment.domain.ShipmentStatus.PICKUP_IN_PROGRESS;
import static io.urbanflow.shipment.domain.ShipmentStatus.RETURNED;
import static io.urbanflow.shipment.domain.ShipmentStatus.RETURN_IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ShipmentTransitionPolicyTest {

  private static final UUID PROOF_ID = UUID.fromString("95c49380-aed4-4dd9-a4d5-cf60f5b48997");

  private final ShipmentTransitionPolicy policy = new ShipmentTransitionPolicy();

  @ParameterizedTest(name = "{0} -> {1} by {2}")
  @MethodSource("validTransitions")
  void acceptsEveryDefinedLifecycleTransition(
      ShipmentStatus current,
      ShipmentStatus target,
      ShipmentActor actor,
      String reason,
      UUID proofId) {
    ShipmentTransitionCommand command =
        new ShipmentTransitionCommand(target, actor, reason, proofId);

    policy.validate(current, command);
  }

  @ParameterizedTest
  @MethodSource("terminalStatuses")
  void rejectsTransitionsFromTerminalStatuses(ShipmentStatus terminalStatus) {
    ShipmentTransitionCommand command =
        new ShipmentTransitionCommand(CONFIRMED, OPERATIONS, null, null);

    assertThatThrownBy(() -> policy.validate(terminalStatus, command))
        .isInstanceOf(InvalidShipmentTransitionException.class)
        .extracting("code")
        .isEqualTo(TransitionRejectionCode.ILLEGAL_STATE_TRANSITION);
  }

  @Test
  void rejectsSkippedLifecycleState() {
    ShipmentTransitionCommand command =
        new ShipmentTransitionCommand(DELIVERED, OPERATIONS, null, PROOF_ID);

    assertThatThrownBy(() -> policy.validate(CREATED, command))
        .isInstanceOf(InvalidShipmentTransitionException.class)
        .extracting("code")
        .isEqualTo(TransitionRejectionCode.ILLEGAL_STATE_TRANSITION);
  }

  @Test
  void rejectsActorWithoutPermissionForOtherwiseLegalTransition() {
    ShipmentTransitionCommand command =
        new ShipmentTransitionCommand(CONFIRMED, COURIER, null, null);

    assertThatThrownBy(() -> policy.validate(CREATED, command))
        .isInstanceOf(InvalidShipmentTransitionException.class)
        .extracting("code")
        .isEqualTo(TransitionRejectionCode.ACTOR_NOT_AUTHORIZED);
  }

  @ParameterizedTest
  @MethodSource("reasonRequiredTransitions")
  void requiresReasonForExceptionalAndCancellationTransitions(
      ShipmentStatus current, ShipmentStatus target, ShipmentActor actor) {
    ShipmentTransitionCommand command = new ShipmentTransitionCommand(target, actor, "  ", null);

    assertThatThrownBy(() -> policy.validate(current, command))
        .isInstanceOf(InvalidShipmentTransitionException.class)
        .extracting("code")
        .isEqualTo(TransitionRejectionCode.REASON_REQUIRED);
  }

  @Test
  void requiresProofBeforeDelivery() {
    ShipmentTransitionCommand command =
        new ShipmentTransitionCommand(DELIVERED, COURIER, null, null);

    assertThatThrownBy(() -> policy.validate(OUT_FOR_DELIVERY, command))
        .isInstanceOf(InvalidShipmentTransitionException.class)
        .extracting("code")
        .isEqualTo(TransitionRejectionCode.DELIVERY_PROOF_REQUIRED);
  }

  @Test
  void returnsImmutableAllowedTargetSet() {
    Set<ShipmentStatus> allowed = policy.allowedTargets(CREATED);

    assertThat(allowed).containsExactlyInAnyOrder(CONFIRMED, CANCELLED);
    assertThatThrownBy(() -> allowed.add(DELIVERED))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static Stream<Arguments> validTransitions() {
    return Stream.of(
        Arguments.of(CREATED, CONFIRMED, SYSTEM, null, null),
        Arguments.of(CREATED, CANCELLED, CUSTOMER, "Changed plans", null),
        Arguments.of(CONFIRMED, COURIER_ASSIGNED, ROUTING_SERVICE, null, null),
        Arguments.of(COURIER_ASSIGNED, PICKUP_IN_PROGRESS, COURIER, null, null),
        Arguments.of(PICKUP_IN_PROGRESS, PICKED_UP, COURIER, null, null),
        Arguments.of(PICKED_UP, AT_ORIGIN_HUB, WAREHOUSE_OPERATOR, null, null),
        Arguments.of(AT_ORIGIN_HUB, IN_TRANSIT, WAREHOUSE_OPERATOR, null, null),
        Arguments.of(IN_TRANSIT, AT_DESTINATION_HUB, WAREHOUSE_OPERATOR, null, null),
        Arguments.of(AT_DESTINATION_HUB, OUT_FOR_DELIVERY, COURIER, null, null),
        Arguments.of(OUT_FOR_DELIVERY, DELIVERY_ATTEMPTED, COURIER, "Recipient unavailable", null),
        Arguments.of(DELIVERY_ATTEMPTED, OUT_FOR_DELIVERY, COURIER, null, null),
        Arguments.of(OUT_FOR_DELIVERY, DELIVERED, COURIER, null, PROOF_ID),
        Arguments.of(DELIVERY_ATTEMPTED, RETURN_IN_PROGRESS, SYSTEM, "Attempts exhausted", null),
        Arguments.of(RETURN_IN_PROGRESS, RETURNED, SYSTEM, null, null),
        Arguments.of(PICKED_UP, RETURN_IN_PROGRESS, OPERATIONS, "Damaged parcel", null));
  }

  private static Stream<ShipmentStatus> terminalStatuses() {
    return Stream.of(DELIVERED, CANCELLED, RETURNED);
  }

  private static Stream<Arguments> reasonRequiredTransitions() {
    return Stream.of(
        Arguments.of(CREATED, CANCELLED, CUSTOMER),
        Arguments.of(OUT_FOR_DELIVERY, DELIVERY_ATTEMPTED, COURIER),
        Arguments.of(PICKED_UP, RETURN_IN_PROGRESS, SYSTEM));
  }
}
