package io.urbanflow.notification.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class NotificationPropertiesTest {

  @Test
  void acceptsHttpsAndBoundedPolicy() {
    NotificationProperties properties =
        new NotificationProperties(
            URI.create("https://communications.urbanflow.test"), Duration.ofSeconds(10), 5);
    assertThat(properties.maximumAttempts()).isEqualTo(5);
  }

  @Test
  void rejectsNonHttpsGateway() {
    assertThatThrownBy(
            () ->
                new NotificationProperties(
                    URI.create("http://communications.test"), Duration.ofSeconds(1), 3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("HTTPS");
  }

  @Test
  void rejectsInvalidTimeoutPartitions() {
    assertThatThrownBy(
            () ->
                new NotificationProperties(
                    URI.create("https://communications.test"), Duration.ZERO, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new NotificationProperties(
                    URI.create("https://communications.test"), Duration.ofSeconds(31), 3))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsAttemptLimitsOutsideBounds() {
    assertThatThrownBy(
            () ->
                new NotificationProperties(
                    URI.create("https://communications.test"), Duration.ofSeconds(1), 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new NotificationProperties(
                    URI.create("https://communications.test"), Duration.ofSeconds(1), 21))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
