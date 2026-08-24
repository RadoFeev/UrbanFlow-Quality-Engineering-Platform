package io.urbanflow.orchestrator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds shell-independent Maven Wrapper commands from validated execution plans. */
public final class MavenCommandBuilder {

  /** Builds a deterministic argument list suitable for ProcessBuilder. */
  public List<String> build(OrchestratorConfig config) {
    return build(config, Path.of(""));
  }

  /** Builds an argument list with a workspace-qualified Maven Wrapper executable. */
  public List<String> build(OrchestratorConfig config, Path workspace) {
    List<String> command = new ArrayList<>();
    Path root = workspace.toAbsolutePath().normalize();
    if (isWindows()) {
      command.add("cmd.exe");
      command.add("/d");
      command.add("/c");
      command.add(root.resolve("mvnw.cmd").toString());
    } else {
      command.add(root.resolve("mvnw").toString());
    }
    command.add("clean");
    command.add("verify");
    module(config.suite())
        .ifPresent(
            module -> {
              command.add("-pl");
              command.add(module);
              command.add("-am");
            });
    command.add("-Denvironment=" + config.environment());
    command.add("-Dbrowser=" + config.browser());
    command.add("-Dheadless=" + config.headless());
    command.add("-Dparallelism=" + config.parallelism());
    command.add("-Dretry.count=" + config.retries());
    command.add("-Dvideo.enabled=" + config.video());
    command.add("-Dcleanup.enabled=" + config.cleanup());
    command.add("-Dallure.results.directory=" + config.reportDirectory().resolve("allure-results"));
    String groups = groups(config);
    if (!groups.isBlank()) {
      command.add("-Dgroups=" + groups);
    }
    if (config.remoteUrl() != null) {
      command.add("-Dremote.url=" + config.remoteUrl());
    }
    return List.copyOf(command);
  }

  private static String groups(OrchestratorConfig config) {
    List<String> groups = new ArrayList<>();
    if (config.suite() != Suite.ALL) {
      groups.add(config.suite().name().toLowerCase(Locale.ROOT));
    }
    groups.addAll(config.tags());
    return String.join(",", groups);
  }

  private static java.util.Optional<String> module(Suite suite) {
    return switch (suite) {
      case API -> java.util.Optional.of(":api-tests");
      case UI -> java.util.Optional.of(":ui-tests");
      case CONTRACT -> java.util.Optional.of(":contract-tests");
      case COMPONENT -> java.util.Optional.of(":component-tests");
      case INTEGRATION -> java.util.Optional.of(":integration-tests");
      case EVENT -> java.util.Optional.of(":event-tests");
      case DATABASE -> java.util.Optional.of(":database-tests");
      case E2E -> java.util.Optional.of(":e2e-tests");
      case ACCESSIBILITY -> java.util.Optional.of(":accessibility-tests");
      case VISUAL -> java.util.Optional.of(":visual-tests");
      case PERFORMANCE -> java.util.Optional.of(":performance-tests");
      case SECURITY -> java.util.Optional.of(":security-tests");
      case ALL, SMOKE, REGRESSION -> java.util.Optional.empty();
    };
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
  }
}
