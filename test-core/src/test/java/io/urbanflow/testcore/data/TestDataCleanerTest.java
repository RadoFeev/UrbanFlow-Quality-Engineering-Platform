package io.urbanflow.testcore.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestDataCleanerTest {

  @Test
  void cleansInReverseCreationOrder() {
    TestDataCleaner cleaner = new TestDataCleaner();
    List<String> calls = new ArrayList<>();
    cleaner.register("first", () -> calls.add("first"));
    cleaner.register("second", () -> calls.add("second"));

    cleaner.close();

    assertThat(calls).containsExactly("second", "first");
    assertThat(cleaner.pending()).isZero();
  }

  @Test
  void continuesAfterFailureAndAggregatesCauses() {
    TestDataCleaner cleaner = new TestDataCleaner();
    List<String> calls = new ArrayList<>();
    cleaner.register("later", () -> calls.add("later"));
    cleaner.register(
        "broken",
        () -> {
          throw new IllegalStateException("boom");
        });

    assertThatThrownBy(cleaner::close)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("broken")
        .satisfies(error -> assertThat(error.getSuppressed()).hasSize(1));
    assertThat(calls).containsExactly("later");
  }

  @Test
  void rejectsUnnamedCleanup() {
    assertThatThrownBy(() -> new TestDataCleaner().register("", () -> {}))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
