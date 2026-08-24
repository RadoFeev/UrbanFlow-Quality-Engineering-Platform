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
class CourierPortalTrackingPactTest {

  private static final String TRACKING = "UF0123456789ABCDEF01";
  private static final String TRACE = "0123456789abcdef0123456789abcdef";
  private static final String CREATED_TIMELINE =
      "{\"trackingNumber\":\""
          + TRACKING
          + "\",\"currentStatus\":\"CREATED\",\"events\":[{\"eventId\":"
          + "\"258c87e3-21ea-42f6-afc2-68006f5999d0\",\"status\":\"CREATED\","
          + "\"version\":0,\"occurredAt\":\"2026-08-24T12:00:00Z\","
          + "\"correlationId\":\"pact-track\",\"traceId\":\""
          + TRACE
          + "\"}],\"correlationId\":\"pact-track\",\"traceId\":\""
          + TRACE
          + "\"}";
  private final PactHttpClient client = new PactHttpClient();

  @Pact(provider = "tracking-service", consumer = "courier-portal")
  V4Pact createdTimeline(PactDslWithProvider builder) {
    return builder
        .given("a created shipment timeline exists")
        .uponReceiving("the courier requests the initial timeline")
        .path("/api/v1/tracking/" + TRACKING)
        .method("GET")
        .headers(Map.of("X-Correlation-ID", "pact-track"))
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json", "Cache-Control", "no-store"))
        .body(CREATED_TIMELINE)
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "tracking-service", pactMethod = "createdTimeline")
  void readsCreatedTimeline(MockServer server) {
    PactHttpClient.Response response = get(server, TRACKING);
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).contains("CREATED", TRACKING, TRACE);
  }

  @Pact(provider = "tracking-service", consumer = "courier-portal")
  V4Pact deliveredTimeline(PactDslWithProvider builder) {
    String body =
        CREATED_TIMELINE
            .replace("\"CREATED\"", "\"DELIVERED\"")
            .replace("\"version\":0", "\"version\":10");
    return builder
        .given("a delivered shipment timeline exists")
        .uponReceiving("the courier requests the completed timeline")
        .path("/api/v1/tracking/" + TRACKING)
        .method("GET")
        .headers(Map.of("X-Correlation-ID", "pact-track"))
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json", "Cache-Control", "no-store"))
        .body(body)
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "tracking-service", pactMethod = "deliveredTimeline")
  void readsDeliveredTimeline(MockServer server) {
    PactHttpClient.Response response = get(server, TRACKING);
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body()).contains("DELIVERED", "\"version\":10");
  }

  @Pact(provider = "tracking-service", consumer = "courier-portal")
  V4Pact missingTimeline(PactDslWithProvider builder) {
    return builder
        .given("no timeline exists for the tracking number")
        .uponReceiving("the courier requests a missing timeline")
        .path("/api/v1/tracking/" + TRACKING)
        .method("GET")
        .headers(Map.of("X-Correlation-ID", "pact-track"))
        .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .body("{\"code\":\"TRACKING_NOT_FOUND\",\"status\":404}")
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "tracking-service", pactMethod = "missingTimeline")
  void handlesMissingTimeline(MockServer server) {
    PactHttpClient.Response response = get(server, TRACKING);
    assertThat(response.status()).isEqualTo(404);
    assertThat(response.body()).contains("TRACKING_NOT_FOUND");
  }

  @Pact(provider = "tracking-service", consumer = "courier-portal")
  V4Pact invalidTrackingNumber(PactDslWithProvider builder) {
    return builder
        .given("tracking syntax validation is active")
        .uponReceiving("the courier submits an invalid tracking number")
        .path("/api/v1/tracking/invalid-number")
        .method("GET")
        .headers(Map.of("X-Correlation-ID", "pact-track"))
        .willRespondWith()
        .status(400)
        .headers(Map.of("Content-Type", "application/json"))
        .body("{\"code\":\"INVALID_TRACKING_NUMBER\",\"status\":400}")
        .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(providerName = "tracking-service", pactMethod = "invalidTrackingNumber")
  void rejectsInvalidTrackingNumber(MockServer server) {
    PactHttpClient.Response response = get(server, "invalid-number");
    assertThat(response.status()).isEqualTo(400);
    assertThat(response.body()).contains("INVALID_TRACKING_NUMBER");
  }

  private PactHttpClient.Response get(MockServer server, String tracking) {
    return client.exchange(
        server.getUrl(),
        "GET",
        "/api/v1/tracking/" + tracking,
        null,
        Map.of("X-Correlation-ID", "pact-track"));
  }
}
