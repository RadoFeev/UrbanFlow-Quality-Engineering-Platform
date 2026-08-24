package io.urbanflow.notification.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.urbanflow.notification.domain.Notification;
import io.urbanflow.notification.event.ShipmentEventEnvelope;
import io.urbanflow.notification.persistence.NotificationRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Idempotently converts allowlisted shipment events into durable notifications. */
@Service
public class NotificationProcessingService {

  private final NotificationRepository notifications;
  private final NotificationTemplatePolicy templates;
  private final Clock clock;
  private final MeterRegistry meters;

  /** Creates the event-to-notification workflow. */
  public NotificationProcessingService(
      NotificationRepository notifications,
      NotificationTemplatePolicy templates,
      Clock clock,
      MeterRegistry meters) {
    this.notifications = notifications;
    this.templates = templates;
    this.clock = clock;
    this.meters = meters;
  }

  /** Handles one event with producer/schema validation and durable duplicate protection. */
  @Transactional
  public ProcessingResult process(ShipmentEventEnvelope event) {
    if (!event.isSupported()) {
      return result(ProcessingResult.IGNORED);
    }
    if (notifications.existsBySourceEventId(event.eventId())) {
      return result(ProcessingResult.DUPLICATE);
    }
    return templates
        .select(event)
        .map(
            template -> {
              notifications.save(
                  Notification.create(
                      UUID.randomUUID(),
                      event.eventId(),
                      event.payload().customerId(),
                      event.aggregateId(),
                      template.type(),
                      template.subject(),
                      template.body(),
                      clock.instant()));
              return result(ProcessingResult.CREATED);
            })
        .orElseGet(() -> result(ProcessingResult.IGNORED));
  }

  private ProcessingResult result(ProcessingResult result) {
    meters
        .counter("urbanflow.notification.processing", "result", result.name().toLowerCase())
        .increment();
    return result;
  }
}
