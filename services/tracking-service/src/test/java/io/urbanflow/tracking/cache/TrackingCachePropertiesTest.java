package io.urbanflow.tracking.cache;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TrackingCachePropertiesTest {

  @Test
  void rejectsBlankKeyPrefix() {
    assertThatThrownBy(() -> new TrackingCacheProperties(" ", Duration.ofSeconds(30)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("keyPrefix");
  }

  @Test
  void rejectsNonPositiveTtl() {
    assertThatThrownBy(() -> new TrackingCacheProperties("tracking:", Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("timeToLive");
  }
}
