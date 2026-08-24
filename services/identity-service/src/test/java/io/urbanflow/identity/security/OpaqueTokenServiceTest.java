package io.urbanflow.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class OpaqueTokenServiceTest {

  private final OpaqueTokenService tokens =
      new OpaqueTokenService(new SecureRandom(new byte[] {1}));

  @Test
  void generatesUrlSafeCredentialWithExpectedEntropyLength() {
    assertThat(tokens.generate()).matches("[A-Za-z0-9_-]{43}");
  }

  @Test
  void generatedCredentialsAreDifferent() {
    assertThat(tokens.generate()).isNotEqualTo(tokens.generate());
  }

  @Test
  void hashesDeterministicallyWithoutReturningRawToken() {
    assertThat(tokens.hash("opaque-token"))
        .hasSize(64)
        .matches("[0-9a-f]{64}")
        .isEqualTo(tokens.hash("opaque-token"))
        .isNotEqualTo("opaque-token");
  }

  @Test
  void rejectsBlankTokenHashInput() {
    assertThatThrownBy(() -> tokens.hash(" ")).isInstanceOf(IllegalArgumentException.class);
  }
}
