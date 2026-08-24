package io.urbanflow.notification.event;

import io.urbanflow.notification.application.NotificationProcessingService;
import io.urbanflow.notification.application.ProcessingResult;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Deserializes shipment events before invoking notification processing. */
@Component
public class ShipmentEventConsumer {

  private final ObjectMapper objectMapper;
  private final NotificationProcessingService processing;

  /** Creates the Kafka consumer boundary. */
  public ShipmentEventConsumer(
      ObjectMapper objectMapper, NotificationProcessingService processing) {
    this.objectMapper = objectMapper;
    this.processing = processing;
  }

  /** Processes one Kafka value. */
  @KafkaListener(
      topics = "${urbanflow.notification.consumer.topic:urbanflow.shipment.events.v1}",
      groupId = "${urbanflow.notification.consumer.group-id:notification-service-v1}")
  public ProcessingResult consume(String value) {
    ShipmentEventEnvelope event;
    try {
      event = objectMapper.readValue(value, ShipmentEventEnvelope.class);
    } catch (Exception exception) {
      throw new NotificationEventDeserializationException(exception);
    }
    return processing.process(event);
  }
}
