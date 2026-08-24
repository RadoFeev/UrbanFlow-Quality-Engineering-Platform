package io.urbanflow.identity.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict optimistic-version entity tag codec. */
public final class EntityTagVersion {

  private static final Pattern FORMAT = Pattern.compile("\"v([0-9]+)\"");

  private EntityTagVersion() {}

  /** Encodes a nonnegative aggregate version. */
  public static String format(long version) {
    if (version < 0) {
      throw new IllegalArgumentException("version must not be negative");
    }
    return "\"v" + version + "\"";
  }

  /** Parses a strong `If-Match` entity tag. */
  public static long parse(String value) {
    Matcher matcher = value == null ? null : FORMAT.matcher(value);
    if (matcher == null || !matcher.matches()) {
      throw new IllegalArgumentException("If-Match must use the format \"v<number>\"");
    }
    try {
      return Long.parseLong(matcher.group(1));
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(
          "If-Match version is outside the supported range", exception);
    }
  }
}
