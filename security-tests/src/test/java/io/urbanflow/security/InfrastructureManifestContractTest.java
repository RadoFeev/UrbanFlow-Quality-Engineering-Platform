package io.urbanflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.ObjectMapper;

/** Validates delivery manifests with parsers and required production resource inventories. */
@Tag("infrastructure")
class InfrastructureManifestContractTest {

  @ParameterizedTest(name = "YAML parses: {0}")
  @MethodSource("yamlFiles")
  void yamlDocumentParses(Path path) throws IOException {
    try (InputStream input = Files.newInputStream(path)) {
      List<Object> documents = new ArrayList<>();
      new Yaml().loadAll(input).forEach(documents::add);
      assertThat(documents).as("YAML documents in %s", path).isNotEmpty().doesNotContainNull();
    }
  }

  @Test
  void grafanaDashboardIsValidJson() throws IOException {
    Path dashboard = workspace().resolve("observability/grafana/dashboards/urbanflow-quality.json");

    Map<?, ?> model = new ObjectMapper().readValue(dashboard.toFile(), Map.class);

    assertThat(model.get("uid")).isEqualTo("urbanflow-quality");
    assertThat((List<?>) model.get("panels")).hasSizeGreaterThanOrEqualTo(8);
  }

  @Test
  void requestedWorkflowInventoryIsComplete() throws IOException {
    Set<String> expected =
        Set.of(
            "pull-request-checks.yml",
            "smoke-tests.yml",
            "api-tests.yml",
            "ui-tests.yml",
            "contract-tests.yml",
            "integration-tests.yml",
            "e2e-tests.yml",
            "accessibility-tests.yml",
            "visual-tests.yml",
            "performance-tests.yml",
            "security-scan.yml",
            "nightly-regression.yml",
            "dependency-update.yml",
            "release.yml",
            "publish-allure.yml");
    Path workflows = workspace().resolve(".github/workflows");
    Set<String> actual = new HashSet<>();
    try (Stream<Path> files = Files.list(workflows)) {
      files
          .filter(path -> !path.getFileName().toString().startsWith("_"))
          .map(path -> path.getFileName().toString())
          .forEach(actual::add);
    }

    assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
  }

  @Test
  void kubernetesInventoryContainsRequiredWorkloads() throws IOException {
    Set<String> kinds = new HashSet<>();
    for (Path path : kubernetesFiles()) {
      try (InputStream input = Files.newInputStream(path)) {
        for (Object document : new Yaml().loadAll(input)) {
          if (document instanceof Map<?, ?> resource && resource.get("kind") != null) {
            kinds.add(resource.get("kind").toString());
          }
        }
      }
    }

    assertThat(kinds)
        .contains(
            "Namespace",
            "ConfigMap",
            "Secret",
            "PersistentVolumeClaim",
            "Deployment",
            "Service",
            "HorizontalPodAutoscaler",
            "Job",
            "CronJob");
  }

  @Test
  void helmChartTemplatesEveryRequiredResource() throws IOException {
    Path chart = workspace().resolve("helm/urbanflow-test-infrastructure");
    String templates;
    try (Stream<Path> files = Files.walk(chart.resolve("templates"))) {
      templates =
          files
              .filter(path -> path.getFileName().toString().endsWith(".yaml"))
              .map(InfrastructureManifestContractTest::read)
              .reduce("", (left, right) -> left + System.lineSeparator() + right);
    }

    assertThat(templates)
        .contains(
            "kind: Namespace",
            "kind: ConfigMap",
            "kind: Secret",
            "kind: PersistentVolumeClaim",
            "kind: Deployment",
            "kind: Service",
            "kind: HorizontalPodAutoscaler",
            "kind: Job",
            "kind: CronJob",
            "readinessProbe:",
            "livenessProbe:",
            "resources:");
  }

  static Stream<Path> yamlFiles() throws IOException {
    List<Path> roots =
        List.of(
            workspace().resolve(".github/workflows"),
            workspace().resolve("infrastructure"),
            workspace().resolve("kubernetes"),
            workspace().resolve("observability"));
    Stream.Builder<Path> result = Stream.builder();
    for (Path root : roots) {
      try (Stream<Path> files = Files.walk(root)) {
        files
            .filter(Files::isRegularFile)
            .filter(InfrastructureManifestContractTest::isYaml)
            .forEach(result::add);
      }
    }
    result.add(workspace().resolve("helm/urbanflow-test-infrastructure/Chart.yaml"));
    result.add(workspace().resolve("helm/urbanflow-test-infrastructure/values.yaml"));
    result.add(workspace().resolve("helm/urbanflow-test-infrastructure/values-minimal.yaml"));
    return result.build();
  }

  private static List<Path> kubernetesFiles() throws IOException {
    try (Stream<Path> files = Files.list(workspace().resolve("kubernetes"))) {
      return files.filter(InfrastructureManifestContractTest::isYaml).toList();
    }
  }

  private static boolean isYaml(Path path) {
    String name = path.getFileName().toString();
    return name.endsWith(".yml") || name.endsWith(".yaml");
  }

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("could not read " + path, exception);
    }
  }

  private static Path workspace() {
    return Path.of(System.getProperty("workspace.root")).toAbsolutePath().normalize();
  }
}
