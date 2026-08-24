package io.urbanflow.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.urbanflow.tracking.cache.TrackingTimelineCache;
import io.urbanflow.tracking.domain.TrackingStatus;
import io.urbanflow.tracking.persistence.TrackingEventProjection;
import io.urbanflow.tracking.persistence.TrackingEventRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackingQueryServiceTest {

  private static final String TRACKING_NUMBER = "UF1123456789ABCDEF01";

  @Mock private TrackingEventRepository repository;
  @Mock private TrackingTimelineCache cache;
  @Mock private TrackingEventProjection projection;

  private TrackingQueryService service;

  @BeforeEach
  void setUp() {
    service = new TrackingQueryService(repository, cache);
  }

  @Test
  void returnsDefensiveCopyFromCacheWithoutDatabaseQuery() {
    List<TrackingTimelineItem> cached = new ArrayList<>(List.of(item()));
    when(cache.get(TRACKING_NUMBER)).thenReturn(Optional.of(cached));

    List<TrackingTimelineItem> result = service.timeline(TRACKING_NUMBER);

    assertThat(result).containsExactly(item());
    assertThatThrownBy(() -> result.add(item())).isInstanceOf(UnsupportedOperationException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void loadsOrderedDatabaseTimelineAndPopulatesCacheOnMiss() {
    when(cache.get(TRACKING_NUMBER)).thenReturn(Optional.empty());
    when(repository.findByTrackingNumberOrderByAggregateVersionAscOccurredAtAscIdAsc(
            TRACKING_NUMBER))
        .thenReturn(List.of(projection));
    stubProjection();

    List<TrackingTimelineItem> result = service.timeline(TRACKING_NUMBER);

    assertThat(result).containsExactly(item());
    verify(cache).put(TRACKING_NUMBER, result);
  }

  @Test
  void reportsNotFoundWithoutCachingEmptyTimeline() {
    when(cache.get(TRACKING_NUMBER)).thenReturn(Optional.empty());
    when(repository.findByTrackingNumberOrderByAggregateVersionAscOccurredAtAscIdAsc(
            TRACKING_NUMBER))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.timeline(TRACKING_NUMBER))
        .isInstanceOf(TrackingNotFoundException.class)
        .hasMessageContaining("not found");
    verify(cache, never())
        .put(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList());
  }

  private void stubProjection() {
    when(projection.id()).thenReturn(item().eventId());
    when(projection.status()).thenReturn(item().status());
    when(projection.aggregateVersion()).thenReturn(item().version());
    when(projection.occurredAt()).thenReturn(item().occurredAt());
    when(projection.correlationId()).thenReturn(item().correlationId());
    when(projection.traceId()).thenReturn(item().traceId());
  }

  private static TrackingTimelineItem item() {
    return new TrackingTimelineItem(
        UUID.fromString("364eea66-5c57-402d-9bcd-848a4e953ac5"),
        TrackingStatus.CONFIRMED,
        1,
        Instant.parse("2026-08-24T12:10:00Z"),
        "corr-tracking-003",
        "fedcba9876543210fedcba9876543210");
  }
}
