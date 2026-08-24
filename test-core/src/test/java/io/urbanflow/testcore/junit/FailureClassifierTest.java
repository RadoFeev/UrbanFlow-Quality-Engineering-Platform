package io.urbanflow.testcore.junit;

import static org.assertj.core.api.Assertions.assertThat;

import io.urbanflow.testcore.resilience.InfrastructureFailure;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FailureClassifierTest {

  @ParameterizedTest
  @MethodSource("failures")
  void classifiesFailureTaxonomy(Throwable failure, FailureCategory expected) {
    assertThat(new FailureClassifier().classify(failure)).isEqualTo(expected);
  }

  static Stream<Arguments> failures() {
    return Stream.of(
        Arguments.of(new InfrastructureFailure("grid"), FailureCategory.INFRASTRUCTURE),
        Arguments.of(
            new RuntimeException(new ConnectException("refused")), FailureCategory.INFRASTRUCTURE),
        Arguments.of(new TimeoutException("late"), FailureCategory.TIMEOUT),
        Arguments.of(new AssertionError("mismatch"), FailureCategory.ASSERTION),
        Arguments.of(new IllegalStateException("domain"), FailureCategory.PRODUCT_DEFECT),
        Arguments.of(new Exception("unknown"), FailureCategory.UNKNOWN));
  }
}
