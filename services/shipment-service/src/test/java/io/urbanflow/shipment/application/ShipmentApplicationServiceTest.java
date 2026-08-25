package io.urbanflow.shipment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.urbanflow.shipment.domain.AddressSnapshot;
import io.urbanflow.shipment.domain.DeliveryType;
import io.urbanflow.shipment.domain.ParcelSnapshot;
import io.urbanflow.shipment.domain.Shipment;
import io.urbanflow.shipment.domain.ShipmentActor;
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
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ShipmentApplicationServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-24T09:00:00Z");
  private static final String IDEMPOTENCY_KEY = "shipment-test-run-001";
  private static final UUID EXISTING_ID = UUID.fromString("2bc0e8f4-f5ba-4306-a64f-d4f5b6ef36a9");
  private static final RequestTraceContext TRACE_CONTEXT =
      new RequestTraceContext("corr-test-001", "0123456789abcdef0123456789abcdef");

  @Mock private ShipmentRepository shipmentRepository;
  @Mock private IdempotencyRecordRepository idempotencyRepository;
  @Mock private ShipmentStatusHistoryRepository historyRepository;
  @Mock private OutboxEventRepository outboxRepository;
  @Mock private TrackingNumberGenerator trackingNumberGenerator;
  @Mock private ObjectMapper objectMapper;

  private ShipmentApplicationService service;

  @BeforeEach
  void setUp() {
    service =
        new ShipmentApplicationService(
            shipmentRepository,
            idempotencyRepository,
            historyRepository,
            outboxRepository,
            new ShipmentTransitionPolicy(),
            trackingNumberGenerator,
            Clock.fixed(NOW, ZoneOffset.UTC),
            objectMapper);
  }

  @Test
  void createsShipmentHistoryIdempotencyRecordAndOutboxEventAtomically() {
    CreateShipmentCommand command = command("Synthetic parcel");
    when(idempotencyRepository.findById(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(trackingNumberGenerator.next()).thenReturn("UF0123456789ABCDEF01");
    when(shipmentRepository.saveAndFlush(any(Shipment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Shipment shipment = service.create(IDEMPOTENCY_KEY, command, TRACE_CONTEXT);

    assertThat(shipment.status()).isEqualTo(ShipmentStatus.CREATED);
    assertThat(shipment.createdAt()).isEqualTo(NOW);
    ArgumentCaptor<IdempotencyRecord> idempotency =
        ArgumentCaptor.forClass(IdempotencyRecord.class);
    verify(idempotencyRepository).save(idempotency.capture());
    assertThat(idempotency.getValue().requestFingerprint()).isEqualTo(command.fingerprint());
    assertThat(idempotency.getValue().shipmentId()).isEqualTo(shipment.id());

    ArgumentCaptor<ShipmentStatusHistory> history =
        ArgumentCaptor.forClass(ShipmentStatusHistory.class);
    verify(historyRepository).save(history.capture());
    assertThat(history.getValue().newStatus()).isEqualTo(ShipmentStatus.CREATED);
    assertThat(history.getValue().actor()).isEqualTo(ShipmentActor.SYSTEM);

    ArgumentCaptor<OutboxEvent> outbox = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxRepository).save(outbox.capture());
    assertThat(outbox.getValue().eventType()).isEqualTo("ShipmentCreated");
    assertThat(outbox.getValue().aggregateId()).isEqualTo(shipment.id());
    assertThat(outbox.getValue().payload()).contains(shipment.id().toString(), "CREATED");
    assertThat(outbox.getValue().correlationId()).isEqualTo(TRACE_CONTEXT.correlationId());
    assertThat(outbox.getValue().traceId()).isEqualTo(TRACE_CONTEXT.traceId());
  }

  @Test
  void returnsOriginalShipmentForIdenticalIdempotentRetryWithoutNewWrites() {
    CreateShipmentCommand command = command("Synthetic parcel");
    Shipment existing = existingShipment();
    IdempotencyRecord record =
        new IdempotencyRecord(
            IDEMPOTENCY_KEY, command.fingerprint(), existing.id(), NOW.minusSeconds(30));
    when(idempotencyRepository.findById(IDEMPOTENCY_KEY)).thenReturn(Optional.of(record));
    when(shipmentRepository.findById(existing.id())).thenReturn(Optional.of(existing));

    Shipment result = service.create(IDEMPOTENCY_KEY, command, TRACE_CONTEXT);

    assertThat(result).isSameAs(existing);
    verify(shipmentRepository, never()).saveAndFlush(any());
    verifyNoInteractions(historyRepository, outboxRepository, trackingNumberGenerator);
  }

  @Test
  void rejectsIdempotencyKeyReusedWithDifferentBusinessInput() {
    CreateShipmentCommand original = command("Synthetic parcel");
    CreateShipmentCommand changed = command("Changed parcel");
    IdempotencyRecord record =
        new IdempotencyRecord(
            IDEMPOTENCY_KEY, original.fingerprint(), EXISTING_ID, NOW.minusSeconds(30));
    when(idempotencyRepository.findById(IDEMPOTENCY_KEY)).thenReturn(Optional.of(record));

    assertThatThrownBy(() -> service.create(IDEMPOTENCY_KEY, changed, TRACE_CONTEXT))
        .isInstanceOf(IdempotencyConflictException.class)
        .hasMessage("Idempotency key was already used with a different request");
    verifyNoInteractions(shipmentRepository, historyRepository, outboxRepository);
  }

  @Test
  void persistsMatchingHistoryAndOutboxForAcceptedTransition() {
    Shipment shipment = existingShipment();
    when(shipmentRepository.findById(shipment.id())).thenReturn(Optional.of(shipment));
    when(shipmentRepository.saveAndFlush(shipment)).thenReturn(shipment);
    ShipmentTransitionCommand command =
        new ShipmentTransitionCommand(ShipmentStatus.CONFIRMED, ShipmentActor.SYSTEM, null, null);

    Shipment result = service.transition(shipment.id(), 0, command, TRACE_CONTEXT);

    assertThat(result.status()).isEqualTo(ShipmentStatus.CONFIRMED);
    ArgumentCaptor<ShipmentStatusHistory> history =
        ArgumentCaptor.forClass(ShipmentStatusHistory.class);
    verify(historyRepository).save(history.capture());
    assertThat(history.getValue().previousStatus()).isEqualTo(ShipmentStatus.CREATED);
    assertThat(history.getValue().newStatus()).isEqualTo(ShipmentStatus.CONFIRMED);

    ArgumentCaptor<OutboxEvent> outbox = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxRepository).save(outbox.capture());
    assertThat(outbox.getValue().eventType()).isEqualTo("ShipmentConfirmed");
    assertThat(outbox.getValue().payload()).contains("CONFIRMED");
  }

  private static Shipment existingShipment() {
    CreateShipmentCommand command = command("Synthetic parcel");
    return Shipment.create(
        EXISTING_ID,
        command.customerId(),
        "UF0123456789ABCDEF01",
        command.deliveryType(),
        command.pickupAddress(),
        command.deliveryAddress(),
        command.parcel(),
        NOW.minusSeconds(60));
  }

  private static CreateShipmentCommand command(String description) {
    return new CreateShipmentCommand(
        UUID.fromString("08b8e8d9-694f-4576-bc56-f4870afe9795"),
        new AddressSnapshot(
            "Test Sender", "10 Example Street", null, "Sofia", "1000", "BG", "+359888000111"),
        new AddressSnapshot(
            "Test Recipient", "20 Sample Avenue", null, "Plovdiv", "4000", "BG", "+359888000222"),
        new ParcelSnapshot(1000, 20, 20, 20, description),
        DeliveryType.STANDARD);
  }
}
