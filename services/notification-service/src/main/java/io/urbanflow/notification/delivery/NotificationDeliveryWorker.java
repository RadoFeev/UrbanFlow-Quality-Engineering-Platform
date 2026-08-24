package io.urbanflow.notification.delivery;

import io.micrometer.core.instrument.MeterRegistry;
import io.urbanflow.notification.config.NotificationProperties;
import io.urbanflow.notification.domain.Notification;
import io.urbanflow.notification.domain.NotificationStatus;
import io.urbanflow.notification.persistence.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Delivers due notifications with bounded retry, DLT state, and local overlap suppression. */
@Component
public class NotificationDeliveryWorker {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDeliveryWorker.class);
  private static final Set<NotificationStatus> DELIVERABLE =
      Set.of(NotificationStatus.PENDING, NotificationStatus.RETRY);

  private final NotificationRepository notifications;
  private final NotificationGateway gateway;
  private final NotificationProperties properties;
  private final Clock clock;
  private final MeterRegistry meters;
  private final AtomicBoolean running = new AtomicBoolean();

  /** Creates the delivery worker. */
  public NotificationDeliveryWorker(
      NotificationRepository notifications,
      NotificationGateway gateway,
      NotificationProperties properties,
      Clock clock,
      MeterRegistry meters) {
    this.notifications = notifications;
    this.gateway = gateway;
    this.properties = properties;
    this.clock = clock;
    this.meters = meters;
  }

  /** Processes one bounded due batch. */
  @Scheduled(fixedDelayString = "${urbanflow.notification.poll-delay:PT1S}")
  public int deliverDue() {
    if (!running.compareAndSet(false, true)) {
      return 0;
    }
    try {
      Instant now = clock.instant();
      return notifications
          .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(DELIVERABLE, now)
          .stream()
          .mapToInt(notification -> deliverOne(notification, now))
          .sum();
    } finally {
      running.set(false);
    }
  }

  private int deliverOne(Notification notification, Instant now) {
    try {
      notification.markDelivered(gateway.deliver(notification), now);
      notifications.save(notification);
      count("delivered");
      return 1;
    } catch (Exception exception) {
      notification.markFailed(
          exception.getClass().getSimpleName(), properties.maximumAttempts(), now);
      notifications.save(notification);
      count(notification.status() == NotificationStatus.DEAD_LETTER ? "dead_letter" : "retry");
      LOGGER.warn(
          "Notification delivery failed notificationId={} attempt={} status={} errorType={}",
          notification.id(),
          notification.attempts(),
          notification.status(),
          exception.getClass().getSimpleName());
      return 0;
    }
  }

  private void count(String result) {
    meters.counter("urbanflow.notification.delivery", "result", result).increment();
  }
}
