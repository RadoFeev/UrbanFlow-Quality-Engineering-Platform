package io.urbanflow.notification.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Delivery gateway, retry, and timeout policy. */
@ConfigurationProperties("urbanflow.notification")
public record NotificationProperties(URI gatewayUrl, Duration gatewayTimeout, int maximumAttempts) {

  /** Validates safe bounded delivery configuration. */
  public NotificationProperties {
    if (gatewayUrl == null || !"https".equalsIgnoreCase(gatewayUrl.getScheme())) {
      throw new IllegalArgumentException("gatewayUrl must use HTTPS");
    }
    if (gatewayTimeout == null
        || gatewayTimeout.isZero()
        || gatewayTimeout.isNegative()
        || gatewayTimeout.compareTo(Duration.ofSeconds(30)) > 0) {
      throw new IllegalArgumentException("gatewayTimeout must be within 30 seconds");
    }
    if (maximumAttempts < 1 || maximumAttempts > 20) {
      throw new IllegalArgumentException("maximumAttempts must be between 1 and 20");
    }
  }
}
