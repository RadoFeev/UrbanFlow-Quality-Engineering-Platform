package io.urbanflow.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Versioned customer notification with retry and dead-letter state. */
@Entity
@Table(name = "notifications")
public class Notification {

  @Id private UUID id;

  @Column(name = "source_event_id", nullable = false, unique = true)
  private UUID sourceEventId;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "shipment_id", nullable = false)
  private UUID shipmentId;

  @Column(nullable = false, length = 80)
  private String type;

  @Column(nullable = false, length = 160)
  private String subject;

  @Column(nullable = false, length = 1000)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NotificationStatus status;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "provider_message_id", length = 120)
  private String providerMessageId;

  @Column(name = "last_error", length = 500)
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Constructor reserved for persistence. */
  protected Notification() {}

  /** Creates a pending, source-event-idempotent customer notification. */
  public static Notification create(
      UUID id,
      UUID sourceEventId,
      UUID customerId,
      UUID shipmentId,
      String type,
      String subject,
      String body,
      Instant now) {
    Notification notification = new Notification();
    notification.id = Objects.requireNonNull(id, "id must not be null");
    notification.sourceEventId =
        Objects.requireNonNull(sourceEventId, "sourceEventId must not be null");
    notification.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
    notification.shipmentId = Objects.requireNonNull(shipmentId, "shipmentId must not be null");
    notification.type = requireText(type, "type");
    notification.subject = requireText(subject, "subject");
    notification.body = requireText(body, "body");
    notification.status = NotificationStatus.PENDING;
    notification.createdAt = Objects.requireNonNull(now, "now must not be null");
    notification.nextAttemptAt = now;
    return notification;
  }

  /** Marks a provider-acknowledged delivery. */
  public void markDelivered(String providerMessageId, Instant now) {
    ensureDeliverable();
    attempts++;
    this.providerMessageId = requireText(providerMessageId, "providerMessageId");
    status = NotificationStatus.DELIVERED;
    deliveredAt = Objects.requireNonNull(now, "now must not be null");
    lastError = null;
  }

  /** Records a failure, scheduling retry or terminal dead-letter state. */
  public void markFailed(String error, int maximumAttempts, Instant now) {
    ensureDeliverable();
    attempts++;
    lastError = truncate(requireText(error, "error"), 500);
    if (attempts >= maximumAttempts) {
      status = NotificationStatus.DEAD_LETTER;
    } else {
      status = NotificationStatus.RETRY;
      long delay = Math.min(300, 1L << Math.min(attempts - 1, 8));
      nextAttemptAt = now.plusSeconds(delay);
    }
  }

  /** Returns whether the worker may attempt delivery at this instant. */
  public boolean isDue(Instant now) {
    return (status == NotificationStatus.PENDING || status == NotificationStatus.RETRY)
        && !nextAttemptAt.isAfter(now);
  }

  /** Returns the notification identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the source event identifier. */
  public UUID sourceEventId() {
    return sourceEventId;
  }

  /** Returns the customer identifier. */
  public UUID customerId() {
    return customerId;
  }

  /** Returns the shipment identifier. */
  public UUID shipmentId() {
    return shipmentId;
  }

  /** Returns the notification type. */
  public String type() {
    return type;
  }

  /** Returns the display subject. */
  public String subject() {
    return subject;
  }

  /** Returns the display body. */
  public String body() {
    return body;
  }

  /** Returns the delivery state. */
  public NotificationStatus status() {
    return status;
  }

  /** Returns completed attempts. */
  public int attempts() {
    return attempts;
  }

  /** Returns the next retry time. */
  public Instant nextAttemptAt() {
    return nextAttemptAt;
  }

  /** Returns the provider message ID or null. */
  public String providerMessageId() {
    return providerMessageId;
  }

  /** Returns the last sanitized error or null. */
  public String lastError() {
    return lastError;
  }

  /** Returns the creation time. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns the delivery time or null. */
  public Instant deliveredAt() {
    return deliveredAt;
  }

  /** Returns the optimistic persistence version. */
  public long version() {
    return version;
  }

  private void ensureDeliverable() {
    if (status == NotificationStatus.DELIVERED || status == NotificationStatus.DEAD_LETTER) {
      throw new IllegalStateException("terminal notification cannot be delivered again");
    }
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  private static String truncate(String value, int maximum) {
    return value.length() <= maximum ? value : value.substring(0, maximum);
  }
}
