package io.urbanflow.routing.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RoutingOutboxPropertiesTest {

  @Test
  void acceptsBoundedConfiguration() {
    RoutingOutboxProperties properties =
        new RoutingOutboxProperties("urbanflow.routing.events.v1", Duration.ofSeconds(10));
    assertThat(properties.topic()).isEqualTo("urbanflow.routing.events.v1");
  }

  @Test
  void rejectsBlankTopic() {
    assertThatThrownBy(() -> new RoutingOutboxProperties(" ", Duration.ofSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonPositiveAndExcessiveTimeout() {
    assertThatThrownBy(() -> new RoutingOutboxProperties("topic", Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RoutingOutboxProperties("topic", Duration.ofSeconds(31)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
