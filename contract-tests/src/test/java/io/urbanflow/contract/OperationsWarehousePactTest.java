package io.urbanflow.contract;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("contract")
@PactConsumerTest
class OperationsWarehousePactTest {

  private static final String WAREHOUSE_ID = "57c0e69b-3b31-43f5-966d-ac182c2b1496";
  private static final String ACTOR_ID = "3b0b8bf4-511f-42fd-9808-d90340fc9dbf";
  private static final String WAREHOUSE =
      "{\"id\":\""
          + WAREHOUSE_ID
          + "\",\"code\":\"SOF01\",\"name\":\"Sofia Central\",\"city\":\"Sofia\","
          + "\"capacity\":10000,\"active\":true,\"createdAt\":\"2026-08-24T12:00:00Z\","
          + "\"updatedAt\":\"2026-08-24T12:00:00Z\",\"version\":0}";
  private final PactHttpClient client = new PactHttpClient();

  @Pact(provider = "warehouse-service", consumer = "operations-dashboard")
  V4Pact searchWarehouses(PactDslWithProvider builder) {
    return builder
        .given("active warehouses exist")
        .uponReceiving("operations searches the first warehouse page")
        .path("/api/v1/warehouses")
        .query("page=0&size=20&sort=name&direction=asc")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            "{\"items\":["
                + WAREHOUSE
                + "],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1}")
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "warehouse-service", pactMethod = "searchWarehouses")
  void searchesWarehousePage(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(),
            "GET",
            "/api/v1/warehouses?page=0&size=20&sort=name&direction=asc",
            null,
            Map.of());
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).contains("SOF01", "totalElements");
  }

  @Pact(provider = "warehouse-service", consumer = "operations-dashboard")
  V4Pact updateWarehouse(PactDslWithProvider builder) {
    String request =
        "{\"name\":\"Sofia Central Updated\",\"city\":\"Sofia\","
            + "\"capacity\":12000,\"active\":true}";
    return builder
        .given("warehouse exists at version zero", Map.of("warehouseId", WAREHOUSE_ID))
        .uponReceiving("operations updates the current warehouse version")
        .path("/api/v1/warehouses/" + WAREHOUSE_ID)
        .method("PUT")
        .headers(
            Map.of(
                "Content-Type", "application/json",
                "X-Actor-Id", ACTOR_ID,
                "If-Match", "\"v0\""))
        .body(request)
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json", "ETag", "\"v1\""))
        .body(
            WAREHOUSE
                .replace("Sofia Central", "Sofia Central Updated")
                .replace("\"version\":0", "\"version\":1"))
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "warehouse-service", pactMethod = "updateWarehouse")
  void updatesWarehouseWithStrongVersion(MockServer server) {
    String body =
        "{\"name\":\"Sofia Central Updated\",\"city\":\"Sofia\","
            + "\"capacity\":12000,\"active\":true}";
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(),
            "PUT",
            "/api/v1/warehouses/" + WAREHOUSE_ID,
            body,
            Map.of(
                "Content-Type", "application/json",
                "X-Actor-Id", ACTOR_ID,
                "If-Match", "\"v0\""));
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).contains("Sofia Central Updated", "\"version\":1");
  }

  @Pact(provider = "warehouse-service", consumer = "operations-dashboard")
  V4Pact staleWarehouseVersion(PactDslWithProvider builder) {
    return builder
        .given("warehouse exists at version two", Map.of("warehouseId", WAREHOUSE_ID))
        .uponReceiving("operations sends a stale warehouse version")
        .path("/api/v1/warehouses/" + WAREHOUSE_ID)
        .method("PUT")
        .headers(
            Map.of(
                "Content-Type", "application/json",
                "X-Actor-Id", ACTOR_ID,
                "If-Match", "\"v0\""))
        .body("{\"name\":\"Stale\",\"city\":\"Sofia\",\"capacity\":10000,\"active\":true}")
        .willRespondWith()
        .status(409)
        .headers(Map.of("Content-Type", "application/json"))
        .body("{\"code\":\"VERSION_CONFLICT\",\"status\":409}")
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "warehouse-service", pactMethod = "staleWarehouseVersion")
  void rejectsStaleWarehouseVersion(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(),
            "PUT",
            "/api/v1/warehouses/" + WAREHOUSE_ID,
            "{\"name\":\"Stale\",\"city\":\"Sofia\",\"capacity\":10000,\"active\":true}",
            Map.of(
                "Content-Type", "application/json",
                "X-Actor-Id", ACTOR_ID,
                "If-Match", "\"v0\""));
    assertThat(response.status()).isEqualTo(409);
    assertThat(response.body()).contains("VERSION_CONFLICT");
  }

  @Pact(provider = "warehouse-service", consumer = "operations-dashboard")
  V4Pact missingWarehouse(PactDslWithProvider builder) {
    return builder
        .given("warehouse does not exist", Map.of("warehouseId", WAREHOUSE_ID))
        .uponReceiving("operations reads a missing warehouse")
        .path("/api/v1/warehouses/" + WAREHOUSE_ID)
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .body("{\"code\":\"ENTITY_NOT_FOUND\",\"status\":404}")
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "warehouse-service", pactMethod = "missingWarehouse")
  void handlesMissingWarehouse(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(), "GET", "/api/v1/warehouses/" + WAREHOUSE_ID, null, Map.of());
    assertThat(response.status()).isEqualTo(404);
    assertThat(response.body()).contains("ENTITY_NOT_FOUND");
  }
}
