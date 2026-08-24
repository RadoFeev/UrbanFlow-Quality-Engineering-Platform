package io.urbanflow.notification.api;

import io.urbanflow.notification.domain.Notification;
import io.urbanflow.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

/** Safe customer notification feed item. */
public record NotificationResponse(
    UUID id,
    UUID shipmentId,
    String type,
    String subject,
    String body,
    NotificationStatus status,
    Instant createdAt,
    Instant deliveredAt) {

  /** Maps a notification without provider or failure internals. */
  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.id(),
        notification.shipmentId(),
        notification.type(),
        notification.subject(),
        notification.body(),
        notification.status(),
        notification.createdAt(),
        notification.deliveredAt());
  }
}
