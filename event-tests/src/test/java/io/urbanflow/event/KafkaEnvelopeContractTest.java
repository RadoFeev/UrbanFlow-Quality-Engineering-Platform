package io.urbanflow.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.urbanflow.event.support.EventFixtures;
import io.urbanflow.event.support.EventFixtures.EventEnvelope;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Wire-contract coverage for every event required by the delivery lifecycle. */
@Tag("event")
@Epic("Event-driven delivery platform")
@Feature("Versioned Kafka envelope")
class KafkaEnvelopeContractTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @ParameterizedTest(name = "{0} uses its owned topic and aggregate key")
  @ValueSource(
      strings = {
        "ShipmentCreated", "ShipmentConfirmed", "CourierAssigned", "ShipmentPickedUp",
        "ShipmentArrivedAtHub", "ShipmentDispatched", "DeliveryAttempted", "ShipmentDelivered",
        "ShipmentCancelled", "ShipmentReturnStarted", "NotificationRequested"
      })
  void publishesOwnedEventWithTopicAndKey(String type) {
    EventEnvelope event = EventFixtures.event(type);
    MockProducer<String, String> producer =
        new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());

    producer.send(EventFixtures.record(event));

    assertThat(producer.history())
        .singleElement()
        .satisfies(
            record -> {
              assertThat(record.topic()).isEqualTo(EventFixtures.topic(type));
              assertThat(record.key()).isEqualTo(event.aggregateId().toString());
            });
  }

  @ParameterizedTest(name = "wire envelope contains {0}")
  @MethodSource("metadata")
  void wireEnvelopeContainsRequiredMetadata(
      String field, java.util.function.Predicate<JsonNode> rule) throws Exception {
    ProducerRecord<String, String> record =
        EventFixtures.record(EventFixtures.event("ShipmentCreated"));
    JsonNode root = JSON.readTree(record.value());

    assertThat(rule.test(root.get(field))).as(field).isTrue();
  }

  @ParameterizedTest(name = "Kafka header {0} is propagated")
  @ValueSource(
      strings = {
        "eventId",
        "eventType",
        "correlationId",
        "traceId",
        "schemaVersion",
        "contentType"
      })
  void propagatesRequiredHeaders(String name) {
    ProducerRecord<String, String> record =
        EventFixtures.record(EventFixtures.event("ShipmentConfirmed"));

    assertThat(record.headers().lastHeader(name)).isNotNull();
    assertThat(new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8))
        .isNotBlank();
  }

  @ParameterizedTest(name = "payload for {0} identifies shipment and status")
  @ValueSource(
      strings = {
        "ShipmentCreated", "ShipmentConfirmed", "CourierAssigned", "ShipmentPickedUp",
        "ShipmentArrivedAtHub", "ShipmentDispatched", "DeliveryAttempted", "ShipmentDelivered",
        "ShipmentCancelled", "ShipmentReturnStarted", "NotificationRequested"
      })
  void payloadCarriesOwnedShipmentIdentity(String type) throws Exception {
    JsonNode root = JSON.readTree(EventFixtures.record(EventFixtures.event(type)).value());

    assertThat(root.at("/payload/shipmentId").asText()).isNotBlank();
    assertThat(root.at("/payload/trackingNumber").asText()).startsWith("UF-QA");
    assertThat(root.at("/payload/status").asText()).isNotBlank();
  }

  private static Stream<Arguments> metadata() {
    return Stream.of(
        Arguments.of(
            "eventId", (java.util.function.Predicate<JsonNode>) node -> uuid(node.asText())),
        Arguments.of(
            "eventType", (java.util.function.Predicate<JsonNode>) node -> !node.asText().isBlank()),
        Arguments.of(
            "aggregateId", (java.util.function.Predicate<JsonNode>) node -> uuid(node.asText())),
        Arguments.of(
            "aggregateVersion",
            (java.util.function.Predicate<JsonNode>) node -> node.asLong() >= 0),
        Arguments.of(
            "occurredAt", (java.util.function.Predicate<JsonNode>) node -> instant(node.asText())),
        Arguments.of(
            "schemaVersion",
            (java.util.function.Predicate<JsonNode>) node -> node.asText().matches("1\\.\\d+")),
        Arguments.of(
            "correlationId", (java.util.function.Predicate<JsonNode>) node -> uuid(node.asText())),
        Arguments.of(
            "traceId",
            (java.util.function.Predicate<JsonNode>)
                node -> node.asText().matches("00-[0-9a-f]{32}-[0-9a-f]{16}-01")),
        Arguments.of(
            "producer",
            (java.util.function.Predicate<JsonNode>) node -> node.asText().endsWith("-service")));
  }

  private static boolean uuid(String value) {
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private static boolean instant(String value) {
    try {
      Instant.parse(value);
      return true;
    } catch (java.time.format.DateTimeParseException exception) {
      return false;
    }
  }
}
