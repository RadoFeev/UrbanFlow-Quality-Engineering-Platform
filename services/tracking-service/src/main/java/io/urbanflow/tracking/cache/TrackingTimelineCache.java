package io.urbanflow.tracking.cache;

import io.urbanflow.tracking.application.TrackingTimelineItem;
import java.util.List;
import java.util.Optional;

/** Cache port that preserves query availability when Redis is unavailable. */
public interface TrackingTimelineCache {

  /** Returns a cached timeline or empty on miss/fallback. */
  Optional<List<TrackingTimelineItem>> get(String trackingNumber);

  /** Stores a complete deterministic timeline. */
  void put(String trackingNumber, List<TrackingTimelineItem> timeline);

  /** Removes cached data after a successful projection update. */
  void evict(String trackingNumber);
}
