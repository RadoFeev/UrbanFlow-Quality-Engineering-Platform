package io.urbanflow.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("database")
@Testcontainers(disabledWithoutDocker = true)
class PostgresPersistenceTest {

  private static final Path WORKSPACE = Path.of(System.getProperty("workspace.root"));
  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");
  private static final List<String> MIGRATIONS =
      List.of(
          "services/identity-service/src/main/resources/db/migration/"
              + "V1__create_identity_schema.sql",
          "services/identity-service/src/main/resources/db/migration/"
              + "V2__normalize_token_hash_column_types.sql",
          "services/shipment-service/src/main/resources/db/migration/"
              + "V1__create_shipment_schema.sql",
          "services/shipment-service/src/main/resources/db/migration/"
              + "V2__add_outbox_publication_leases.sql",
          "services/shipment-service/src/main/resources/db/migration/"
              + "V3__normalize_shipment_constraint_names.sql",
          "services/tracking-service/src/main/resources/db/migration/"
              + "V1__create_tracking_schema.sql",
          "services/routing-service/src/main/resources/db/migration/"
              + "V1__create_routing_schema.sql",
          "services/routing-service/src/main/resources/db/migration/"
              + "V2__normalize_trace_id_column_type.sql",
          "services/notification-service/src/main/resources/db/migration/"
              + "V1__create_notification_schema.sql",
          "services/warehouse-service/src/main/resources/db/migration/"
              + "V1__create_warehouse_schema.sql");

  @Container
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18.6-alpine")
          .withDatabaseName("urbanflow_test")
          .withUsername("urbanflow_test")
          .withPassword("local-container-password");

  @BeforeAll
  static void applyProductionMigrations() throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      for (String migration : MIGRATIONS) {
        statement.execute(read(migration));
      }
    }
  }

  @BeforeEach
  void cleanDatabase() throws SQLException {
    execute(
        "TRUNCATE audit_records, feature_flags, incidents, logistics_hubs, warehouses, "
            + "notifications, routing_outbox_events, processed_shipment_events, delivery_routes, "
            + "couriers, tracking_events, outbox_events, idempotency_records, "
            + "shipment_status_history, shipments, password_reset_tokens, refresh_sessions, "
            + "user_accounts CASCADE");
  }

  @Test
  void allProductionMigrationsCreateTheirOwnedTables() throws SQLException {
    assertThat(tableNames())
        .contains(
            "user_accounts",
            "refresh_sessions",
            "password_reset_tokens",
            "shipments",
            "shipment_status_history",
            "idempotency_records",
            "outbox_events",
            "tracking_events",
            "couriers",
            "delivery_routes",
            "processed_shipment_events",
            "routing_outbox_events",
            "notifications",
            "warehouses",
            "logistics_hubs",
            "incidents",
            "feature_flags",
            "audit_records");
  }

  @Test
  void persistsCompleteShipment() throws SQLException {
    UUID shipmentId = UUID.randomUUID();
    insertShipment(shipmentId, "UF0123456789ABCDEF01", 2500);
    assertThat(scalar("SELECT status FROM shipments WHERE id = ?", shipmentId))
        .isEqualTo("CREATED");
  }

  @ParameterizedTest(name = "accepts shipment weight boundary {0}")
  @MethodSource("validWeights")
  void acceptsShipmentWeightBoundaries(int weight) throws SQLException {
    insertShipment(UUID.randomUUID(), tracking(weight), weight);
    assertThat(scalar("SELECT weight_grams FROM shipments", new Object[0])).isEqualTo(weight);
  }

  @ParameterizedTest(name = "rejects shipment weight partition {0}")
  @MethodSource("invalidWeights")
  void rejectsShipmentWeightsOutsideTheDomain(int weight) {
    assertThatThrownBy(() -> insertShipment(UUID.randomUUID(), tracking(weight), weight))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ck_shipments");
  }

  @Test
  void rejectsTrackingNumberOutsideTheCanonicalFormat() {
    assertThatThrownBy(() -> insertShipment(UUID.randomUUID(), "not-a-tracking-number", 1000))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ck_shipments_tracking_number");
  }

  @Test
  void preventsDuplicateTrackingNumbers() throws SQLException {
    insertShipment(UUID.randomUUID(), "UF111111111111111111", 1000);
    assertThatThrownBy(() -> insertShipment(UUID.randomUUID(), "UF111111111111111111", 1000))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("tracking_number");
  }

  @Test
  void statusHistoryRequiresAnExistingShipment() {
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO shipment_status_history "
                        + "(id, shipment_id, new_status, actor, occurred_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "CREATED",
                    "SYSTEM",
                    NOW))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("shipment_status_history_shipment_id_fkey");
  }

  @Test
  void transactionRollbackLeavesNoPartialWarehouse() throws SQLException {
    UUID id = UUID.randomUUID();
    try (Connection connection = connection()) {
      connection.setAutoCommit(false);
      execute(
          connection,
          "INSERT INTO warehouses "
              + "(id, code, name, city, capacity, created_at, updated_at) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?)",
          id,
          "SOF-ROLLBACK",
          "Rollback Warehouse",
          "Sofia",
          100,
          NOW,
          NOW);
      connection.rollback();
    }
    assertThat(scalar("SELECT COUNT(*) FROM warehouses WHERE id = ?", id)).isEqualTo(0L);
  }

  @Test
  void staleVersionUpdateAffectsNoRows() throws SQLException {
    UUID id = insertWarehouse("SOF-VERSION");
    assertThat(
            update(
                "UPDATE warehouses SET name = ?, version = version + 1 "
                    + "WHERE id = ? AND version = ?",
                "Current name",
                id,
                0L))
        .isEqualTo(1);
    assertThat(
            update(
                "UPDATE warehouses SET name = ?, version = version + 1 "
                    + "WHERE id = ? AND version = ?",
                "Stale name",
                id,
                0L))
        .isZero();
    assertThat(scalar("SELECT name FROM warehouses WHERE id = ?", id)).isEqualTo("Current name");
  }

  @Test
  void softDeletedShipmentRemainsAuditable() throws SQLException {
    UUID id = UUID.randomUUID();
    insertShipment(id, "UF222222222222222222", 1000);
    execute("UPDATE shipments SET deleted_at = ? WHERE id = ?", NOW, id);
    assertThat(scalar("SELECT COUNT(*) FROM shipments WHERE id = ?", id)).isEqualTo(1L);
    assertThat(scalar("SELECT COUNT(*) FROM shipments WHERE id = ? AND deleted_at IS NULL", id))
        .isEqualTo(0L);
  }

  @Test
  void outboxPreventsDuplicateAggregateVersionAndType() throws SQLException {
    UUID shipmentId = UUID.randomUUID();
    insertShipment(shipmentId, "UF333333333333333333", 1000);
    insertOutbox(shipmentId, 0L, "ShipmentCreated");
    assertThatThrownBy(() -> insertOutbox(shipmentId, 0L, "ShipmentCreated"))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("uq_outbox_aggregate_version_type");
  }

  @Test
  void jsonbPayloadRoundTripsAsStructuredData() throws SQLException {
    UUID shipmentId = UUID.randomUUID();
    insertShipment(shipmentId, "UF444444444444444444", 1000);
    insertOutbox(shipmentId, 0L, "ShipmentCreated");
    assertThat(scalar("SELECT payload ->> 'status' FROM outbox_events", new Object[0]))
        .isEqualTo("CREATED");
  }

  @Test
  void outboxLeaseMustBeComplete() throws SQLException {
    UUID shipmentId = UUID.randomUUID();
    insertShipment(shipmentId, "UF555555555555555555", 1000);
    insertOutbox(shipmentId, 0L, "ShipmentCreated");
    assertThatThrownBy(() -> execute("UPDATE outbox_events SET claimed_by = ?", "worker-only"))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ck_outbox_claim_complete");
  }

  @Test
  void identityEmailMustBeCanonicalLowercase() {
    assertThatThrownBy(() -> insertUser(UUID.randomUUID(), "Upper@Example.Test", "CUSTOMER"))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ck_user_accounts_email_lowercase");
  }

  @Test
  void identityEmailIsUnique() throws SQLException {
    insertUser(UUID.randomUUID(), "unique@example.test", "CUSTOMER");
    assertThatThrownBy(() -> insertUser(UUID.randomUUID(), "unique@example.test", "CUSTOMER"))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("uq_user_accounts_email");
  }

  @Test
  void identityRejectsUnsupportedRoles() {
    assertThatThrownBy(() -> insertUser(UUID.randomUUID(), "role@example.test", "SUPERUSER"))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ck_user_accounts_role");
  }

  @Test
  void deletingUserCascadesRefreshSessions() throws SQLException {
    UUID userId = UUID.randomUUID();
    insertUser(userId, "cascade@example.test", "CUSTOMER");
    execute(
        "INSERT INTO refresh_sessions "
            + "(id, user_id, token_hash, issued_at, expires_at) VALUES (?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        userId,
        "a".repeat(64),
        NOW,
        NOW.plusSeconds(3600));
    execute("DELETE FROM user_accounts WHERE id = ?", userId);
    assertThat(scalar("SELECT COUNT(*) FROM refresh_sessions", new Object[0])).isEqualTo(0L);
  }

  @Test
  void trackingProjectionIsUniquePerShipmentVersion() throws SQLException {
    UUID shipmentId = UUID.randomUUID();
    insertTrackingEvent(shipmentId, 1L, "UF666666666666666666");
    assertThatThrownBy(() -> insertTrackingEvent(shipmentId, 1L, "UF666666666666666666"))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("uq_tracking_shipment_version");
  }

  @Test
  void courierCapacityCannotBeExceeded() {
    assertThatThrownBy(() -> insertCourier(2, 3))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ck_couriers_capacity");
  }

  @Test
  void oneShipmentCannotHaveTwoDeliveryRoutes() throws SQLException {
    UUID courierId = insertCourier(5, 0);
    UUID shipmentId = UUID.randomUUID();
    insertRoute(shipmentId, courierId);
    assertThatThrownBy(() -> insertRoute(shipmentId, courierId))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("uq_delivery_routes_shipment");
  }

  @Test
  void notificationSourceEventIsIdempotent() throws SQLException {
    UUID sourceEventId = UUID.randomUUID();
    insertNotification(sourceEventId, "PENDING", null, null);
    assertThatThrownBy(() -> insertNotification(sourceEventId, "PENDING", null, null))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("uq_notifications_source_event");
  }

  @Test
  void deliveredNotificationRequiresProviderAcknowledgement() {
    assertThatThrownBy(() -> insertNotification(UUID.randomUUID(), "DELIVERED", NOW, null))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ck_notifications_delivery");
  }

  @Test
  void warehouseCodeIsUnique() throws SQLException {
    insertWarehouse("SOF-01");
    assertThatThrownBy(() -> insertWarehouse("SOF-01"))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("warehouses_code_key");
  }

  @Test
  void logisticsHubRequiresExistingWarehouse() {
    assertThatThrownBy(() -> insertHub(UUID.randomUUID(), "Missing owner"))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("logistics_hubs_warehouse_id_fkey");
  }

  @Test
  void hubNameIsUniqueWithinItsWarehouse() throws SQLException {
    UUID warehouseId = insertWarehouse("SOF-HUB");
    insertHub(warehouseId, "North Hub");
    assertThatThrownBy(() -> insertHub(warehouseId, "North Hub"))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("uq_logistics_hubs_warehouse_name");
  }

  @ParameterizedTest(name = "rejects inconsistent incident state {0}")
  @MethodSource("invalidIncidentStates")
  void rejectsInconsistentIncidentResolution(String status, Instant resolvedAt) {
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO incidents "
                        + "(id, shipment_id, type, description, status, created_at, resolved_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "DELIVERY_DELAY",
                    "Synthetic incident",
                    status,
                    NOW,
                    resolvedAt))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ck_incidents_resolution");
  }

  @Test
  void featureFlagKeyIsUnique() throws SQLException {
    insertFeatureFlag("courier-offline-sync");
    assertThatThrownBy(() -> insertFeatureFlag("courier-offline-sync"))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("feature_flags_flag_key_key");
  }

  @Test
  void timestampsRoundTripAsUtcInstants() throws SQLException {
    UUID id = insertWarehouse("UTC-01");
    Object value = scalar("SELECT created_at FROM warehouses WHERE id = ?", id);
    assertThat(((Timestamp) value).toInstant()).isEqualTo(NOW);
  }

  @Test
  void cleanupProvidesIndependentEmptyFixtures() throws SQLException {
    assertThat(scalar("SELECT COUNT(*) FROM shipments", new Object[0])).isEqualTo(0L);
    assertThat(scalar("SELECT COUNT(*) FROM user_accounts", new Object[0])).isEqualTo(0L);
    assertThat(scalar("SELECT COUNT(*) FROM audit_records", new Object[0])).isEqualTo(0L);
  }

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static void insertShipment(UUID id, String trackingNumber, int weight)
      throws SQLException {
    execute(
        "INSERT INTO shipments (id, customer_id, tracking_number, status, delivery_type, "
            + "pickup_contact_name, pickup_line_1, pickup_city, pickup_postal_code, "
            + "pickup_country_code, pickup_phone, delivery_contact_name, delivery_line_1, "
            + "delivery_city, delivery_postal_code, delivery_country_code, delivery_phone, "
            + "weight_grams, length_cm, width_cm, height_cm, description, created_at, updated_at) "
            + "VALUES (?, ?, ?, 'CREATED', 'STANDARD', 'Synthetic Sender', '1 Test Street', "
            + "'Sofia', '1000', 'BG', '+359880000001', 'Synthetic Receiver', '2 Test Street', "
            + "'Varna', '9000', 'BG', '+359880000002', ?, 10, 20, 30, 'Synthetic parcel', ?, ?)",
        id,
        UUID.randomUUID(),
        trackingNumber,
        weight,
        NOW,
        NOW);
  }

  private static void insertOutbox(UUID shipmentId, long version, String type) throws SQLException {
    execute(
        "INSERT INTO outbox_events "
            + "(id, aggregate_id, aggregate_version, event_type, schema_version, payload, "
            + "correlation_id, trace_id, occurred_at) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)",
        UUID.randomUUID(),
        shipmentId,
        version,
        type,
        "1.0",
        "{\"status\":\"CREATED\"}",
        "database-test",
        "0123456789abcdef0123456789abcdef",
        NOW);
  }

  private static void insertUser(UUID id, String email, String role) throws SQLException {
    execute(
        "INSERT INTO user_accounts "
            + "(id, email, display_name, password_hash, role, status, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, ?)",
        id,
        email,
        "Synthetic User",
        "bcrypt-hash",
        role,
        NOW,
        NOW);
  }

  private static void insertTrackingEvent(UUID shipmentId, long version, String tracking)
      throws SQLException {
    execute(
        "INSERT INTO tracking_events "
            + "(id, shipment_id, tracking_number, status, aggregate_version, event_type, "
            + "occurred_at, received_at, correlation_id, trace_id) "
            + "VALUES (?, ?, ?, 'CONFIRMED', ?, 'ShipmentConfirmed', ?, ?, ?, ?)",
        UUID.randomUUID(),
        shipmentId,
        tracking,
        version,
        NOW,
        NOW,
        "database-test",
        "0123456789abcdef0123456789abcdef");
  }

  private static UUID insertCourier(int maximum, int active) throws SQLException {
    UUID id = UUID.randomUUID();
    execute(
        "INSERT INTO couriers "
            + "(id, display_name, status, maximum_assignments, active_assignments, updated_at) "
            + "VALUES (?, 'Synthetic Courier', 'AVAILABLE', ?, ?, ?)",
        id,
        maximum,
        active,
        NOW);
    return id;
  }

  private static void insertRoute(UUID shipmentId, UUID courierId) throws SQLException {
    execute(
        "INSERT INTO delivery_routes (id, shipment_id, courier_id, status, created_at) "
            + "VALUES (?, ?, ?, 'PLANNED', ?)",
        UUID.randomUUID(),
        shipmentId,
        courierId,
        NOW);
  }

  private static void insertNotification(
      UUID sourceEventId, String status, Instant deliveredAt, String providerId)
      throws SQLException {
    execute(
        "INSERT INTO notifications "
            + "(id, source_event_id, customer_id, shipment_id, type, subject, body, status, "
            + "next_attempt_at, provider_message_id, created_at, delivered_at) "
            + "VALUES (?, ?, ?, ?, 'ShipmentCreated', 'Created', 'Created safely', ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        sourceEventId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        status,
        NOW,
        providerId,
        NOW,
        deliveredAt);
  }

  private static UUID insertWarehouse(String code) throws SQLException {
    UUID id = UUID.randomUUID();
    execute(
        "INSERT INTO warehouses "
            + "(id, code, name, city, capacity, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
        id,
        code,
        "Synthetic Warehouse",
        "Sofia",
        1000,
        NOW,
        NOW);
    return id;
  }

  private static void insertHub(UUID warehouseId, String name) throws SQLException {
    execute(
        "INSERT INTO logistics_hubs (id, warehouse_id, name, city, updated_at) "
            + "VALUES (?, ?, ?, 'Sofia', ?)",
        UUID.randomUUID(),
        warehouseId,
        name,
        NOW);
  }

  private static void insertFeatureFlag(String key) throws SQLException {
    execute(
        "INSERT INTO feature_flags (id, flag_key, description, enabled, updated_at) "
            + "VALUES (?, ?, 'Synthetic flag', false, ?)",
        UUID.randomUUID(),
        key,
        NOW);
  }

  private static List<String> tableNames() throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {
      try (ResultSet result = statement.executeQuery()) {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        while (result.next()) {
          names.add(result.getString(1));
        }
        return names;
      }
    }
  }

  private static Object scalar(String sql, Object... parameters) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, parameters);
      try (ResultSet result = statement.executeQuery()) {
        assertThat(result.next()).isTrue();
        return result.getObject(1);
      }
    }
  }

  private static int update(String sql, Object... parameters) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, parameters);
      return statement.executeUpdate();
    }
  }

  private static void execute(String sql, Object... parameters) throws SQLException {
    try (Connection connection = connection()) {
      execute(connection, sql, parameters);
    }
  }

  private static void execute(Connection connection, String sql, Object... parameters)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, parameters);
      statement.executeUpdate();
    }
  }

  private static void bind(PreparedStatement statement, Object... parameters) throws SQLException {
    for (int index = 0; index < parameters.length; index++) {
      Object value = parameters[index];
      if (value instanceof Instant instant) {
        statement.setTimestamp(index + 1, Timestamp.from(instant));
      } else {
        statement.setObject(index + 1, value);
      }
    }
  }

  private static String read(String relativePath) {
    try {
      return Files.readString(WORKSPACE.resolve(relativePath));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static String tracking(int value) {
    return "UF" + String.format(java.util.Locale.ROOT, "%018X", Math.abs(value));
  }

  private static Stream<Integer> validWeights() {
    return Stream.of(1, 50_000);
  }

  private static Stream<Integer> invalidWeights() {
    return Stream.of(0, 50_001);
  }

  private static Stream<Arguments> invalidIncidentStates() {
    return Stream.of(Arguments.of("OPEN", NOW), Arguments.of("RESOLVED", NOW.minusSeconds(1)));
  }
}
