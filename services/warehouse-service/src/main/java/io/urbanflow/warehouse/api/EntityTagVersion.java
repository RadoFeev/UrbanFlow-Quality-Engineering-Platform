package io.urbanflow.warehouse.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strong entity-tag codec for optimistic versions. */
final class EntityTagVersion {

  private static final Pattern VERSION = Pattern.compile("\\\"v(\\d+)\\\"");

  private EntityTagVersion() {}

  static long parse(String value) {
    Matcher matcher = VERSION.matcher(value == null ? "" : value.trim());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("If-Match must be a strong version tag such as \"v2\"");
    }
    try {
      return Long.parseLong(matcher.group(1));
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("If-Match version is too large", exception);
    }
  }

  static String format(long version) {
    return "\"v" + version + "\"";
  }
}
