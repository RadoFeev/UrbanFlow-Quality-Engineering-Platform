package io.urbanflow.integration.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.urbanflow.shipment.ShipmentServiceApplication;
import io.urbanflow.shipment.application.CreateShipmentCommand;
import io.urbanflow.shipment.application.IdempotencyConflictException;
import io.urbanflow.shipment.application.RequestTraceContext;
import io.urbanflow.shipment.application.ShipmentApplicationService;
import io.urbanflow.shipment.application.ShipmentNotFoundException;
import io.urbanflow.shipment.domain.AddressSnapshot;
import io.urbanflow.shipment.domain.DeliveryType;
import io.urbanflow.shipment.domain.InvalidShipmentTransitionException;
import io.urbanflow.shipment.domain.ParcelSnapshot;
import io.urbanflow.shipment.domain.Shipment;
import io.urbanflow.shipment.domain.ShipmentActor;
import io.urbanflow.shipment.domain.ShipmentStatus;
import io.urbanflow.shipment.domain.ShipmentTransitionCommand;
import io.urbanflow.shipment.domain.ShipmentVersionConflictException;
import io.urbanflow.shipment.persistence.IdempotencyRecordRepository;
import io.urbanflow.shipment.persistence.OutboxEventRepository;
import io.urbanflow.shipment.persistence.ShipmentRepository;
import io.urbanflow.shipment.persistence.ShipmentStatusHistoryRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    classes = ShipmentServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "urbanflow.outbox.enabled=false",
      "spring.main.web-application-type=none",
      "spring.kafka.bootstrap-servers=localhost:1"
    })
class ShipmentPersistenceIntegrationTest {

  private static final RequestTraceContext TRACE =
      new RequestTraceContext("integration-case", "0123456789abcdef0123456789abcdef");

  @Container
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18.6-alpine")
          .withDatabaseName("shipment_integration")
          .withUsername("shipment_test")
          .withPassword("local-container-password");

  @Autowired private ShipmentApplicationService service;
  @Autowired private ShipmentRepository shipments;
  @Autowired private ShipmentStatusHistoryRepository history;
  @Autowired private IdempotencyRecordRepository idempotency;
  @Autowired private OutboxEventRepository outbox;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void cleanPersistenceGraph() {
    outbox.deleteAllInBatch();
    idempotency.deleteAllInBatch();
    history.deleteAllInBatch();
    shipments.deleteAllInBatch();
  }

  @Test
  void createPersistsTheCompleteAggregate() {
    Shipment shipment = create("integration-create-01", command("Primary parcel"));
    Shipment persisted = shipments.findById(shipment.id()).orElseThrow();
    assertThat(persisted.customerId()).isEqualTo(shipment.customerId());
    assertThat(persisted.pickupAddress().city()).isEqualTo("Sofia");
    assertThat(persisted.deliveryAddress().city()).isEqualTo("Varna");
    assertThat(persisted.parcel().description()).isEqualTo("Primary parcel");
  }

  @Test
  void createPersistsInitialStatusHistory() {
    Shipment shipment = create("integration-history-01", command("History parcel"));
    assertThat(history.findAll())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.shipmentId()).isEqualTo(shipment.id());
              assertThat(item.previousStatus()).isNull();
              assertThat(item.newStatus()).isEqualTo(ShipmentStatus.CREATED);
              assertThat(item.actor()).isEqualTo(ShipmentActor.SYSTEM);
            });
  }

  @Test
  void createStagesAnUnpublishedVersionedOutboxEvent() {
    Shipment shipment = create("integration-outbox-01", command("Outbox parcel"));
    assertThat(outbox.findAll())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.aggregateId()).isEqualTo(shipment.id());
              assertThat(event.aggregateVersion()).isZero();
              assertThat(event.eventType()).isEqualTo("ShipmentCreated");
              assertThat(event.publishedAt()).isNull();
            });
  }

  @Test
  void outboxCarriesCorrelationTraceAndPayloadIdentity() {
    Shipment shipment = create("integration-trace-01", command("Trace parcel"));
    assertThat(outbox.findAll())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.correlationId()).isEqualTo(TRACE.correlationId());
              assertThat(event.traceId()).isEqualTo(TRACE.traceId());
              assertThat(event.payload())
                  .contains(shipment.id().toString(), shipment.trackingNumber(), "CREATED");
            });
  }

  @Test
  void identicalIdempotentRetryReturnsTheOriginalAggregate() {
    CreateShipmentCommand command = command("Idempotent parcel");
    Shipment first = create("integration-idem-01", command);
    Shipment replay = create("integration-idem-01", command);
    assertThat(replay.id()).isEqualTo(first.id());
    assertThat(shipments.count()).isEqualTo(1L);
    assertThat(outbox.count()).isEqualTo(1L);
  }

  @Test
  void changedRequestWithTheSameIdempotencyKeyIsRejected() {
    create("integration-idem-02", command("Original parcel"));
    assertThatThrownBy(() -> create("integration-idem-02", command("Changed parcel")))
        .isInstanceOf(IdempotencyConflictException.class);
    assertThat(shipments.count()).isEqualTo(1L);
  }

  @Test
  void idempotencyRecordPointsToThePersistedShipment() {
    Shipment created = create("integration-idem-link", command("Linked parcel"));
    assertThat(idempotency.findAll())
        .singleElement()
        .satisfies(
            record -> {
              assertThat(record.key()).isEqualTo("integration-idem-link");
              assertThat(record.shipmentId()).isEqualTo(created.id());
              assertThat(record.requestFingerprint()).isNotBlank();
            });
  }

  @Test
  void rejectedIdempotencyConflictDoesNotStageAnotherEvent() {
    create("integration-idem-rollback", command("Original parcel"));
    assertThatThrownBy(() -> create("integration-idem-rollback", command("Conflicting parcel")))
        .isInstanceOf(IdempotencyConflictException.class);
    assertThat(idempotency.count()).isEqualTo(1L);
    assertThat(outbox.count()).isEqualTo(1L);
  }

  @Test
  void persistedShipmentCanBeReadByUuid() {
    Shipment created = create("integration-read-id", command("Read parcel"));
    assertThat(service.findById(created.id()).trackingNumber()).isEqualTo(created.trackingNumber());
  }

  @Test
  void persistedShipmentCanBeReadByTrackingNumber() {
    Shipment created = create("integration-read-tracking", command("Track parcel"));
    assertThat(service.findByTrackingNumber(created.trackingNumber()).id()).isEqualTo(created.id());
  }

  @Test
  void missingUuidProducesAStableDomainFailure() {
    assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
        .isInstanceOf(ShipmentNotFoundException.class);
  }

  @Test
  void missingTrackingNumberProducesAStableDomainFailure() {
    assertThatThrownBy(() -> service.findByTrackingNumber("UF000000000000000000"))
        .isInstanceOf(ShipmentNotFoundException.class);
  }

  @Test
  void allowedDeliveryAddressChangeIsPersisted() {
    Shipment created = create("integration-address-01", command("Address parcel"));
    AddressSnapshot replacement = address("Replacement", "Burgas", "+359880000099");
    Shipment updated =
        service.changeDeliveryAddress(created.id(), created.version(), replacement, TRACE);
    assertThat(shipments.findById(created.id()).orElseThrow().deliveryAddress().city())
        .isEqualTo("Burgas");
    assertThat(updated.version()).isEqualTo(1L);
  }

  @Test
  void addressChangeStagesItsOwnOutboxEvent() {
    Shipment created = create("integration-address-02", command("Address event parcel"));
    service.changeDeliveryAddress(
        created.id(), created.version(), address("Replacement", "Ruse", "+359880000088"), TRACE);
    assertThat(outbox.findAll())
        .extracting(event -> event.eventType())
        .containsExactlyInAnyOrder("ShipmentCreated", "ShipmentDeliveryAddressChanged");
  }

  @Test
  void addressChangeEventCarriesTheNewDestination() {
    Shipment created = create("integration-address-payload", command("Address payload parcel"));
    service.changeDeliveryAddress(
        created.id(), created.version(), address("Replacement", "Dobrich", "+359880000055"), TRACE);
    assertThat(outbox.findAll())
        .filteredOn(event -> event.eventType().equals("ShipmentDeliveryAddressChanged"))
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.aggregateVersion()).isEqualTo(1L);
              assertThat(event.payload()).contains("Dobrich", "Replacement");
            });
  }

  @Test
  void staleAddressVersionDoesNotMutatePersistence() {
    Shipment created = create("integration-address-03", command("Stale address parcel"));
    service.changeDeliveryAddress(
        created.id(), 0L, address("Current", "Plovdiv", "+359880000077"), TRACE);
    assertThatThrownBy(
            () ->
                service.changeDeliveryAddress(
                    created.id(), 0L, address("Stale", "Ruse", "+359880000066"), TRACE))
        .isInstanceOf(ShipmentVersionConflictException.class);
    assertThat(shipments.findById(created.id()).orElseThrow().deliveryAddress().city())
        .isEqualTo("Plovdiv");
  }

  @ParameterizedTest(name = "persists legal lifecycle target {0}")
  @MethodSource("normalLifecycleTargets")
  void legalLifecycleTransitionsPersistHistoryAndOutbox(ShipmentStatus target) {
    Shipment shipment =
        create(
            "lifecycle-" + target.name().toLowerCase(java.util.Locale.ROOT),
            command("Lifecycle parcel"));
    shipment = advance(shipment, target);
    assertThat(shipments.findById(shipment.id()).orElseThrow().status()).isEqualTo(target);
    assertThat(history.count()).isEqualTo((long) normalPath().indexOf(target) + 2L);
    assertThat(outbox.count()).isEqualTo(history.count());
  }

  @Test
  void successiveTransitionsIncrementTheDurableAggregateVersion() {
    Shipment shipment = create("integration-version-chain", command("Version chain parcel"));
    assertThat(shipment.version()).isZero();
    shipment =
        service.transition(
            shipment.id(), shipment.version(), transition(ShipmentStatus.CONFIRMED), TRACE);
    assertThat(shipment.version()).isEqualTo(1L);
    shipment =
        service.transition(
            shipment.id(), shipment.version(), transition(ShipmentStatus.COURIER_ASSIGNED), TRACE);
    assertThat(shipment.version()).isEqualTo(2L);
    assertThat(shipments.findById(shipment.id()).orElseThrow().version()).isEqualTo(2L);
  }

  @ParameterizedTest(name = "maps {0} to durable event {1}")
  @MethodSource("representativeEventMappings")
  void representativeTransitionsStageTheirPublishedEventType(
      ShipmentStatus target, String expectedEventType) {
    Shipment shipment =
        create(
            "event-map-" + target.name().toLowerCase(java.util.Locale.ROOT),
            command("Event mapping parcel"));
    shipment = advance(shipment, target);
    long aggregateVersion = shipment.version();
    assertThat(outbox.findAll())
        .filteredOn(event -> event.aggregateVersion() == aggregateVersion)
        .singleElement()
        .extracting(event -> event.eventType())
        .isEqualTo(expectedEventType);
  }

  @Test
  void illegalTransitionRollsBackHistoryAndOutbox() {
    Shipment created = create("integration-illegal", command("Illegal parcel"));
    long historyBefore = history.count();
    long outboxBefore = outbox.count();
    assertThatThrownBy(
            () ->
                service.transition(
                    created.id(), created.version(), transition(ShipmentStatus.DELIVERED), TRACE))
        .isInstanceOf(InvalidShipmentTransitionException.class);
    assertThat(history.count()).isEqualTo(historyBefore);
    assertThat(outbox.count()).isEqualTo(outboxBefore);
    assertThat(shipments.findById(created.id()).orElseThrow().status())
        .isEqualTo(ShipmentStatus.CREATED);
  }

  @Test
  void unauthorizedActorCannotAdvanceTheShipment() {
    Shipment created = create("integration-actor", command("Actor parcel"));
    assertThatThrownBy(
            () ->
                service.transition(
                    created.id(),
                    created.version(),
                    new ShipmentTransitionCommand(
                        ShipmentStatus.CONFIRMED, ShipmentActor.CUSTOMER, null, null),
                    TRACE))
        .isInstanceOf(InvalidShipmentTransitionException.class);
  }

  @Test
  void cancellationRequiresAReason() {
    Shipment created = create("integration-cancel-reason", command("Cancel parcel"));
    assertThatThrownBy(
            () ->
                service.transition(
                    created.id(),
                    created.version(),
                    new ShipmentTransitionCommand(
                        ShipmentStatus.CANCELLED, ShipmentActor.CUSTOMER, null, null),
                    TRACE))
        .isInstanceOf(InvalidShipmentTransitionException.class);
  }

  @Test
  void cancellationReasonIsPersistedInHistory() {
    Shipment created = create("integration-cancel", command("Cancel parcel"));
    service.transition(
        created.id(),
        created.version(),
        new ShipmentTransitionCommand(
            ShipmentStatus.CANCELLED, ShipmentActor.CUSTOMER, "Customer request", null),
        TRACE);
    assertThat(history.findAll())
        .anySatisfy(
            item -> {
              assertThat(item.newStatus()).isEqualTo(ShipmentStatus.CANCELLED);
              assertThat(item.reason()).isEqualTo("Customer request");
            });
  }

  @Test
  void deliveredTransitionRequiresProof() {
    Shipment shipment =
        advance(
            create("integration-proof-01", command("Proof parcel")),
            ShipmentStatus.OUT_FOR_DELIVERY);
    Shipment beforeDelivery = shipment;
    assertThatThrownBy(
            () ->
                service.transition(
                    beforeDelivery.id(),
                    beforeDelivery.version(),
                    new ShipmentTransitionCommand(
                        ShipmentStatus.DELIVERED, ShipmentActor.COURIER, null, null),
                    TRACE))
        .isInstanceOf(InvalidShipmentTransitionException.class);
  }

  @Test
  void deliveryProofIsPersistedWithDeliveredHistory() {
    Shipment shipment =
        advance(
            create("integration-proof-02", command("Proof parcel")),
            ShipmentStatus.OUT_FOR_DELIVERY);
    UUID proofId = UUID.randomUUID();
    service.transition(
        shipment.id(),
        shipment.version(),
        new ShipmentTransitionCommand(
            ShipmentStatus.DELIVERED, ShipmentActor.COURIER, null, proofId),
        TRACE);
    assertThat(history.findAll())
        .anySatisfy(
            item -> {
              assertThat(item.newStatus()).isEqualTo(ShipmentStatus.DELIVERED);
              assertThat(item.deliveryProofId()).isEqualTo(proofId);
            });
  }

  private Shipment create(String idempotencyKey, CreateShipmentCommand command) {
    return service.create(idempotencyKey, command, TRACE);
  }

  private Shipment advance(Shipment shipment, ShipmentStatus target) {
    for (ShipmentStatus step : normalPath()) {
      shipment = service.transition(shipment.id(), shipment.version(), transition(step), TRACE);
      if (step == target) {
        return shipment;
      }
    }
    throw new IllegalArgumentException("target is outside the normal lifecycle path: " + target);
  }

  private static ShipmentTransitionCommand transition(ShipmentStatus target) {
    ShipmentActor actor =
        switch (target) {
          case CONFIRMED -> ShipmentActor.SYSTEM;
          case COURIER_ASSIGNED -> ShipmentActor.ROUTING_SERVICE;
          case PICKUP_IN_PROGRESS, PICKED_UP, OUT_FOR_DELIVERY -> ShipmentActor.COURIER;
          case AT_ORIGIN_HUB, IN_TRANSIT, AT_DESTINATION_HUB -> ShipmentActor.WAREHOUSE_OPERATOR;
          default -> ShipmentActor.OPERATIONS;
        };
    return new ShipmentTransitionCommand(target, actor, null, null);
  }

  private static List<ShipmentStatus> normalPath() {
    return List.of(
        ShipmentStatus.CONFIRMED,
        ShipmentStatus.COURIER_ASSIGNED,
        ShipmentStatus.PICKUP_IN_PROGRESS,
        ShipmentStatus.PICKED_UP,
        ShipmentStatus.AT_ORIGIN_HUB,
        ShipmentStatus.IN_TRANSIT,
        ShipmentStatus.AT_DESTINATION_HUB,
        ShipmentStatus.OUT_FOR_DELIVERY);
  }

  private static Stream<Arguments> normalLifecycleTargets() {
    return normalPath().stream().map(Arguments::of);
  }

  private static Stream<Arguments> representativeEventMappings() {
    return Stream.of(
        Arguments.of(ShipmentStatus.CONFIRMED, "ShipmentConfirmed"),
        Arguments.of(ShipmentStatus.COURIER_ASSIGNED, "CourierAssigned"),
        Arguments.of(ShipmentStatus.PICKED_UP, "ShipmentPickedUp"),
        Arguments.of(ShipmentStatus.AT_ORIGIN_HUB, "ShipmentArrivedAtHub"),
        Arguments.of(ShipmentStatus.OUT_FOR_DELIVERY, "ShipmentDispatched"));
  }

  private static CreateShipmentCommand command(String description) {
    return new CreateShipmentCommand(
        UUID.randomUUID(),
        address("Sender", "Sofia", "+359880000001"),
        address("Receiver", "Varna", "+359880000002"),
        new ParcelSnapshot(2500, 30, 20, 15, description),
        DeliveryType.STANDARD);
  }

  private static AddressSnapshot address(String contact, String city, String phone) {
    return new AddressSnapshot(contact, "100 Synthetic Route", null, city, "1000", "BG", phone);
  }
}
