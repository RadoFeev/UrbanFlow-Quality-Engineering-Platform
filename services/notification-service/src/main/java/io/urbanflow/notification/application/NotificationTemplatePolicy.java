package io.urbanflow.notification.application;

import io.urbanflow.notification.event.ShipmentEventEnvelope;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Central allowlist of shipment events that produce customer messages. */
@Component
public class NotificationTemplatePolicy {

  private static final Map<String, NotificationTemplate> TEMPLATES =
      Map.of(
          "ShipmentCreated",
          template("SHIPMENT_CREATED", "Shipment created", "Your shipment is being prepared."),
          "ShipmentConfirmed",
          template("SHIPMENT_CONFIRMED", "Shipment confirmed", "Your shipment is confirmed."),
          "ShipmentOutForDelivery",
          template("OUT_FOR_DELIVERY", "Out for delivery", "Your shipment is out for delivery."),
          "ShipmentDelivered",
          template("SHIPMENT_DELIVERED", "Shipment delivered", "Your shipment was delivered."),
          "DeliveryAttempted",
          template("DELIVERY_ATTEMPTED", "Delivery attempted", "A delivery attempt was recorded."),
          "ShipmentCancelled",
          template("SHIPMENT_CANCELLED", "Shipment cancelled", "Your shipment was cancelled."),
          "ShipmentReturnStarted",
          template("RETURN_STARTED", "Return started", "Your shipment return has started."));

  /** Returns an allowlisted template enriched only with the public tracking number. */
  public Optional<NotificationTemplate> select(ShipmentEventEnvelope event) {
    return Optional.ofNullable(TEMPLATES.get(event.eventType()))
        .map(
            template ->
                new NotificationTemplate(
                    template.type(),
                    template.subject(),
                    template.body() + " Tracking number: " + event.payload().trackingNumber()));
  }

  private static NotificationTemplate template(String type, String subject, String body) {
    return new NotificationTemplate(type, subject, body);
  }
}
