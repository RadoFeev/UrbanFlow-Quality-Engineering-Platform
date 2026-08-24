package io.urbanflow.routing.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.urbanflow.routing.config.RoutingOutboxProperties;
import io.urbanflow.routing.domain.DeliveryRoute;
import io.urbanflow.routing.persistence.RoutingOutboxEvent;
import io.urbanflow.routing.persistence.RoutingOutboxRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RoutingOutboxPublisherTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Mock private RoutingOutboxRepository outbox;
  @Mock private KafkaTemplate<String, String> kafka;

  private ObjectMapper mapper;
  private SimpleMeterRegistry meters;
  private RoutingOutboxPublisher publisher;

  @BeforeEach
  void setUp() {
    mapper = JsonMapper.builder().findAndAddModules().build();
    meters = new SimpleMeterRegistry();
    publisher =
        new RoutingOutboxPublisher(
            outbox,
            kafka,
            mapper,
            new RoutingOutboxProperties("urbanflow.routing.events.v1", Duration.ofSeconds(1)),
            Clock.fixed(NOW, ZoneOffset.UTC),
            meters);
  }

  @Test
  void publishesEnvelopeWithOrderingKeyAndTraceHeaders() throws Exception {
    RoutingOutboxEvent event = event();
    when(outbox.findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(NOW))
        .thenReturn(List.of(event));
    when(kafka.send(org.mockito.ArgumentMatchers.<ProducerRecord<String, String>>any()))
        .thenReturn(CompletableFuture.completedFuture(null));

    assertThat(publisher.publishDue()).isEqualTo(1);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<ProducerRecord<String, String>> record =
        ArgumentCaptor.forClass(ProducerRecord.class);
    verify(kafka).send(record.capture());
    assertThat(record.getValue().topic()).isEqualTo("urbanflow.routing.events.v1");
    assertThat(record.getValue().key()).isEqualTo(event.shipmentId().toString());
    assertThat(mapper.readTree(record.getValue().value()).get("eventType").asString())
        .isEqualTo("CourierAssigned");
    assertThat(header(record.getValue(), "traceId")).isEqualTo("0123456789abcdef0123456789abcdef");
    assertThat(event.publishedAt()).isEqualTo(NOW);
    assertThat(metric("published")).isEqualTo(1.0);
  }

  @Test
  void retainsFailedEventForRetry() {
    RoutingOutboxEvent event = event();
    when(outbox.findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(NOW))
        .thenReturn(List.of(event));
    when(kafka.send(org.mockito.ArgumentMatchers.<ProducerRecord<String, String>>any()))
        .thenReturn(
            CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

    assertThat(publisher.publishDue()).isZero();

    assertThat(event.publishedAt()).isNull();
    assertThat(event.attempts()).isEqualTo(1);
    assertThat(event.nextAttemptAt()).isEqualTo(NOW.plusSeconds(1));
    verify(outbox).save(event);
    assertThat(metric("failed")).isEqualTo(1.0);
  }

  @Test
  void returnsZeroForEmptyDueBatch() {
    when(outbox.findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(NOW))
        .thenReturn(List.of());
    assertThat(publisher.publishDue()).isZero();
  }

  private double metric(String result) {
    return meters.get("urbanflow.routing.outbox").tag("result", result).counter().count();
  }

  private static String header(ProducerRecord<String, String> record, String name) {
    return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
  }

  private static RoutingOutboxEvent event() {
    DeliveryRoute route =
        DeliveryRoute.plan(
            UUID.fromString("547f06cf-c1dd-4ad0-9780-359af05811b9"),
            UUID.fromString("7d38bce9-4f03-4269-b1db-a19fc9358679"),
            UUID.fromString("6be406c1-022e-4d36-bb5c-2da15e311fb0"),
            NOW);
    return RoutingOutboxEvent.courierAssigned(
        route, "corr-routing-001", "0123456789abcdef0123456789abcdef", NOW);
  }
}
