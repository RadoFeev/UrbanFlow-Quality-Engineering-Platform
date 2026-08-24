package io.urbanflow.shipment.api;

/** Parses strong or weak HTTP entity tags into optimistic-lock version numbers. */
final class EntityTagVersion {

  private EntityTagVersion() {}

  static long parse(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("If-Match header must not be blank");
    }
    String normalized = value.trim();
    if (normalized.startsWith("W/")) {
      normalized = normalized.substring(2).trim();
    }
    if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
      normalized = normalized.substring(1, normalized.length() - 1);
    }
    try {
      long version = Long.parseLong(normalized);
      if (version < 0) {
        throw new IllegalArgumentException("If-Match version must not be negative");
      }
      return version;
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(
          "If-Match header must contain a numeric version", exception);
    }
  }
}
