package io.urbanflow.testcore.config;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Resolves immutable test configuration with property, environment, and default precedence. */
public final class ConfigurationManager {

  private final Map<String, String> environment;
  private final Properties properties;

  /** Creates a resolver backed by supplied sources for deterministic tests. */
  public ConfigurationManager(Map<String, String> environment, Properties properties) {
    this.environment = Map.copyOf(environment);
    this.properties = new Properties();
    this.properties.putAll(Objects.requireNonNull(properties, "properties must not be null"));
  }

  /** Creates a resolver backed by process environment and JVM properties. */
  public static ConfigurationManager system() {
    return new ConfigurationManager(System.getenv(), System.getProperties());
  }

  /** Resolves all supported execution values into one immutable record. */
  public EnvironmentConfig resolve() {
    return new EnvironmentConfig(
        value("environment", "TEST_ENVIRONMENT", "local"),
        uri("customer.url", "CUSTOMER_PORTAL_URL", "http://localhost:3001"),
        uri("courier.url", "COURIER_PORTAL_URL", "http://localhost:3002"),
        uri("operations.url", "OPERATIONS_DASHBOARD_URL", "http://localhost:3003"),
        uri("identity.url", "IDENTITY_SERVICE_URL", "http://localhost:8081"),
        uri("shipment.url", "SHIPMENT_SERVICE_URL", "http://localhost:8082"),
        uri("tracking.url", "TRACKING_SERVICE_URL", "http://localhost:8083"),
        uri("routing.url", "ROUTING_SERVICE_URL", "http://localhost:8084"),
        uri("notification.url", "NOTIFICATION_SERVICE_URL", "http://localhost:8085"),
        uri("warehouse.url", "WAREHOUSE_SERVICE_URL", "http://localhost:8086"),
        value("browser", "BROWSER", "chrome"),
        bool("headless", "HEADLESS", true),
        optionalUri("remote.url", "REMOTE_URL"),
        integer("parallelism", "PARALLELISM", 4),
        Duration.parse(value("timeout", "TEST_TIMEOUT", "PT20S")),
        bool("video", "VIDEO_ENABLED", false),
        bool("cleanup", "CLEANUP_ENABLED", true));
  }

  private URI uri(String property, String variable, String fallback) {
    return URI.create(value(property, variable, fallback));
  }

  private URI optionalUri(String property, String variable) {
    String resolved = value(property, variable, "");
    return resolved.isBlank() ? null : URI.create(resolved);
  }

  private boolean bool(String property, String variable, boolean fallback) {
    String resolved = value(property, variable, Boolean.toString(fallback));
    if (!resolved.equalsIgnoreCase("true") && !resolved.equalsIgnoreCase("false")) {
      throw new IllegalArgumentException(property + " must be true or false");
    }
    return Boolean.parseBoolean(resolved);
  }

  private int integer(String property, String variable, int fallback) {
    try {
      return Integer.parseInt(value(property, variable, Integer.toString(fallback)));
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(property + " must be an integer", exception);
    }
  }

  private String value(String property, String variable, String fallback) {
    String fromProperty = properties.getProperty(property);
    if (fromProperty != null) {
      return fromProperty.trim();
    }
    return environment.getOrDefault(variable, fallback).trim();
  }
}
