package io.urbanflow.notification.application;

/** Safe customer-facing subject and body selected by event type. */
public record NotificationTemplate(String type, String subject, String body) {}
