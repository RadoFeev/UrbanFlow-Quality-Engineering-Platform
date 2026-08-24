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
class CustomerPortalShipmentPactTest {

  private static final String SHIPMENT_ID = "59a53a9c-4b44-4d4b-b93e-0c44e2e07ef4";
  private static final String CUSTOMER_ID = "67915956-da92-4df9-837f-27f36bf97c41";
  private static final String CREATE_BODY =
      """
      {"customerId":"67915956-da92-4df9-837f-27f36bf97c41",
       "pickupAddress":{"contactName":"Sender","line1":"1 Test Street","city":"Sofia","postalCode":"1000","countryCode":"BG","phone":"+359880000001"},
       "deliveryAddress":{"contactName":"Receiver","line1":"2 Test Street","city":"Varna","postalCode":"9000","countryCode":"BG","phone":"+359880000002"},
       "parcel":{"weightGrams":2500,"lengthCm":30,"widthCm":20,"heightCm":15,"description":"Synthetic parcel"},
       "deliveryType":"STANDARD"}
      """;
  private static final String SHIPMENT_BODY =
      "{\"id\":\""
          + SHIPMENT_ID
          + "\",\"customerId\":\""
          + CUSTOMER_ID
          + "\",\"trackingNumber\":\"UF0123456789ABCDEF01\",\"status\":\"CREATED\","
          + "\"version\":0}";
  private final PactHttpClient client = new PactHttpClient();

  @Pact(provider = "shipment-service", consumer = "customer-portal")
  V4Pact createShipment(PactDslWithProvider builder) {
    return builder
        .given("shipment creation is available")
        .uponReceiving("a valid idempotent shipment creation request")
        .path("/api/v1/shipments")
        .method("POST")
        .headers(Map.of("Content-Type", "application/json", "Idempotency-Key", "pact-create-0001"))
        .body(CREATE_BODY)
        .willRespondWith()
        .status(201)
        .headers(Map.of("Content-Type", "application/json", "ETag", "\"0\""))
        .body(SHIPMENT_BODY)
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "shipment-service", pactMethod = "createShipment")
  void createsShipmentFromCustomerPortal(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(),
            "POST",
            "/api/v1/shipments",
            CREATE_BODY,
            Map.of("Content-Type", "application/json", "Idempotency-Key", "pact-create-0001"));
    assertThat(response.status()).isEqualTo(201);
    assertThat(response.body()).contains(SHIPMENT_ID, "CREATED", "UF0123456789ABCDEF01");
  }

  @Pact(provider = "shipment-service", consumer = "customer-portal")
  V4Pact replayShipment(PactDslWithProvider builder) {
    return builder
        .given("an identical shipment request was already accepted")
        .uponReceiving("the same idempotency key and body")
        .path("/api/v1/shipments")
        .method("POST")
        .headers(Map.of("Content-Type", "application/json", "Idempotency-Key", "pact-replay-0001"))
        .body(CREATE_BODY)
        .willRespondWith()
        .status(201)
        .headers(Map.of("Content-Type", "application/json", "ETag", "\"0\""))
        .body(SHIPMENT_BODY)
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "shipment-service", pactMethod = "replayShipment")
  void replaysOriginalShipmentForIdenticalRetry(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(),
            "POST",
            "/api/v1/shipments",
            CREATE_BODY,
            Map.of("Content-Type", "application/json", "Idempotency-Key", "pact-replay-0001"));
    assertThat(response.status()).isEqualTo(201);
    assertThat(response.body()).contains(SHIPMENT_ID);
  }

  @Pact(provider = "shipment-service", consumer = "customer-portal")
  V4Pact invalidShipment(PactDslWithProvider builder) {
    return builder
        .given("shipment request validation is active")
        .uponReceiving("a shipment without a delivery address")
        .path("/api/v1/shipments")
        .method("POST")
        .headers(Map.of("Content-Type", "application/json", "Idempotency-Key", "pact-invalid-001"))
        .body("{\"customerId\":\"" + CUSTOMER_ID + "\",\"deliveryType\":\"STANDARD\"}")
        .willRespondWith()
        .status(400)
        .headers(Map.of("Content-Type", "application/json"))
        .body("{\"code\":\"VALIDATION_FAILED\",\"status\":400}")
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "shipment-service", pactMethod = "invalidShipment")
  void rejectsIncompleteShipmentContract(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(
            server.getUrl(),
            "POST",
            "/api/v1/shipments",
            "{\"customerId\":\"" + CUSTOMER_ID + "\",\"deliveryType\":\"STANDARD\"}",
            Map.of("Content-Type", "application/json", "Idempotency-Key", "pact-invalid-001"));
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.body()).contains("VALIDATION_FAILED");
  }

  @Pact(provider = "shipment-service", consumer = "customer-portal")
  V4Pact missingShipment(PactDslWithProvider builder) {
    return builder
        .given("the shipment does not exist", Map.of("shipmentId", SHIPMENT_ID))
        .uponReceiving("a request for a missing shipment")
        .path("/api/v1/shipments/" + SHIPMENT_ID)
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .body("{\"code\":\"SHIPMENT_NOT_FOUND\",\"status\":404}")
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "shipment-service", pactMethod = "missingShipment")
  void handlesMissingShipmentWithoutAmbiguity(MockServer server) {
    PactHttpClient.Response response =
        client.exchange(server.getUrl(), "GET", "/api/v1/shipments/" + SHIPMENT_ID, null, Map.of());
    assertThat(response.status()).isEqualTo(404);
    assertThat(response.body()).contains("SHIPMENT_NOT_FOUND");
  }
}
