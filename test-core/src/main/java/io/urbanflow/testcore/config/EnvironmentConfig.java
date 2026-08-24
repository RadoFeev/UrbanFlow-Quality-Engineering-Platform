package io.urbanflow.testcore.config;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/** Immutable execution environment resolved once per test run. */
public record EnvironmentConfig(
    String environment,
    URI customerPortalUrl,
    URI courierPortalUrl,
    URI operationsDashboardUrl,
    URI identityServiceUrl,
    URI shipmentServiceUrl,
    URI trackingServiceUrl,
    URI routingServiceUrl,
    URI notificationServiceUrl,
    URI warehouseServiceUrl,
    String browser,
    boolean headless,
    URI remoteUrl,
    int parallelism,
    Duration timeout,
    boolean videoEnabled,
    boolean cleanupEnabled) {

  /** Validates bounded and absolute configuration values. */
  public EnvironmentConfig {
    environment = requireText(environment, "environment");
    customerPortalUrl = requireAbsolute(customerPortalUrl, "customerPortalUrl");
    courierPortalUrl = requireAbsolute(courierPortalUrl, "courierPortalUrl");
    operationsDashboardUrl = requireAbsolute(operationsDashboardUrl, "operationsDashboardUrl");
    identityServiceUrl = requireAbsolute(identityServiceUrl, "identityServiceUrl");
    shipmentServiceUrl = requireAbsolute(shipmentServiceUrl, "shipmentServiceUrl");
    trackingServiceUrl = requireAbsolute(trackingServiceUrl, "trackingServiceUrl");
    routingServiceUrl = requireAbsolute(routingServiceUrl, "routingServiceUrl");
    notificationServiceUrl = requireAbsolute(notificationServiceUrl, "notificationServiceUrl");
    warehouseServiceUrl = requireAbsolute(warehouseServiceUrl, "warehouseServiceUrl");
    browser = requireText(browser, "browser").toLowerCase(java.util.Locale.ROOT);
    if (!java.util.Set.of("chrome", "firefox", "edge").contains(browser)) {
      throw new IllegalArgumentException("browser must be chrome, firefox, or edge");
    }
    if (remoteUrl != null) {
      remoteUrl = requireAbsolute(remoteUrl, "remoteUrl");
    }
    if (parallelism < 1 || parallelism > 64) {
      throw new IllegalArgumentException("parallelism must be between 1 and 64");
    }
    Objects.requireNonNull(timeout, "timeout must not be null");
    if (timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofMinutes(5)) > 0) {
      throw new IllegalArgumentException("timeout must be between PT0S and PT5M");
    }
  }

  private static URI requireAbsolute(URI value, String field) {
    if (value == null || !value.isAbsolute()) {
      throw new IllegalArgumentException(field + " must be an absolute URI");
    }
    return value;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }
}
