package io.urbanflow.api.contract;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

final class OpenApiCatalogue {

  private static final List<String> SPECIFICATIONS =
      List.of(
          "identity-service.yaml",
          "shipment-service.yaml",
          "tracking-service.yaml",
          "routing-service.yaml",
          "notification-service.yaml",
          "warehouse-service.yaml");
  private static final java.util.Set<String> METHODS =
      java.util.Set.of("get", "post", "put", "patch", "delete");

  private OpenApiCatalogue() {}

  static List<Operation> operations() {
    List<Operation> operations = new ArrayList<>();
    for (String specification : SPECIFICATIONS) {
      Map<String, Object> document = load(specification);
      map(document.get("paths"))
          .forEach(
              (path, pathValue) ->
                  map(pathValue)
                      .forEach(
                          (method, operationValue) -> {
                            if (METHODS.contains(method)) {
                              operations.add(
                                  new Operation(specification, path, method, map(operationValue)));
                            }
                          }));
    }
    return List.copyOf(operations);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> load(String specification) {
    try (InputStream stream =
        OpenApiCatalogue.class.getResourceAsStream("/openapi/" + specification)) {
      if (stream == null) {
        throw new IllegalStateException("OpenAPI resource is missing: " + specification);
      }
      return new Yaml().load(stream);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> map(Object value) {
    return (Map<String, Object>) value;
  }

  record Operation(String specification, String path, String method, Map<String, Object> contract) {

    String displayName() {
      return specification + " " + method.toUpperCase(java.util.Locale.ROOT) + " " + path;
    }

    String operationId() {
      return String.valueOf(contract.getOrDefault("operationId", ""));
    }

    Map<String, Object> responses() {
      return map(contract.get("responses"));
    }
  }
}
