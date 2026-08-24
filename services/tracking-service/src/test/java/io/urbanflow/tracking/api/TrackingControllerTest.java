package io.urbanflow.tracking.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.urbanflow.tracking.application.TrackingQueryService;
import io.urbanflow.tracking.application.TrackingTimelineItem;
import io.urbanflow.tracking.domain.TrackingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TrackingControllerTest {

  private static final String TRACKING_NUMBER = "UF1123456789ABCDEF01";

  @Mock private TrackingQueryService queryService;

  @Test
  void returnsImmutableTimelineWithCurrentStatusAndRequestContext() {
    TrackingController controller = new TrackingController(queryService);
    List<TrackingTimelineItem> events =
        List.of(
            item(0, TrackingStatus.CREATED, "2026-08-24T12:00:00Z"),
            item(1, TrackingStatus.CONFIRMED, "2026-08-24T12:01:00Z"));
    when(queryService.timeline(TRACKING_NUMBER)).thenReturn(events);

    ResponseEntity<TrackingTimelineResponse> response =
        controller.timeline(
            TRACKING_NUMBER, "corr-controller-001", "abcdef0123456789abcdef0123456789");

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    assertThat(response.getHeaders().getFirst(HttpHeaders.VARY))
        .isEqualTo(RequestTracingFilter.CORRELATION_HEADER);
    assertThat(response.getBody())
        .isNotNull()
        .extracting(
            TrackingTimelineResponse::trackingNumber,
            TrackingTimelineResponse::currentStatus,
            TrackingTimelineResponse::correlationId)
        .containsExactly(TRACKING_NUMBER, TrackingStatus.CONFIRMED, "corr-controller-001");
    assertThat(response.getBody().events()).containsExactlyElementsOf(events);
  }

  private static TrackingTimelineItem item(long version, TrackingStatus status, String occurredAt) {
    return new TrackingTimelineItem(
        UUID.randomUUID(),
        status,
        version,
        Instant.parse(occurredAt),
        "corr-controller-001",
        "abcdef0123456789abcdef0123456789");
  }
}
