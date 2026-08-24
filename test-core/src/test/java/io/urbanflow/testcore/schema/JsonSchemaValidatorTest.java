package io.urbanflow.testcore.schema;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JsonSchemaValidatorTest {

  private static final String SCHEMA =
      """
      {"$schema":"https://json-schema.org/draft/2020-12/schema",
       "type":"object","additionalProperties":false,"required":["id"],
       "properties":{"id":{"type":"string","format":"uuid"}}}
      """;

  @Test
  void acceptsCompatibleJson() {
    assertThatCode(
            () ->
                new JsonSchemaValidator()
                    .validate(SCHEMA, "{\"id\":\"5e8b5967-504d-4bd5-bcff-72f71d9b2b5e\"}"))
        .doesNotThrowAnyException();
  }

  @Test
  void reportsAllIncompatibleJsonPaths() {
    assertThatThrownBy(() -> new JsonSchemaValidator().validate(SCHEMA, "{\"extra\":true}"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("JSON schema validation failed")
        .hasMessageContaining("id");
  }

  @Test
  void rejectsBlankSchema() {
    assertThatThrownBy(() -> new JsonSchemaValidator().validate("", "{}"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
