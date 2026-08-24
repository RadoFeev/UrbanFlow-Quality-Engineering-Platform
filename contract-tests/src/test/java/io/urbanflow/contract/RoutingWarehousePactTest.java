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
class RoutingWarehousePactTest {

  private static final String WAREHOUSE_ID = "3ee61398-e08a-44c5-967e-c54afcc826d1";
  private static final String HUB_ID = "a87a3fb7-5fc1-4130-ac28-f37e22cdf538";
  private static final String WAREHOUSE =
      "{\"id\":\""
          + WAREHOUSE_ID
          + "\",\"code\":\"SOF01\",\"name\":\"Sofia Central\",\"city\":\"Sofia\","
          + "\"capacity\":10000,\"active\":true,\"createdAt\":\"2026-08-24T12:00:00Z\","
          + "\"updatedAt\":\"2026-08-24T12:00:00Z\",\"version\":0}";
  private final PactHttpClient client = new PactHttpClient();

  @Pact(provider = "warehouse-service", consumer = "routing-service")
  V4Pact candidateWarehouses(PactDslWithProvider builder) {
    return builder
        .given("active Sofia warehouses exist")
        .uponReceiving("routing searches candidate warehouses")
        .path("/api/v1/warehouses")
        .query("query=Sofia&page=0&size=20&sort=name&direction=asc")
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
  @PactTestFor(providerName = "warehouse-service", pactMethod = "candidateWarehouses")
  void findsCandidateWarehouse(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(),
            "GET",
            "/api/v1/warehouses?query=Sofia&page=0&size=20&sort=name&direction=asc",
            null,
            Map.of());
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).contains("SOF01", "Sofia Central");
  }

  @Pact(provider = "warehouse-service", consumer = "routing-service")
  V4Pact selectedWarehouse(PactDslWithProvider builder) {
    return builder
        .given("routing warehouse exists", Map.of("warehouseId", WAREHOUSE_ID))
        .uponReceiving("routing reads a selected warehouse")
        .path("/api/v1/warehouses/" + WAREHOUSE_ID)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json", "ETag", "\"v0\""))
        .body(WAREHOUSE)
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "warehouse-service", pactMethod = "selectedWarehouse")
  void readsSelectedWarehouse(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(), "GET", "/api/v1/warehouses/" + WAREHOUSE_ID, null, Map.of());
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).contains(WAREHOUSE_ID, "\"active\":true");
  }

  @Pact(provider = "warehouse-service", consumer = "routing-service")
  V4Pact warehouseHubs(PactDslWithProvider builder) {
    String hub =
        "{\"id\":\""
            + HUB_ID
            + "\",\"warehouseId\":\""
            + WAREHOUSE_ID
            + "\",\"name\":\"Sofia East\",\"city\":\"Sofia\",\"active\":true,"
            + "\"updatedAt\":\"2026-08-24T12:00:00Z\",\"version\":0}";
    return builder
        .given("routing warehouse has active hubs", Map.of("warehouseId", WAREHOUSE_ID))
        .uponReceiving("routing lists warehouse hubs")
        .path("/api/v1/warehouses/" + WAREHOUSE_ID + "/hubs")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body("[" + hub + "]")
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "warehouse-service", pactMethod = "warehouseHubs")
  void listsRoutingHubs(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(), "GET", "/api/v1/warehouses/" + WAREHOUSE_ID + "/hubs", null, Map.of());
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).contains(HUB_ID, "Sofia East");
  }

  @Pact(provider = "warehouse-service", consumer = "routing-service")
  V4Pact missingWarehouse(PactDslWithProvider builder) {
    return builder
        .given("routing warehouse does not exist", Map.of("warehouseId", WAREHOUSE_ID))
        .uponReceiving("routing reads a missing warehouse")
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
  void handlesMissingRoutingWarehouse(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(), "GET", "/api/v1/warehouses/" + WAREHOUSE_ID, null, Map.of());
    assertThat(response.status()).isEqualTo(404);
    assertThat(response.body()).contains("ENTITY_NOT_FOUND");
  }
}
