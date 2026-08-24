package io.urbanflow.routing.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.urbanflow.routing.application.AssignmentResult;
import io.urbanflow.routing.application.RoutingAssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ShipmentEventConsumerTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private RoutingAssignmentService assignments;

  @Test
  void deserializesAndDelegatesAssignment() {
    ShipmentEventEnvelope event =
        ShipmentEventEnvelopeTest.event(
            "ShipmentConfirmed", "CONFIRMED", "1.0", "shipment-service");
    when(objectMapper.readValue("json", ShipmentEventEnvelope.class)).thenReturn(event);
    when(assignments.assign(event)).thenReturn(AssignmentResult.ASSIGNED);

    assertThat(new ShipmentEventConsumer(objectMapper, assignments).consume("json"))
        .isEqualTo(AssignmentResult.ASSIGNED);
  }

  @Test
  void classifiesMalformedRecordWithoutInvokingDomain() {
    when(objectMapper.readValue("bad", ShipmentEventEnvelope.class))
        .thenThrow(new IllegalArgumentException("bad JSON"));

    assertThatThrownBy(() -> new ShipmentEventConsumer(objectMapper, assignments).consume("bad"))
        .isInstanceOf(RoutingEventDeserializationException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(assignments);
  }

  @Test
  void preservesAssignmentFailures() {
    ShipmentEventEnvelope event =
        ShipmentEventEnvelopeTest.event(
            "ShipmentConfirmed", "CONFIRMED", "1.0", "shipment-service");
    IllegalStateException failure = new IllegalStateException("assignment failed");
    when(objectMapper.readValue("json", ShipmentEventEnvelope.class)).thenReturn(event);
    when(assignments.assign(event)).thenThrow(failure);

    assertThatThrownBy(() -> new ShipmentEventConsumer(objectMapper, assignments).consume("json"))
        .isSameAs(failure);
  }
}
