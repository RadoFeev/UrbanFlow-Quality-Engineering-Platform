package io.urbanflow.orchestrator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;

/** Writes unified Allure environment and executor metadata within the workspace. */
public final class UnifiedReportWriter {

  private final Clock clock;

  /** Creates the report writer. */
  public UnifiedReportWriter(Clock clock) {
    this.clock = clock;
  }

  /** Writes deterministic execution metadata and returns the Allure results directory. */
  public Path write(Path workspace, OrchestratorConfig config) {
    Path root = workspace.toAbsolutePath().normalize();
    Path report = config.reportDirectory().toAbsolutePath().normalize();
    if (!report.startsWith(root)) {
      throw new IllegalArgumentException("report directory must stay within the workspace");
    }
    Path results = report.resolve("allure-results");
    try {
      Files.createDirectories(results);
      String environment =
          "Environment="
              + config.environment()
              + System.lineSeparator()
              + "Suite="
              + config.suite()
              + System.lineSeparator()
              + "Browser="
              + config.browser()
              + System.lineSeparator()
              + "Headless="
              + config.headless()
              + System.lineSeparator()
              + "Parallelism="
              + config.parallelism()
              + System.lineSeparator();
      write(results.resolve("environment.properties"), environment);
      String executor =
          """
          {
            "name" : "UrbanFlow Test Orchestrator",
            "type" : "local-or-ci",
            "buildName" : "%s on %s",
            "startedAt" : "%s"
          }
          """
              .formatted(config.suite(), config.environment(), clock.instant());
      write(results.resolve("executor.json"), executor);
      write(results.resolve("categories.json"), categories());
      return results;
    } catch (IOException exception) {
      throw new ReportWriteException("unified report metadata could not be written", exception);
    }
  }

  private static void write(Path path, String content) throws IOException {
    Files.writeString(
        path,
        content,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  private static String categories() {
    String value =
        """
        [ {
          "name" : "Infrastructure failure",
          "matchedStatuses" : [ "failed", "broken" ],
          "messageRegex" : ".*(connection|container|browser|grid|broker|network).*"
        }, {
          "name" : "Timeout",
          "matchedStatuses" : [ "failed", "broken" ],
          "messageRegex" : ".*(timed out|timeout|condition was not fulfilled).*"
        }, {
          "name" : "Assertion failure",
          "matchedStatuses" : [ "failed" ],
          "traceRegex" : ".*(AssertionError|AssertionFailedError).*"
        }, {
          "name" : "Product defect",
          "matchedStatuses" : [ "failed" ]
        } ]
        """;
    return value;
  }

  /** Normalizes report filesystem failures. */
  public static class ReportWriteException extends RuntimeException {

    /** Creates a report failure. */
    public ReportWriteException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
