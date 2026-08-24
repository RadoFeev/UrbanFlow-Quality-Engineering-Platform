package io.urbanflow.routing.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Routing event topic and broker acknowledgment timeout. */
@ConfigurationProperties("urbanflow.routing.outbox")
public record RoutingOutboxProperties(String topic, Duration acknowledgmentTimeout) {

  /** Validates bounded publication configuration. */
  public RoutingOutboxProperties {
    if (topic == null || topic.isBlank()) {
      throw new IllegalArgumentException("topic must not be blank");
    }
    if (acknowledgmentTimeout == null
        || acknowledgmentTimeout.isZero()
        || acknowledgmentTimeout.isNegative()
        || acknowledgmentTimeout.compareTo(Duration.ofSeconds(30)) > 0) {
      throw new IllegalArgumentException("acknowledgmentTimeout must be within 30 seconds");
    }
  }
}
