package io.urbanflow.orchestrator;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Immutable command-line test execution plan. */
public record OrchestratorConfig(
    String environment,
    Suite suite,
    String browser,
    boolean headless,
    int parallelism,
    List<String> tags,
    int retries,
    URI remoteUrl,
    boolean video,
    boolean cleanup,
    Path reportDirectory,
    boolean execute) {

  /** Validates safe orchestration bounds and copies tags. */
  public OrchestratorConfig {
    if (environment == null || !environment.matches("[a-z0-9-]{2,30}")) {
      throw new IllegalArgumentException("environment format is invalid");
    }
    if (suite == null) {
      throw new IllegalArgumentException("suite is required");
    }
    browser = browser == null ? "" : browser.toLowerCase(Locale.ROOT);
    if (!java.util.Set.of("chrome", "firefox", "edge").contains(browser)) {
      throw new IllegalArgumentException("unsupported browser");
    }
    if (parallelism < 1 || parallelism > 64) {
      throw new IllegalArgumentException("parallelism must be between 1 and 64");
    }
    if (retries < 0 || retries > 2) {
      throw new IllegalArgumentException("retries must be between 0 and 2");
    }
    tags = List.copyOf(tags);
    if (tags.stream().anyMatch(tag -> !tag.matches("[a-z][a-z0-9-]{1,30}"))) {
      throw new IllegalArgumentException("tag format is invalid");
    }
    if (remoteUrl != null && !remoteUrl.isAbsolute()) {
      throw new IllegalArgumentException("remoteUrl must be absolute");
    }
    reportDirectory = reportDirectory.toAbsolutePath().normalize();
  }
}
