package io.urbanflow.tracking.event;

import io.urbanflow.tracking.application.ProjectionResult;
import io.urbanflow.tracking.application.TrackingProjectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Deserializes shipment records and delegates idempotent timeline projection. */
@Component
@ConditionalOnProperty(
    prefix = "urbanflow.tracking.consumer",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ShipmentEventConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentEventConsumer.class);

  private final ObjectMapper objectMapper;
  private final TrackingProjectionService projectionService;

  ShipmentEventConsumer(ObjectMapper objectMapper, TrackingProjectionService projectionService) {
    this.objectMapper = objectMapper;
    this.projectionService = projectionService;
  }

  /** Processes one Kafka value and returns its projection outcome for direct verification. */
  @KafkaListener(
      topics = "${urbanflow.tracking.consumer.topic:urbanflow.shipment.events.v1}",
      groupId = "${urbanflow.tracking.consumer.group-id:tracking-service-v1}")
  public ProjectionResult consume(String value) {
    ShipmentEventEnvelope event;
    try {
      event = objectMapper.readValue(value, ShipmentEventEnvelope.class);
    } catch (Exception exception) {
      throw new EventDeserializationException(exception);
    }
    ProjectionResult result = projectionService.project(event);
    LOGGER.debug("Shipment event consumed eventId={} result={}", event.eventId(), result.name());
    return result;
  }
}
