package io.urbanflow.routing.event;

import io.urbanflow.routing.application.AssignmentResult;
import io.urbanflow.routing.application.RoutingAssignmentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Deserializes shipment events and delegates transactional assignment. */
@Component
public class ShipmentEventConsumer {

  private final ObjectMapper objectMapper;
  private final RoutingAssignmentService assignments;

  /** Creates the Kafka consumer boundary. */
  public ShipmentEventConsumer(ObjectMapper objectMapper, RoutingAssignmentService assignments) {
    this.objectMapper = objectMapper;
    this.assignments = assignments;
  }

  /** Processes one Kafka record value. */
  @KafkaListener(
      topics = "${urbanflow.routing.consumer.topic:urbanflow.shipment.events.v1}",
      groupId = "${urbanflow.routing.consumer.group-id:routing-service-v1}")
  public AssignmentResult consume(String value) {
    ShipmentEventEnvelope event;
    try {
      event = objectMapper.readValue(value, ShipmentEventEnvelope.class);
    } catch (Exception exception) {
      throw new RoutingEventDeserializationException(exception);
    }
    return assignments.assign(event);
  }
}
