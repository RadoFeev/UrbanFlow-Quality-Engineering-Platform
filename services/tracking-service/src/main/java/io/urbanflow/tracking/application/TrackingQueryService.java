package io.urbanflow.tracking.application;

import io.urbanflow.tracking.cache.TrackingTimelineCache;
import io.urbanflow.tracking.persistence.TrackingEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serves deterministic tracking timelines through a resilient cache-aside query. */
@Service
public class TrackingQueryService {

  private final TrackingEventRepository repository;
  private final TrackingTimelineCache cache;

  TrackingQueryService(TrackingEventRepository repository, TrackingTimelineCache cache) {
    this.repository = repository;
    this.cache = cache;
  }

  /** Returns a cached or database-backed immutable timeline. */
  @Transactional(readOnly = true)
  public List<TrackingTimelineItem> timeline(String trackingNumber) {
    return cache
        .get(trackingNumber)
        .map(List::copyOf)
        .orElseGet(() -> loadAndCache(trackingNumber));
  }

  private List<TrackingTimelineItem> loadAndCache(String trackingNumber) {
    List<TrackingTimelineItem> timeline =
        repository
            .findByTrackingNumberOrderByAggregateVersionAscOccurredAtAscIdAsc(trackingNumber)
            .stream()
            .map(TrackingTimelineItem::from)
            .toList();
    if (timeline.isEmpty()) {
      throw new TrackingNotFoundException();
    }
    cache.put(trackingNumber, timeline);
    return timeline;
  }
}
