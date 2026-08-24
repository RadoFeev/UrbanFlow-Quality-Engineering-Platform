package io.urbanflow.testcore.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class IdentifierManagerTest {

  @Test
  void correlationIdIsStableUntilCleared() {
    CorrelationIdManager manager = new CorrelationIdManager();
    String first = manager.current();
    assertThat(manager.current()).isEqualTo(first);
    manager.clear();
    assertThat(manager.current()).isNotEqualTo(first);
  }

  @Test
  void acceptsValidatedCorrelationId() {
    CorrelationIdManager manager = new CorrelationIdManager();
    manager.set("run_2026.case-42");
    assertThat(manager.current()).isEqualTo("run_2026.case-42");
  }

  @Test
  void rejectsUnsafeCorrelationId() {
    CorrelationIdManager manager = new CorrelationIdManager();
    assertThatThrownBy(() -> manager.set("bad header\r\nvalue"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void traceIdIsStableAndValid() {
    TraceIdManager manager = new TraceIdManager();
    assertThat(manager.current()).matches("[0-9a-f]{32}").isEqualTo(manager.current());
  }

  @Test
  void threadLocalIdentifiersAreIndependent() throws Exception {
    CorrelationIdManager manager = new CorrelationIdManager();
    Set<String> identifiers = ConcurrentHashMap.newKeySet();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var tasks =
          IntStream.range(0, 20)
              .mapToObj(ignored -> executor.submit(() -> identifiers.add(manager.current())))
              .toList();
      for (var task : tasks) {
        task.get();
      }
    }

    assertThat(identifiers).hasSize(20).allMatch(value -> value.length() == 36);
  }
}
