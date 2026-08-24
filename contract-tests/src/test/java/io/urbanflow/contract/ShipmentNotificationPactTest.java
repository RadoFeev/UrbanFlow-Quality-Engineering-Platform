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
class ShipmentNotificationPactTest {

  private static final String CUSTOMER_ID = "dd4d1bb3-6839-4cbf-8efe-e8fdc8611906";
  private static final String SHIPMENT_ID = "8e2dd34c-877d-49af-844d-8f99e99ec56a";
  private static final String NOTIFICATION_ID = "c4216838-4156-4d46-9df3-e6179a5255ed";
  private static final String PATH = "/api/v1/customers/" + CUSTOMER_ID + "/notifications";
  private final PactHttpClient client = new PactHttpClient();

  @Pact(provider = "notification-service", consumer = "shipment-service")
  V4Pact pendingNotifications(PactDslWithProvider builder) {
    return builder
        .given("customer has a pending shipment notification", Map.of("customerId", CUSTOMER_ID))
        .uponReceiving("shipment support checks a pending notification")
        .path(PATH)
        .query("page=0&size=20")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json", "Cache-Control", "no-store"))
        .body(page("PENDING", null))
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "notification-service", pactMethod = "pendingNotifications")
  void readsPendingNotification(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(server.getUrl(), "GET", PATH + "?page=0&size=20", null, Map.of());
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).contains("PENDING", SHIPMENT_ID);
  }

  @Pact(provider = "notification-service", consumer = "shipment-service")
  V4Pact deliveredNotifications(PactDslWithProvider builder) {
    return builder
        .given("customer has a delivered shipment notification", Map.of("customerId", CUSTOMER_ID))
        .uponReceiving("shipment support checks a delivered notification")
        .path(PATH)
        .query("page=0&size=10")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json", "Cache-Control", "no-store"))
        .body(page("DELIVERED", "2026-08-24T12:01:00Z"))
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "notification-service", pactMethod = "deliveredNotifications")
  void readsDeliveredNotification(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(server.getUrl(), "GET", PATH + "?page=0&size=10", null, Map.of());
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).contains("DELIVERED", "deliveredAt");
  }

  @Pact(provider = "notification-service", consumer = "shipment-service")
  V4Pact emptyNotifications(PactDslWithProvider builder) {
    return builder
        .given("customer has no notifications", Map.of("customerId", CUSTOMER_ID))
        .uponReceiving("shipment support checks an empty notification feed")
        .path(PATH)
        .query("page=0&size=20")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json", "Cache-Control", "no-store"))
        .body("{\"items\":[],\"page\":0,\"size\":20,\"totalElements\":0,\"totalPages\":0}")
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "notification-service", pactMethod = "emptyNotifications")
  void handlesEmptyNotificationFeed(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(server.getUrl(), "GET", PATH + "?page=0&size=20", null, Map.of());
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).contains("\"items\":[]", "\"totalElements\":0");
  }

  @Pact(provider = "notification-service", consumer = "shipment-service")
  V4Pact invalidPagination(PactDslWithProvider builder) {
    return builder
        .given("customer notification feed is available", Map.of("customerId", CUSTOMER_ID))
        .uponReceiving("shipment support requests an invalid notification page")
        .path(PATH)
        .query("page=-1&size=101")
        .method("GET")
        .willRespondWith()
        .status(400)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            "{\"code\":\"INVALID_REQUEST\",\"message\":\"Invalid page boundary\",\"status\":400,"
                + "\"path\":\""
                + PATH
                + "\",\"timestamp\":\"2026-08-24T12:00:00Z\"}")
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "notification-service", pactMethod = "invalidPagination")
  void rejectsInvalidNotificationPagination(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(server.getUrl(), "GET", PATH + "?page=-1&size=101", null, Map.of());
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.body()).contains("INVALID_REQUEST");
  }

  private String page(String status, String deliveredAt) {
    String delivered = deliveredAt == null ? "null" : "\"" + deliveredAt + "\"";
    return "{\"items\":[{\"id\":\""
        + NOTIFICATION_ID
        + "\",\"shipmentId\":\""
        + SHIPMENT_ID
        + "\",\"type\":\"SHIPMENT_STATUS_CHANGED\",\"subject\":\"Shipment update\","
        + "\"body\":\"Shipment status changed\",\"status\":\""
        + status
        + "\",\"createdAt\":\"2026-08-24T12:00:00Z\",\"deliveredAt\":"
        + delivered
        + "}],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1}";
  }
}
