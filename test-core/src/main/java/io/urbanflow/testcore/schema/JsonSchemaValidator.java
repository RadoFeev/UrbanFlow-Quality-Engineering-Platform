package io.urbanflow.testcore.schema;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;

/** Compiles Draft 2020-12 JSON schemas and reports every validation failure. */
public final class JsonSchemaValidator {

  private final SchemaRegistry registry =
      SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

  /** Validates JSON content and throws one diagnostic error when it is incompatible. */
  public void validate(String schemaContent, String jsonContent) {
    if (schemaContent == null || schemaContent.isBlank() || jsonContent == null) {
      throw new IllegalArgumentException("schema and JSON content are required");
    }
    Schema schema = registry.getSchema(schemaContent);
    List<com.networknt.schema.Error> errors = schema.validate(jsonContent, InputFormat.JSON);
    if (!errors.isEmpty()) {
      throw new AssertionError(
          "JSON schema validation failed: "
              + errors.stream().map(Object::toString).sorted().toList());
    }
  }
}
