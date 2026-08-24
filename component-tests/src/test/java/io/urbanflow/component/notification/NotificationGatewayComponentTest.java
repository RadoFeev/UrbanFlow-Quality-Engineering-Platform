package io.urbanflow.component.notification;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.urbanflow.notification.delivery.HttpNotificationGateway;
import io.urbanflow.notification.delivery.NotificationGatewayException;
import io.urbanflow.notification.domain.Notification;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Tag("component")
class NotificationGatewayComponentTest {

  private static final UUID NOTIFICATION_ID =
      UUID.fromString("a53b4fa8-61b0-4b4f-afbd-f646ecdf81f2");
  private static final UUID SOURCE_EVENT_ID =
      UUID.fromString("02413e6a-a6b1-43cb-8ab3-fd83a7cf2b0d");
  private static final UUID CUSTOMER_ID = UUID.fromString("ee27926b-696f-4f6a-b211-254ca8c4ba9b");
  private static final UUID SHIPMENT_ID = UUID.fromString("d87f7a20-e292-478c-9c8f-ec0396dd1dc7");
  private static WireMockServer provider;
  private static HttpNotificationGateway gateway;

  @BeforeAll
  static void startProvider() {
    provider = new WireMockServer(wireMockConfig().dynamicPort());
    provider.start();
    JdkClientHttpRequestFactory requestFactory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build());
    gateway =
        new HttpNotificationGateway(
            RestClient.builder()
                .baseUrl(provider.baseUrl())
                .requestFactory(requestFactory)
                .build());
  }

  @AfterEach
  void resetProvider() {
    provider.resetAll();
  }

  @AfterAll
  static void stopProvider() {
    if (provider != null) {
      provider.stop();
    }
  }

  @ParameterizedTest(name = "serializes {0} without losing the event type")
  @MethodSource("eventTypes")
  void serializesEverySupportedNotificationFamily(String type) {
    acknowledge("provider-" + type.toLowerCase(java.util.Locale.ROOT));
    Notification notification = notification(type, "Status update", "Shipment state changed");

    assertThat(gateway.deliver(notification)).startsWith("provider-");

    provider.verify(
        postRequestedFor(urlEqualTo("/v1/messages"))
            .withHeader("Content-Type", equalTo(MediaType.APPLICATION_JSON_VALUE))
            .withHeader("Idempotency-Key", equalTo(NOTIFICATION_ID.toString()))
            .withRequestBody(matchingJsonPath("$.customerId", equalTo(CUSTOMER_ID.toString())))
            .withRequestBody(matchingJsonPath("$.type", equalTo(type)))
            .withRequestBody(matchingJsonPath("$.subject", equalTo("Status update")))
            .withRequestBody(matchingJsonPath("$.body", equalTo("Shipment state changed"))));
  }

  @ParameterizedTest(name = "accepts provider acknowledgement {0}")
  @MethodSource("validAcknowledgements")
  void acceptsNonBlankProviderAcknowledgements(String messageId) {
    acknowledge(messageId);
    assertThat(gateway.deliver(notification("ShipmentDelivered", "Delivered", "Delivered")))
        .isEqualTo(messageId);
  }

  @ParameterizedTest(name = "rejects invalid acknowledgement partition {index}")
  @MethodSource("invalidAcknowledgements")
  void rejectsMissingOrBlankProviderAcknowledgements(String responseBody) {
    provider.stubFor(
        post(urlEqualTo("/v1/messages"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(responseBody)));
    assertThatThrownBy(
            () ->
                gateway.deliver(notification("ShipmentDelivered", "Delivered", "Delivered safely")))
        .isInstanceOf(NotificationGatewayException.class)
        .hasMessageContaining("messageId");
  }

  @ParameterizedTest(name = "propagates provider HTTP status {0}")
  @MethodSource("providerFailures")
  void propagatesProviderHttpFailuresForRetryClassification(int status) {
    provider.stubFor(
        post(urlEqualTo("/v1/messages"))
            .willReturn(aResponse().withStatus(status).withBody("{\"code\":\"provider_error\"}")));
    assertThatThrownBy(
            () -> gateway.deliver(notification("ShipmentCreated", "Created", "Created safely")))
        .isInstanceOf(RestClientResponseException.class)
        .extracting(exception -> ((RestClientResponseException) exception).getStatusCode().value())
        .isEqualTo(status);
  }

  @Test
  void repeatsUseTheSameStableIdempotencyKey() {
    acknowledge("provider-repeat");
    Notification notification = notification("ShipmentConfirmed", "Confirmed", "Confirmed");
    gateway.deliver(notification);
    gateway.deliver(notification);
    provider.verify(
        2,
        postRequestedFor(urlEqualTo("/v1/messages"))
            .withHeader("Idempotency-Key", equalTo(NOTIFICATION_ID.toString())));
  }

  @Test
  void outboundPayloadExcludesInternalAndSensitiveFields() {
    acknowledge("provider-minimal");
    gateway.deliver(
        notification(
            "CourierAssigned", "Courier assigned", "A courier was assigned to your shipment"));
    String json = provider.getAllServeEvents().getFirst().getRequest().getBodyAsString();
    assertThat(json)
        .doesNotContain(SOURCE_EVENT_ID.toString(), SHIPMENT_ID.toString())
        .doesNotContain("status", "attempts", "providerMessageId", "address", "phone")
        .contains(CUSTOMER_ID.toString());
  }

  @Test
  void utf8CustomerFacingContentIsPreserved() {
    acknowledge("provider-utf8");
    gateway.deliver(notification("ShipmentDelivered", "Пратката е доставена", "Благодарим ви!"));
    String json = provider.getAllServeEvents().getFirst().getRequest().getBodyAsString();
    assertThat(json).contains("Пратката е доставена", "Благодарим ви!");
  }

  private static void acknowledge(String messageId) {
    provider.stubFor(
        post(urlEqualTo("/v1/messages"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"messageId\":\"" + messageId + "\"}")));
  }

  private static Notification notification(String type, String subject, String body) {
    return Notification.create(
        NOTIFICATION_ID,
        SOURCE_EVENT_ID,
        CUSTOMER_ID,
        SHIPMENT_ID,
        type,
        subject,
        body,
        Instant.parse("2026-08-24T12:00:00Z"));
  }

  private static Stream<String> eventTypes() {
    return Stream.of(
        "ShipmentCreated",
        "ShipmentConfirmed",
        "CourierAssigned",
        "ShipmentPickedUp",
        "ShipmentArrivedAtHub",
        "ShipmentDispatched",
        "DeliveryAttempted",
        "ShipmentDelivered",
        "ShipmentCancelled",
        "ShipmentReturnStarted",
        "NotificationRequested");
  }

  private static Stream<String> validAcknowledgements() {
    return Stream.of(
        "provider-1", "MSG_2026_000042", "c0e8658a-a200-4b6b-b418-9c772302edcd", "bg-42");
  }

  private static Stream<Arguments> invalidAcknowledgements() {
    return Stream.of(
        Arguments.of("{}"),
        Arguments.of("{\"messageId\":null}"),
        Arguments.of("{\"messageId\":\"\"}"),
        Arguments.of("{\"messageId\":\"   \"}"));
  }

  private static Stream<Integer> providerFailures() {
    return Stream.of(400, 401, 409, 429, 500, 503);
  }
}
