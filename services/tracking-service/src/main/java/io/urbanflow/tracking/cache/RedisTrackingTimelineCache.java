package io.urbanflow.tracking.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.urbanflow.tracking.application.TrackingTimelineItem;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/** Redis JSON adapter with cache-aside metrics and database fallback on failure. */
@Component
public class RedisTrackingTimelineCache implements TrackingTimelineCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisTrackingTimelineCache.class);

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final TrackingCacheProperties properties;
  private final Counter hitCounter;
  private final Counter missCounter;
  private final Counter fallbackCounter;

  RedisTrackingTimelineCache(
      StringRedisTemplate redis,
      ObjectMapper objectMapper,
      TrackingCacheProperties properties,
      MeterRegistry meterRegistry) {
    this.redis = redis;
    this.objectMapper = objectMapper;
    this.properties = properties;
    hitCounter = counter(meterRegistry, "hit");
    missCounter = counter(meterRegistry, "miss");
    fallbackCounter = counter(meterRegistry, "fallback");
  }

  @Override
  public Optional<List<TrackingTimelineItem>> get(String trackingNumber) {
    String key = key(trackingNumber);
    try {
      String value = redis.opsForValue().get(key);
      if (value == null) {
        missCounter.increment();
        return Optional.empty();
      }
      JavaType type =
          objectMapper
              .getTypeFactory()
              .constructCollectionType(List.class, TrackingTimelineItem.class);
      List<TrackingTimelineItem> timeline = objectMapper.readValue(value, type);
      hitCounter.increment();
      return Optional.of(List.copyOf(timeline));
    } catch (Exception exception) {
      fallbackCounter.increment();
      safeDelete(key);
      LOGGER.warn("Tracking cache read failed errorType={}", exception.getClass().getSimpleName());
      return Optional.empty();
    }
  }

  @Override
  public void put(String trackingNumber, List<TrackingTimelineItem> timeline) {
    try {
      String value = objectMapper.writeValueAsString(List.copyOf(timeline));
      redis.opsForValue().set(key(trackingNumber), value, properties.timeToLive());
    } catch (Exception exception) {
      fallbackCounter.increment();
      LOGGER.warn("Tracking cache write failed errorType={}", exception.getClass().getSimpleName());
    }
  }

  @Override
  public void evict(String trackingNumber) {
    try {
      redis.delete(key(trackingNumber));
    } catch (RuntimeException exception) {
      fallbackCounter.increment();
      LOGGER.warn(
          "Tracking cache eviction failed errorType={}", exception.getClass().getSimpleName());
    }
  }

  private void safeDelete(String key) {
    try {
      redis.delete(key);
    } catch (RuntimeException ignored) {
      LOGGER.debug("Corrupt tracking cache key could not be deleted");
    }
  }

  private String key(String trackingNumber) {
    return properties.keyPrefix() + trackingNumber;
  }

  private static Counter counter(MeterRegistry registry, String result) {
    return Counter.builder("urbanflow.tracking.cache.operations")
        .tag("result", result)
        .register(registry);
  }
}
