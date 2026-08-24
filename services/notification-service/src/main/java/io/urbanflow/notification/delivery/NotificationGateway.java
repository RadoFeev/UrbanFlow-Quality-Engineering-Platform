package io.urbanflow.notification.delivery;

import io.urbanflow.notification.domain.Notification;

/** Port for the internal customer communications gateway. */
public interface NotificationGateway {

  /** Delivers one idempotent message and returns the provider identifier. */
  String deliver(Notification notification);
}
