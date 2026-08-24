package io.urbanflow.testdata.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

  private final SensitiveDataMasker masker = new SensitiveDataMasker();

  @Test
  void masksBearerCredential() {
    assertThat(masker.mask("Authorization: Bearer header.payload.signature"))
        .isEqualTo("Authorization: Bearer [REDACTED]");
  }

  @Test
  void masksPasswordAndTokenFields() {
    assertThat(masker.mask("password=Visible accessToken:abc refreshToken=xyz"))
        .doesNotContain("Visible", "abc", "xyz")
        .contains("[REDACTED]");
  }

  @Test
  void masksEmailAndPhone() {
    assertThat(masker.mask("contact user@example.test at +359880000001"))
        .isEqualTo("contact [EMAIL_REDACTED] at [PHONE_REDACTED]");
  }

  @Test
  void handlesNullWithoutFailure() {
    assertThat(masker.mask(null)).isEmpty();
  }

  @Test
  void preservesNonSensitiveDiagnosticText() {
    assertThat(masker.mask("shipment status changed to DELIVERED"))
        .isEqualTo("shipment status changed to DELIVERED");
  }
}
