package io.urbanflow.orchestrator;

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Parses strict `--name=value` orchestration options. */
public final class OrchestratorParser {

  private static final java.util.Set<String> SUPPORTED =
      java.util.Set.of(
          "environment",
          "suite",
          "browser",
          "headless",
          "parallelism",
          "tags",
          "retries",
          "remote-url",
          "video",
          "cleanup",
          "report-directory",
          "execute");

  /** Parses command-line arguments into a validated configuration. */
  public OrchestratorConfig parse(String... arguments) {
    Map<String, String> values = new LinkedHashMap<>();
    for (String argument : arguments) {
      if (!argument.startsWith("--") || !argument.contains("=")) {
        throw new IllegalArgumentException("arguments must use --name=value format");
      }
      String[] parts = argument.substring(2).split("=", 2);
      if (!SUPPORTED.contains(parts[0]) || values.put(parts[0], parts[1]) != null) {
        throw new IllegalArgumentException("unsupported or duplicate option: " + parts[0]);
      }
    }
    return new OrchestratorConfig(
        values.getOrDefault("environment", "local"),
        Suite.valueOf(values.getOrDefault("suite", "ALL").toUpperCase(Locale.ROOT)),
        values.getOrDefault("browser", "chrome"),
        bool(values, "headless", true),
        integer(values, "parallelism", 4),
        tags(values.getOrDefault("tags", "")),
        integer(values, "retries", 0),
        values.containsKey("remote-url") ? URI.create(values.get("remote-url")) : null,
        bool(values, "video", false),
        bool(values, "cleanup", true),
        Path.of(values.getOrDefault("report-directory", "target/unified-report")),
        bool(values, "execute", false));
  }

  private static boolean bool(Map<String, String> values, String key, boolean fallback) {
    String value = values.getOrDefault(key, Boolean.toString(fallback));
    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
      throw new IllegalArgumentException(key + " must be true or false");
    }
    return Boolean.parseBoolean(value);
  }

  private static int integer(Map<String, String> values, String key, int fallback) {
    try {
      return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback)));
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(key + " must be an integer", exception);
    }
  }

  private static List<String> tags(String value) {
    return value.isBlank()
        ? List.of()
        : Arrays.stream(value.split(",")).map(String::trim).filter(tag -> !tag.isBlank()).toList();
  }
}
