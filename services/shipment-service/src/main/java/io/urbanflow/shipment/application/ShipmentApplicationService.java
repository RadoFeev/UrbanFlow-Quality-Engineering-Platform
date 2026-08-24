package io.urbanflow.shipment.application;

import io.urbanflow.shipment.domain.AddressSnapshot;
import io.urbanflow.shipment.domain.Shipment;
import io.urbanflow.shipment.domain.ShipmentStatus;
import io.urbanflow.shipment.domain.ShipmentTransitionCommand;
import io.urbanflow.shipment.domain.ShipmentTransitionPolicy;
import io.urbanflow.shipment.persistence.IdempotencyRecord;
import io.urbanflow.shipment.persistence.IdempotencyRecordRepository;
import io.urbanflow.shipment.persistence.OutboxEvent;
import io.urbanflow.shipment.persistence.OutboxEventRepository;
import io.urbanflow.shipment.persistence.ShipmentRepository;
import io.urbanflow.shipment.persistence.ShipmentStatusHistory;
import io.urbanflow.shipment.persistence.ShipmentStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates versioned shipment persistence, history, idempotency, and outbox writes. */
@Service
public class ShipmentApplicationService {

  private final ShipmentRepository shipmentRepository;
  private final IdempotencyRecordRepository idempotencyRepository;
  private final ShipmentStatusHistoryRepository historyRepository;
  private final OutboxEventRepository outboxRepository;
  private final ShipmentTransitionPolicy transitionPolicy;
  private final TrackingNumberGenerator trackingNumberGenerator;
  private final Clock clock;

  /** Creates the application service with explicit persistence and domain collaborators. */
  public ShipmentApplicationService(
      ShipmentRepository shipmentRepository,
      IdempotencyRecordRepository idempotencyRepository,
      ShipmentStatusHistoryRepository historyRepository,
      OutboxEventRepository outboxRepository,
      ShipmentTransitionPolicy transitionPolicy,
      TrackingNumberGenerator trackingNumberGenerator,
      Clock clock) {
    this.shipmentRepository = shipmentRepository;
    this.idempotencyRepository = idempotencyRepository;
    this.historyRepository = historyRepository;
    this.outboxRepository = outboxRepository;
    this.transitionPolicy = transitionPolicy;
    this.trackingNumberGenerator = trackingNumberGenerator;
    this.clock = clock;
  }

  /** Creates a shipment once for a key and returns the original result on an identical retry. */
  @Transactional
  public Shipment create(
      String idempotencyKey, CreateShipmentCommand command, RequestTraceContext traceContext) {
    String fingerprint = command.fingerprint();
    return idempotencyRepository
        .findById(idempotencyKey)
        .map(record -> resolveExisting(record, fingerprint))
        .orElseGet(() -> persistNew(idempotencyKey, fingerprint, command, traceContext));
  }

  /** Returns a shipment by UUID. */
  @Transactional(readOnly = true)
  public Shipment findById(UUID shipmentId) {
    return shipmentRepository
        .findById(shipmentId)
        .orElseThrow(() -> new ShipmentNotFoundException(shipmentId.toString()));
  }

  /** Returns a shipment by tracking number. */
  @Transactional(readOnly = true)
  public Shipment findByTrackingNumber(String trackingNumber) {
    return shipmentRepository
        .findByTrackingNumber(trackingNumber)
        .orElseThrow(() -> new ShipmentNotFoundException(trackingNumber));
  }

  /** Applies one lifecycle transition and persists matching history and outbox records. */
  @Transactional
  public Shipment transition(
      UUID shipmentId,
      long expectedVersion,
      ShipmentTransitionCommand command,
      RequestTraceContext traceContext) {
    Shipment shipment = findById(shipmentId);
    ShipmentStatus previous = shipment.status();
    Instant now = clock.instant();
    shipment.transition(expectedVersion, command, transitionPolicy, now);
    Shipment persisted = shipmentRepository.saveAndFlush(shipment);
    historyRepository.save(
        ShipmentStatusHistory.transition(
            shipment.id(),
            previous,
            shipment.status(),
            command.actor(),
            command.reason(),
            command.deliveryProofId(),
            now));
    outboxRepository.save(
        OutboxEvent.shipmentEvent(
            persisted,
            eventType(persisted.status()),
            traceContext.correlationId(),
            traceContext.traceId(),
            now));
    return persisted;
  }

  /** Changes a delivery address and emits an auditable shipment event. */
  @Transactional
  public Shipment changeDeliveryAddress(
      UUID shipmentId,
      long expectedVersion,
      AddressSnapshot newAddress,
      RequestTraceContext traceContext) {
    Shipment shipment = findById(shipmentId);
    Instant now = clock.instant();
    shipment.changeDeliveryAddress(expectedVersion, newAddress, now);
    Shipment persisted = shipmentRepository.saveAndFlush(shipment);
    outboxRepository.save(
        OutboxEvent.shipmentEvent(
            persisted,
            "ShipmentDeliveryAddressChanged",
            traceContext.correlationId(),
            traceContext.traceId(),
            now));
    return persisted;
  }

  private Shipment persistNew(
      String idempotencyKey,
      String fingerprint,
      CreateShipmentCommand command,
      RequestTraceContext traceContext) {
    Instant now = clock.instant();
    Shipment shipment =
        Shipment.create(
            UUID.randomUUID(),
            command.customerId(),
            trackingNumberGenerator.next(),
            command.deliveryType(),
            command.pickupAddress(),
            command.deliveryAddress(),
            command.parcel(),
            now);
    Shipment persisted = shipmentRepository.saveAndFlush(shipment);
    idempotencyRepository.save(
        new IdempotencyRecord(idempotencyKey, fingerprint, shipment.id(), now));
    historyRepository.save(ShipmentStatusHistory.created(shipment.id(), now));
    outboxRepository.save(
        OutboxEvent.shipmentEvent(
            persisted,
            "ShipmentCreated",
            traceContext.correlationId(),
            traceContext.traceId(),
            now));
    return persisted;
  }

  private Shipment resolveExisting(IdempotencyRecord record, String fingerprint) {
    if (!record.requestFingerprint().equals(fingerprint)) {
      throw new IdempotencyConflictException(record.key());
    }
    return shipmentRepository
        .findById(record.shipmentId())
        .orElseThrow(() -> new ShipmentNotFoundException(record.shipmentId().toString()));
  }

  private static String eventType(ShipmentStatus status) {
    return switch (status) {
      case CONFIRMED -> "ShipmentConfirmed";
      case COURIER_ASSIGNED -> "CourierAssigned";
      case PICKED_UP -> "ShipmentPickedUp";
      case AT_ORIGIN_HUB, AT_DESTINATION_HUB -> "ShipmentArrivedAtHub";
      case OUT_FOR_DELIVERY -> "ShipmentDispatched";
      case DELIVERY_ATTEMPTED -> "DeliveryAttempted";
      case DELIVERED -> "ShipmentDelivered";
      case CANCELLED -> "ShipmentCancelled";
      case RETURN_IN_PROGRESS -> "ShipmentReturnStarted";
      default -> "ShipmentStatusChanged";
    };
  }
}
