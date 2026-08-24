package io.urbanflow.ui.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Creates independent browser-storage documents with synthetic reserved-domain identities. */
public final class PortalStateFixtures {

  public static final String CUSTOMER_STORE = "urbanflow.customer.v1";
  public static final String COURIER_STORE = "urbanflow.courier.v1";
  public static final String OPERATIONS_STORE = "urbanflow.operations.v1";
  public static final String PASSWORD = "Quality!Pass25";

  private PortalStateFixtures() {}

  /** Returns an authenticated customer state with no shipments. */
  public static Map<String, Object> customer() {
    return customer(true, false, System.currentTimeMillis());
  }

  /** Returns customer state with configurable session and lock status. */
  public static Map<String, Object> customer(
      boolean authenticated, boolean locked, long touchedAt) {
    String userId = UUID.randomUUID().toString();
    Map<String, Object> user = new LinkedHashMap<>();
    user.put("id", userId);
    user.put("name", "Synthetic Customer");
    user.put("email", "customer-" + UUID.randomUUID() + "@example.test");
    user.put("passwordHash", sha256(PASSWORD));
    user.put("failures", locked ? 5 : 0);
    user.put("locked", locked);
    user.put("role", "CUSTOMER");

    Map<String, Object> state = new LinkedHashMap<>();
    state.put("users", new ArrayList<>(List.of(user)));
    state.put(
        "session",
        authenticated
            ? new LinkedHashMap<>(
                Map.of("userId", userId, "touchedAt", touchedAt, "role", "CUSTOMER"))
            : null);
    state.put("addresses", new ArrayList<>());
    state.put("shipments", new ArrayList<>());
    state.put("incidents", new ArrayList<>());
    state.put("language", "en");
    return state;
  }

  /** Adds one owned shipment and status history to customer state. */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> customerWithShipment(String status) {
    Map<String, Object> state = customer();
    final Map<String, Object> session = (Map<String, Object>) state.get("session");
    String shipmentId = UUID.randomUUID().toString();
    String now = Instant.now().toString();
    List<Map<String, Object>> history = new ArrayList<>();
    history.add(new LinkedHashMap<>(Map.of("status", "CREATED", "at", now)));
    if (!"CREATED".equals(status)) {
      history.add(new LinkedHashMap<>(Map.of("status", status, "at", now)));
    }
    Map<String, Object> shipment = new LinkedHashMap<>();
    shipment.put("id", shipmentId);
    shipment.put("ownerId", session.get("userId"));
    shipment.put("trackingNumber", "UF-A1B2C3D4");
    shipment.put("pickup", "Sofia Test Hub");
    shipment.put("delivery", "Varna Synthetic Address");
    shipment.put("phone", "+359888123456");
    shipment.put("weight", 2.5);
    shipment.put("dimensions", List.of(30, 20, 15));
    shipment.put("description", "Synthetic parcel");
    shipment.put("type", "STANDARD");
    shipment.put("status", status);
    shipment.put("version", 0);
    shipment.put("createdAt", now);
    shipment.put("history", history);
    ((List<Map<String, Object>>) state.get("shipments")).add(shipment);
    return state;
  }

  /** Returns an authenticated courier with three deterministic assignments. */
  public static Map<String, Object> courier(String status, boolean routeStarted, boolean offline) {
    List<Map<String, Object>> assignments = new ArrayList<>();
    for (int index = 1; index <= 3; index++) {
      Map<String, Object> assignment = new LinkedHashMap<>();
      assignment.put("id", UUID.randomUUID().toString());
      assignment.put("courierId", "123456");
      assignment.put("stop", index);
      assignment.put("tracking", "UF-QA00000" + index);
      assignment.put("pickup", "Synthetic Hub " + index);
      assignment.put("delivery", "Test Zone " + index);
      assignment.put("service", index == 1 ? "EXPRESS" : "STANDARD");
      assignment.put("status", status);
      assignment.put("queued", false);
      assignment.put("history", new ArrayList<>());
      assignments.add(assignment);
    }
    Map<String, Object> state = new LinkedHashMap<>();
    state.put(
        "session",
        new LinkedHashMap<>(
            Map.of("courierId", "123456", "startedAt", System.currentTimeMillis())));
    state.put("routeStarted", routeStarted);
    state.put("offline", offline);
    state.put("assignments", assignments);
    state.put("queue", new ArrayList<>());
    return state;
  }

  /** Returns operations state by allowing the application to create its deterministic seed. */
  public static Map<String, Object> operationsSession(String role) {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put(
        "session",
        new LinkedHashMap<>(
            Map.of(
                "operatorId", "QA-0001", "role", role, "signedInAt", System.currentTimeMillis())));
    state.put("page", 0);
    return state;
  }

  /** Computes the same browser-side password digest used by the customer portal. */
  public static String sha256(String value) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 must be available", exception);
    }
  }
}
