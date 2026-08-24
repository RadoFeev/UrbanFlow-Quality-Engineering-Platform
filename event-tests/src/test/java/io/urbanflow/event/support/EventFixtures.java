package io.urbanflow.event.support;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import tools.jackson.databind.json.JsonMapper;

/** Builds versioned synthetic event envelopes and their exact Kafka wire representation. */
public final class EventFixtures {

  public static final String SHIPMENT_TOPIC = "urbanflow.shipment-events.v1";
  public static final String ROUTING_TOPIC = "urbanflow.routing-events.v1";
  public static final String NOTIFICATION_TOPIC = "urbanflow.notification-events.v1";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private EventFixtures() {}

  /** Returns a valid envelope for an owned event type. */
  public static EventEnvelope event(String type) {
    UUID aggregateId = UUID.randomUUID();
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("shipmentId", aggregateId.toString());
    payload.put(
        "trackingNumber",
        "UF-QA" + aggregateId.toString().replace("-", "").substring(0, 6).toUpperCase());
    payload.put("status", status(type));
    return new EventEnvelope(
        UUID.randomUUID(),
        type,
        aggregateId,
        1,
        Instant.now(),
        "1.0",
        UUID.randomUUID().toString(),
        "00-" + "1".repeat(32) + "-" + "2".repeat(16) + "-01",
        producer(type),
        Map.copyOf(payload));
  }

  /** Converts an envelope to its acknowledged Kafka producer record contract. */
  public static ProducerRecord<String, String> record(EventEnvelope event) {
    RecordHeaders headers = new RecordHeaders();
    add(headers, "eventId", event.eventId().toString());
    add(headers, "eventType", event.eventType());
    add(headers, "correlationId", event.correlationId());
    add(headers, "traceId", event.traceId());
    add(headers, "schemaVersion", event.schemaVersion());
    add(headers, "contentType", "application/json");
    return new ProducerRecord<>(
        topic(event.eventType()),
        null,
        event.aggregateId().toString(),
        JSON.writeValueAsString(event),
        headers);
  }

  /** Returns the owner topic for an event type. */
  public static String topic(String type) {
    if ("CourierAssigned".equals(type)) {
      return ROUTING_TOPIC;
    }
    if ("NotificationRequested".equals(type)) {
      return NOTIFICATION_TOPIC;
    }
    return SHIPMENT_TOPIC;
  }

  private static void add(RecordHeaders headers, String name, String value) {
    headers.add(name, value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private static String producer(String type) {
    return switch (type) {
      case "CourierAssigned" -> "routing-service";
      case "NotificationRequested" -> "notification-service";
      default -> "shipment-service";
    };
  }

  private static String status(String type) {
    return type.replace("Shipment", "")
        .replace("CourierAssigned", "COURIER_ASSIGNED")
        .replaceAll("([a-z])([A-Z])", "$1_$2")
        .toUpperCase(java.util.Locale.ROOT);
  }

  /** Stable consumer-owned event contract. */
  public record EventEnvelope(
      UUID eventId,
      String eventType,
      UUID aggregateId,
      long aggregateVersion,
      Instant occurredAt,
      String schemaVersion,
      String correlationId,
      String traceId,
      String producer,
      Map<String, Object> payload) {}
}
