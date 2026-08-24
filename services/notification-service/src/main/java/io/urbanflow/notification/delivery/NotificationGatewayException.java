package io.urbanflow.notification.delivery;

/** Signals an invalid communications gateway response. */
public class NotificationGatewayException extends RuntimeException {

  /** Creates a safe gateway failure. */
  public NotificationGatewayException(String message) {
    super(message);
  }
}
