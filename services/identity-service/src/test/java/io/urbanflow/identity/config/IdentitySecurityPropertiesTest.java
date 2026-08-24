package io.urbanflow.identity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class IdentitySecurityPropertiesTest {

  @Test
  void acceptsSafeConfiguration() {
    assertThat(valid().maximumLoginAttempts()).isEqualTo(5);
  }

  @ParameterizedTest
  @MethodSource("invalidConfigurations")
  void rejectsUnsafeConfiguration(UnaryOperator<Builder> mutation) {
    Builder builder = mutation.apply(new Builder());

    assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
  }

  private static Stream<UnaryOperator<Builder>> invalidConfigurations() {
    return Stream.of(
        builder -> builder.withIssuer(" "),
        builder -> builder.withAudience(null),
        builder -> builder.withSecret("short"),
        builder -> builder.withAccessTtl(Duration.ZERO),
        builder -> builder.withRefreshTtl(Duration.ofSeconds(-1)),
        builder -> builder.withResetTtl(null),
        builder -> builder.withLockoutDuration(Duration.ZERO),
        builder -> builder.withMaximumAttempts(1));
  }

  private static IdentitySecurityProperties valid() {
    return new Builder().build();
  }

  private static final class Builder {
    private String issuer = "https://identity.urbanflow.test";
    private String audience = "urbanflow-platform";
    private String secret = "identity-test-secret-with-32-plus-characters";
    private Duration accessTtl = Duration.ofMinutes(15);
    private Duration refreshTtl = Duration.ofDays(14);
    private Duration resetTtl = Duration.ofMinutes(30);
    private Duration lockoutDuration = Duration.ofMinutes(15);
    private int maximumAttempts = 5;

    Builder withIssuer(String value) {
      issuer = value;
      return this;
    }

    Builder withAudience(String value) {
      audience = value;
      return this;
    }

    Builder withSecret(String value) {
      secret = value;
      return this;
    }

    Builder withAccessTtl(Duration value) {
      accessTtl = value;
      return this;
    }

    Builder withRefreshTtl(Duration value) {
      refreshTtl = value;
      return this;
    }

    Builder withResetTtl(Duration value) {
      resetTtl = value;
      return this;
    }

    Builder withLockoutDuration(Duration value) {
      lockoutDuration = value;
      return this;
    }

    Builder withMaximumAttempts(int value) {
      maximumAttempts = value;
      return this;
    }

    IdentitySecurityProperties build() {
      return new IdentitySecurityProperties(
          issuer,
          audience,
          secret,
          accessTtl,
          refreshTtl,
          resetTtl,
          lockoutDuration,
          maximumAttempts);
    }
  }
}
