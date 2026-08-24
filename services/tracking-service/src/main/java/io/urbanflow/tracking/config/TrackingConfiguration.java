package io.urbanflow.tracking.config;

import io.urbanflow.tracking.cache.TrackingCacheProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides deterministic tracking collaborators and typed cache configuration. */
@Configuration
@EnableConfigurationProperties(TrackingCacheProperties.class)
public class TrackingConfiguration {

  /** Provides the clock used for projection receipt timestamps. */
  @Bean
  Clock trackingClock() {
    return Clock.systemUTC();
  }
}
