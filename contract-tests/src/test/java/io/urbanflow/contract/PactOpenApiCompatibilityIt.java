package io.urbanflow.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

@Tag("contract")
class PactOpenApiCompatibilityIt {

  private static final Map<String, String> PROVIDER_SPECS =
      Map.of(
          "shipment-service",
          "services/shipment-service/src/main/resources/openapi/shipment-service.yaml",
          "tracking-service",
          "services/tracking-service/src/main/resources/openapi/tracking-service.yaml",
          "notification-service",
          "services/notification-service/src/main/resources/openapi/notification-service.yaml",
          "warehouse-service",
          "services/warehouse-service/src/main/resources/openapi/warehouse-service.yaml");

  @Test
  void everyGeneratedInteractionExistsInProviderOpenApi() throws IOException {
    Path pactDirectory = Path.of(System.getProperty("pact.rootDir"));
    List<String> compatibilityErrors = new ArrayList<>();
    int interactionCount = 0;

    try (Stream<Path> files = Files.list(pactDirectory)) {
      for (Path pactFile : files.filter(path -> path.toString().endsWith(".json")).toList()) {
        interactionCount += verifyPact(pactFile, compatibilityErrors);
      }
    }

    assertThat(interactionCount).as("generated Pact interactions").isEqualTo(20);
    assertThat(compatibilityErrors).as("Pact/OpenAPI compatibility errors").isEmpty();
  }

  @SuppressWarnings("unchecked")
  private int verifyPact(Path pactFile, List<String> errors) throws IOException {
    Map<String, Object> pact;
    try (Reader reader = Files.newBufferedReader(pactFile)) {
      pact = new Yaml().load(reader);
    }
    String provider = (String) ((Map<String, Object>) pact.get("provider")).get("name");
    String specRelativePath = PROVIDER_SPECS.get(provider);
    assertThat(specRelativePath).as("known provider for %s", pactFile.getFileName()).isNotNull();
    Map<String, Object> openApi = readYaml(workspaceRoot().resolve(specRelativePath));
    Map<String, Object> paths = (Map<String, Object>) openApi.get("paths");
    List<Map<String, Object>> interactions = (List<Map<String, Object>>) pact.get("interactions");

    for (Map<String, Object> interaction : interactions) {
      Map<String, Object> request = (Map<String, Object>) interaction.get("request");
      Map<String, Object> response = (Map<String, Object>) interaction.get("response");
      String method = request.get("method").toString().toLowerCase();
      String concretePath = request.get("path").toString();
      String template = matchingTemplate(paths, concretePath);
      if (template == null) {
        errors.add(provider + " does not publish path " + concretePath);
        continue;
      }
      Map<String, Object> operation = (Map<String, Object>) paths.get(template);
      if (!operation.containsKey(method)) {
        errors.add(provider + " does not publish " + method.toUpperCase() + " " + template);
        continue;
      }
      Map<String, Object> operationDefinition = (Map<String, Object>) operation.get(method);
      Map<String, Object> responses = (Map<String, Object>) operationDefinition.get("responses");
      String status = response.get("status").toString();
      if (!responses.containsKey(status)) {
        errors.add(
            provider + " does not publish status " + status + " for " + method + " " + template);
      }
    }
    return interactions.size();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readYaml(Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path)) {
      return new Yaml().load(reader);
    }
  }

  private Path workspaceRoot() {
    return Path.of(System.getProperty("workspace.root"));
  }

  private String matchingTemplate(Map<String, Object> paths, String concretePath) {
    return paths.keySet().stream()
        .filter(
            template -> Pattern.matches(template.replaceAll("\\{[^/]+}", "[^/]+"), concretePath))
        .findFirst()
        .orElse(null);
  }
}
