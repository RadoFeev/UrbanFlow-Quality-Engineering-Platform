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
import static io.urbanflow.shipment.domain.TransitionRejectionCode.ACTOR_NOT_AUTHORIZED;
import static io.urbanflow.shipment.domain.TransitionRejectionCode.DELIVERY_PROOF_REQUIRED;
import static io.urbanflow.shipment.domain.TransitionRejectionCode.ILLEGAL_STATE_TRANSITION;
import static io.urbanflow.shipment.domain.TransitionRejectionCode.REASON_REQUIRED;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Central authority for legal shipment lifecycle transitions and actor permissions. */
public final class ShipmentTransitionPolicy {

  private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS =
      allowedTransitions();

  private static final Map<ShipmentActor, Set<ShipmentStatus>> ACTOR_TARGETS = actorTargets();

  /** Rejects the command unless its state, actor, and evidence are valid. */
  public void validate(ShipmentStatus current, ShipmentTransitionCommand command) {
    ShipmentStatus target = command.targetStatus();
    if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
      throw rejection(
          ILLEGAL_STATE_TRANSITION,
          current,
          command,
          "Transition from %s to %s is not allowed".formatted(current, target));
    }
    if (!ACTOR_TARGETS.getOrDefault(command.actor(), Set.of()).contains(target)) {
      throw rejection(
          ACTOR_NOT_AUTHORIZED,
          current,
          command,
          "Actor %s cannot transition a shipment to %s".formatted(command.actor(), target));
    }
    if (requiresReason(target) && !command.hasReason()) {
      throw rejection(
          REASON_REQUIRED,
          current,
          command,
          "A reason is required when transitioning to %s".formatted(target));
    }
    if (target == DELIVERED && command.deliveryProofId() == null) {
      throw rejection(
          DELIVERY_PROOF_REQUIRED,
          current,
          command,
          "Delivery proof is required before a shipment can be delivered");
    }
  }

  /** Returns an immutable copy of the allowed targets for a status. */
  public Set<ShipmentStatus> allowedTargets(ShipmentStatus current) {
    return Set.copyOf(ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()));
  }

  private static boolean requiresReason(ShipmentStatus target) {
    return target == DELIVERY_ATTEMPTED || target == CANCELLED || target == RETURN_IN_PROGRESS;
  }

  private static InvalidShipmentTransitionException rejection(
      TransitionRejectionCode code,
      ShipmentStatus current,
      ShipmentTransitionCommand command,
      String message) {
    return new InvalidShipmentTransitionException(
        code, current, command.targetStatus(), command.actor(), message);
  }

  private static Map<ShipmentStatus, Set<ShipmentStatus>> allowedTransitions() {
    Map<ShipmentStatus, Set<ShipmentStatus>> transitions = new EnumMap<>(ShipmentStatus.class);
    transitions.put(CREATED, EnumSet.of(CONFIRMED, CANCELLED));
    transitions.put(CONFIRMED, EnumSet.of(COURIER_ASSIGNED, CANCELLED));
    transitions.put(COURIER_ASSIGNED, EnumSet.of(PICKUP_IN_PROGRESS, CANCELLED));
    transitions.put(PICKUP_IN_PROGRESS, EnumSet.of(PICKED_UP));
    transitions.put(PICKED_UP, EnumSet.of(AT_ORIGIN_HUB, RETURN_IN_PROGRESS));
    transitions.put(AT_ORIGIN_HUB, EnumSet.of(IN_TRANSIT, RETURN_IN_PROGRESS));
    transitions.put(IN_TRANSIT, EnumSet.of(AT_DESTINATION_HUB, RETURN_IN_PROGRESS));
    transitions.put(AT_DESTINATION_HUB, EnumSet.of(OUT_FOR_DELIVERY, RETURN_IN_PROGRESS));
    transitions.put(OUT_FOR_DELIVERY, EnumSet.of(DELIVERY_ATTEMPTED, DELIVERED));
    transitions.put(DELIVERY_ATTEMPTED, EnumSet.of(OUT_FOR_DELIVERY, RETURN_IN_PROGRESS));
    transitions.put(RETURN_IN_PROGRESS, EnumSet.of(RETURNED));
    return Map.copyOf(transitions);
  }

  private static Map<ShipmentActor, Set<ShipmentStatus>> actorTargets() {
    Map<ShipmentActor, Set<ShipmentStatus>> targets = new EnumMap<>(ShipmentActor.class);
    targets.put(CUSTOMER, EnumSet.of(CANCELLED));
    targets.put(
        COURIER,
        EnumSet.of(PICKUP_IN_PROGRESS, PICKED_UP, OUT_FOR_DELIVERY, DELIVERY_ATTEMPTED, DELIVERED));
    targets.put(WAREHOUSE_OPERATOR, EnumSet.of(AT_ORIGIN_HUB, IN_TRANSIT, AT_DESTINATION_HUB));
    targets.put(ROUTING_SERVICE, EnumSet.of(COURIER_ASSIGNED));
    targets.put(SYSTEM, EnumSet.of(CONFIRMED, IN_TRANSIT, RETURN_IN_PROGRESS, RETURNED));
    targets.put(OPERATIONS, EnumSet.allOf(ShipmentStatus.class));
    return Map.copyOf(targets);
  }
}
