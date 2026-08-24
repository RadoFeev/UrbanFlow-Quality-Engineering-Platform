package io.urbanflow.testcore.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ScenarioContextTest {

  @Test
  void storesAndReturnsTypedValues() {
    ScenarioContext context = new ScenarioContext();
    ContextKey<String> key = new ContextKey<>("tracking", String.class);
    context.put(key, "UF-12345678");
    assertThat(context.required(key)).isEqualTo("UF-12345678");
    assertThat(context.find(key)).contains("UF-12345678");
  }

  @Test
  void missingRequiredValueHasDiagnosticName() {
    ScenarioContext context = new ScenarioContext();
    assertThatThrownBy(() -> context.required(new ContextKey<>("shipment", String.class)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("shipment");
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void rejectsValueWithWrongRuntimeType() {
    ScenarioContext context = new ScenarioContext();
    ContextKey raw = new ContextKey("count", Integer.class);
    assertThatThrownBy(() -> context.put(raw, "not an integer"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void clearRemovesAllScenarioState() {
    ScenarioContext context = new ScenarioContext();
    context.put(new ContextKey<>("one", String.class), "value");
    context.clear();
    assertThat(context.size()).isZero();
  }

  @Test
  void rejectsInvalidContextKeys() {
    assertThatThrownBy(() -> new ContextKey<>(" ", String.class))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ContextKey<>("valid", null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
