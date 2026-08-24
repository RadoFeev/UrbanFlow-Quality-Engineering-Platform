package io.urbanflow.testdata.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GeneratorTest {

  @Test
  void deterministicGeneratorsReplaySameSequence() {
    DeterministicRandom first = new DeterministicRandom(42);
    DeterministicRandom second = new DeterministicRandom(42);
    assertThat(IntStream.range(0, 10).map(ignored -> first.integer(1, 100)).toArray())
        .containsExactly(IntStream.range(0, 10).map(ignored -> second.integer(1, 100)).toArray());
  }

  @Test
  void deterministicAlphaNumericHasRequestedShape() {
    assertThat(new DeterministicRandom(7).alphanumeric(32)).hasSize(32).matches("[a-z0-9]{32}");
  }

  @Test
  void rejectsUnsafeRandomLength() {
    assertThatThrownBy(() -> new DeterministicRandom(1).alphanumeric(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void emailGeneratorIsUniqueUnderParallelLoad() {
    UniqueEmailGenerator generator = new UniqueEmailGenerator("run-42");
    Set<String> values = ConcurrentHashMap.newKeySet();
    IntStream.range(0, 500).parallel().forEach(ignored -> values.add(generator.next("Customer")));
    assertThat(values)
        .hasSize(500)
        .allMatch(value -> value.matches("customer\\+run-42-\\d+@example\\.test"));
  }

  @Test
  void emailGeneratorSanitizesPurpose() {
    assertThat(new UniqueEmailGenerator("run-42").next("Role Admin!"))
        .startsWith("roleadmin+run-42-");
  }

  @Test
  void rejectsUnsafeRunIdentifier() {
    assertThatThrownBy(() -> new UniqueEmailGenerator("BAD RUN"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void trackingGeneratorIsDeterministicAndUnique() {
    UniqueTrackingNumberGenerator generator = new UniqueTrackingNumberGenerator("a1b2");
    assertThat(generator.next()).isEqualTo("UF-A1B20001");
    assertThat(generator.next()).isEqualTo("UF-A1B20002");
  }

  @Test
  void rejectsInvalidTrackingRunCode() {
    assertThatThrownBy(() -> new UniqueTrackingNumberGenerator("abc"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
