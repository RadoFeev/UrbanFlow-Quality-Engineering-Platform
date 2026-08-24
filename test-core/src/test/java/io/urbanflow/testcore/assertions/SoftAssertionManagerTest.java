package io.urbanflow.testcore.assertions;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SoftAssertionManagerTest {

  @Test
  void assertsAndClearsSuccessfulCollector() {
    SoftAssertionManager manager = new SoftAssertionManager();
    manager.current().assertThat(2 + 2).isEqualTo(4);
    assertThatCode(manager::assertAllAndClear).doesNotThrowAnyException();
  }

  @Test
  void reportsAllCollectedFailures() {
    SoftAssertionManager manager = new SoftAssertionManager();
    manager.current().assertThat("actual").as("first").isEqualTo("expected");
    manager.current().assertThat(3).as("second").isGreaterThan(5);
    assertThatThrownBy(manager::assertAllAndClear)
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("first")
        .hasMessageContaining("second");
  }

  @Test
  void clearDiscardsAbortedCollector() {
    SoftAssertionManager manager = new SoftAssertionManager();
    manager.current().fail("aborted");
    manager.clear();
    assertThatCode(manager::assertAllAndClear).doesNotThrowAnyException();
  }
}
