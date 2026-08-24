package io.urbanflow.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.urbanflow.notification.domain.Notification;
import io.urbanflow.notification.event.ShipmentEventEnvelope;
import io.urbanflow.notification.persistence.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationProcessingServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Mock private NotificationRepository notifications;
  @Mock private NotificationTemplatePolicy templates;

  private SimpleMeterRegistry meters;
  private NotificationProcessingService service;

  @BeforeEach
  void setUp() {
    meters = new SimpleMeterRegistry();
    service =
        new NotificationProcessingService(
            notifications, templates, Clock.fixed(NOW, ZoneOffset.UTC), meters);
  }

  @Test
  void createsExactlyOneDurableNotificationForAllowlistedEvent() {
    ShipmentEventEnvelope event = NotificationTemplatePolicyTest.event("ShipmentConfirmed");
    when(templates.select(event))
        .thenReturn(
            Optional.of(
                new NotificationTemplate(
                    "SHIPMENT_CONFIRMED", "Confirmed", "Tracking UF0123456789ABCDEF01")));

    assertThat(service.process(event)).isEqualTo(ProcessingResult.CREATED);

    ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).save(saved.capture());
    assertThat(saved.getValue().sourceEventId()).isEqualTo(event.eventId());
    assertThat(saved.getValue().customerId()).isEqualTo(event.payload().customerId());
    assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
    assertThat(metric("created")).isEqualTo(1.0);
  }

  @Test
  void acknowledgesDuplicateSourceEvent() {
    ShipmentEventEnvelope event = NotificationTemplatePolicyTest.event("ShipmentConfirmed");
    when(notifications.existsBySourceEventId(event.eventId())).thenReturn(true);

    assertThat(service.process(event)).isEqualTo(ProcessingResult.DUPLICATE);

    verifyNoInteractions(templates);
    verify(notifications, never()).save(any());
  }

  @Test
  void ignoresSupportedEventWithoutTemplate() {
    ShipmentEventEnvelope event = NotificationTemplatePolicyTest.event("ShipmentPickedUp");
    when(templates.select(event)).thenReturn(Optional.empty());

    assertThat(service.process(event)).isEqualTo(ProcessingResult.IGNORED);

    verify(notifications, never()).save(any());
  }

  @Test
  void ignoresUnsupportedProducerBeforeRepositoryAccess() {
    ShipmentEventEnvelope valid = NotificationTemplatePolicyTest.event("ShipmentConfirmed");
    ShipmentEventEnvelope unsupported =
        new ShipmentEventEnvelope(
            valid.eventId(),
            valid.eventType(),
            valid.aggregateId(),
            valid.aggregateVersion(),
            valid.occurredAt(),
            valid.schemaVersion(),
            valid.correlationId(),
            valid.traceId(),
            "unknown-service",
            valid.payload());

    assertThat(service.process(unsupported)).isEqualTo(ProcessingResult.IGNORED);

    verifyNoInteractions(notifications, templates);
  }

  @Test
  void ignoresUnsupportedSchema() {
    ShipmentEventEnvelope valid = NotificationTemplatePolicyTest.event("ShipmentConfirmed");
    ShipmentEventEnvelope unsupported =
        new ShipmentEventEnvelope(
            valid.eventId(),
            valid.eventType(),
            valid.aggregateId(),
            valid.aggregateVersion(),
            valid.occurredAt(),
            "2.0",
            valid.correlationId(),
            valid.traceId(),
            valid.producer(),
            valid.payload());

    assertThat(service.process(unsupported)).isEqualTo(ProcessingResult.IGNORED);
  }

  private double metric(String result) {
    return meters.get("urbanflow.notification.processing").tag("result", result).counter().count();
  }
}
