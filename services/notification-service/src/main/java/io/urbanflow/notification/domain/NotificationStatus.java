package io.urbanflow.notification.domain;

/** Durable notification delivery state. */
public enum NotificationStatus {
  PENDING,
  RETRY,
  DELIVERED,
  DEAD_LETTER
}
