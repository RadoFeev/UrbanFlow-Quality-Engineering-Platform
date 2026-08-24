package io.urbanflow.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.urbanflow.identity.config.IdentitySecurityProperties;
import io.urbanflow.identity.domain.Role;
import io.urbanflow.identity.domain.UserAccount;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class AccessTokenServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");
  private static final String SECRET = "identity-test-secret-with-32-plus-characters";
  private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

  @Test
  void roundTripsVerifiedIdentityAndAuthorizationClaims() {
    AccessTokenService service = service(properties("issuer-a", "audience-a", SECRET));

    AccessTokenClaims claims = service.verify(service.issue(user(), NOW), NOW.plusSeconds(1));

    assertThat(claims.userId()).isEqualTo(user().id());
    assertThat(claims.email()).isEqualTo("customer@example.test");
    assertThat(claims.role()).isEqualTo(Role.CUSTOMER);
    assertThat(claims.permissions())
        .containsExactlyInAnyOrderElementsOf(Role.CUSTOMER.permissions());
    assertThat(claims.tokenId()).isNotNull();
    assertThat(claims.issuedAt()).isEqualTo(NOW);
    assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
  }

  @Test
  void rejectsTamperedPayloadBeforeDeserialization() {
    AccessTokenService service = service(properties("issuer-a", "audience-a", SECRET));
    String token = service.issue(user(), NOW);
    String[] parts = token.split("\\.");
    byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
    payload[0] ^= 1;
    String tampered =
        parts[0]
            + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
            + "."
            + parts[2];

    assertThatThrownBy(() -> service.verify(tampered, NOW))
        .isInstanceOf(TokenValidationException.class)
        .hasMessageContaining("signature");
  }

  @Test
  void rejectsExpiredTokenAtExactBoundary() {
    AccessTokenService service = service(properties("issuer-a", "audience-a", SECRET));
    String token = service.issue(user(), NOW);

    assertThatThrownBy(() -> service.verify(token, NOW.plus(Duration.ofMinutes(15))))
        .isInstanceOf(TokenValidationException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void rejectsTokenIssuedBeyondClockSkew() {
    AccessTokenService service = service(properties("issuer-a", "audience-a", SECRET));
    String token = service.issue(user(), NOW.plusSeconds(31));

    assertThatThrownBy(() -> service.verify(token, NOW))
        .isInstanceOf(TokenValidationException.class)
        .hasMessageContaining("future");
  }

  @Test
  void rejectsDifferentIssuerOrAudience() {
    String token = service(properties("issuer-a", "audience-a", SECRET)).issue(user(), NOW);

    assertThatThrownBy(
            () -> service(properties("issuer-b", "audience-a", SECRET)).verify(token, NOW))
        .isInstanceOf(TokenValidationException.class)
        .hasMessageContaining("issuer");
    assertThatThrownBy(
            () -> service(properties("issuer-a", "audience-b", SECRET)).verify(token, NOW))
        .isInstanceOf(TokenValidationException.class)
        .hasMessageContaining("audience");
  }

  @Test
  void rejectsDifferentSigningKey() {
    String token = service(properties("issuer-a", "audience-a", SECRET)).issue(user(), NOW);

    assertThatThrownBy(
            () ->
                service(properties("issuer-a", "audience-a", "another-test-secret-with-32-chars!!"))
                    .verify(token, NOW))
        .isInstanceOf(TokenValidationException.class)
        .hasMessageContaining("signature");
  }

  @ParameterizedTest
  @MethodSource("malformedTokens")
  void rejectsMalformedCompactSerialization(String token) {
    AccessTokenService service = service(properties("issuer-a", "audience-a", SECRET));

    assertThatThrownBy(() -> service.verify(token, NOW))
        .isInstanceOf(TokenValidationException.class);
  }

  @Test
  void rejectsNullToken() {
    assertThatThrownBy(
            () -> service(properties("issuer-a", "audience-a", SECRET)).verify(null, NOW))
        .isInstanceOf(TokenValidationException.class);
  }

  private AccessTokenService service(IdentitySecurityProperties properties) {
    return new AccessTokenService(objectMapper, properties);
  }

  private static Stream<String> malformedTokens() {
    return Stream.of("", "one", "one.two", "one.two.three.four", "..", "a.b.***");
  }

  private static UserAccount user() {
    return UserAccount.register(
        UUID.fromString("0426270a-20ef-42a6-a10f-6d16f4b427a6"),
        "customer@example.test",
        "Customer",
        "bcrypt-hash",
        NOW);
  }

  private static IdentitySecurityProperties properties(
      String issuer, String audience, String secret) {
    return new IdentitySecurityProperties(
        issuer,
        audience,
        secret,
        Duration.ofMinutes(15),
        Duration.ofDays(14),
        Duration.ofMinutes(30),
        Duration.ofMinutes(15),
        5);
  }
}
