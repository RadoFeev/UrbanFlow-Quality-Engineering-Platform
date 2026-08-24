package io.urbanflow.notification.event;

/** Signals a malformed shipment event value. */
public class NotificationEventDeserializationException extends RuntimeException {

  /** Creates the consumer-boundary failure. */
  public NotificationEventDeserializationException(Throwable cause) {
    super("shipment event could not be deserialized", cause);
  }
}
