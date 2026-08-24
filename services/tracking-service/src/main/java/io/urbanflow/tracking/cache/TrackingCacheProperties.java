package io.urbanflow.tracking.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Immutable Redis cache settings for public tracking timelines. */
@ConfigurationProperties("urbanflow.tracking.cache")
public record TrackingCacheProperties(
    @DefaultValue("tracking:timeline:") String keyPrefix,
    @DefaultValue("30s") Duration timeToLive) {

  /** Validates bounded cache keys and positive expiration. */
  public TrackingCacheProperties {
    if (keyPrefix == null || keyPrefix.isBlank()) {
      throw new IllegalArgumentException("keyPrefix must not be blank");
    }
    keyPrefix = keyPrefix.trim();
    if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()) {
      throw new IllegalArgumentException("timeToLive must be positive");
    }
  }
}
