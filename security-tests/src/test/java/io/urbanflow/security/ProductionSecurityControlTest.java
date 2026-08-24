package io.urbanflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Guards production security decisions against accidental removal or weakening. */
@Tag("security")
class ProductionSecurityControlTest {

  @ParameterizedTest(name = "security control: {0}")
  @MethodSource("controls")
  void productionBoundaryRetainsSecurityControl(
      String control, String relativePath, List<String> requiredEvidence) throws IOException {
    Path source = workspace().resolve(relativePath).normalize();

    assertThat(source).as("source boundary for %s", control).isRegularFile();
    assertThat(Files.readString(source))
        .as("production evidence for %s", control)
        .contains(requiredEvidence.toArray(String[]::new));
  }

  private static Stream<Arguments> controls() {
    return Stream.of(
        identity(
            "bcrypt work factor", "config/IdentityConfiguration.java", "BCryptPasswordEncoder(11)"),
        identity(
            "enumeration-resistant password work",
            "application/AuthService.java",
            "DUMMY_PASSWORD_HASH",
            "passwordEncoder.matches"),
        identity(
            "temporary login lockout",
            "domain/UserAccount.java",
            "recordFailedLogin",
            "lockedUntil"),
        identity(
            "constant-time JWT signature validation",
            "security/AccessTokenService.java",
            "MessageDigest.isEqual"),
        identity(
            "JWT issuer and audience validation",
            "security/AccessTokenService.java",
            "properties.issuer().equals",
            "properties.audience().equals"),
        identity(
            "JWT permission escalation rejection",
            "security/AccessTokenService.java",
            "permissions exceed the assigned role"),
        identity(
            "opaque credential hashing",
            "security/OpaqueTokenService.java",
            "SHA-256",
            "MessageDigest.getInstance"),
        identity(
            "one-time refresh rotation",
            "domain/RefreshSession.java",
            "revokedAt == null",
            "now.isBefore(expiresAt)"),
        identity(
            "single-use password reset",
            "domain/PasswordResetToken.java",
            "usedAt == null",
            "usedAt = now"),
        identity(
            "password reset revokes sessions",
            "application/AuthService.java",
            "findAllByUserIdAndRevokedAtIsNull",
            "session.revoke(now)"),
        identity(
            "explicit CORS allowlist",
            "security/SecurityConfiguration.java",
            "setAllowedOrigins(properties.allowedOrigins())",
            "setAllowedMethods"),
        identity(
            "default-deny endpoint authorization",
            "security/SecurityConfiguration.java",
            ".anyRequest()",
            ".denyAll()"),
        identity(
            "fine-grained role administration permission",
            "security/SecurityConfiguration.java",
            ".hasAuthority(\"ROLE_MANAGE\")"),
        identity(
            "authenticated profile no-store policy",
            "api/UserController.java",
            "CacheControl.noStore()"),
        identity(
            "strong optimistic precondition", "api/EntityTagVersion.java", "If-Match", "v<number>"),
        service(
            "idempotency request fingerprint",
            "shipment",
            "application/CreateShipmentCommand.java",
            "SHA-256",
            "fingerprint()"),
        service(
            "safe correlation and trace parsing",
            "shipment",
            "api/RequestTracingFilter.java",
            "SAFE_CORRELATION",
            "TRACE_PARENT"),
        service(
            "HTTPS-only notification provider",
            "notification",
            "config/NotificationProperties.java",
            "https",
            "gatewayUrl.getScheme()"),
        control(
            "delivery proof MIME and size allowlist",
            "apps/courier-portal/src/main/resources/static/app.js",
            "'image/jpeg', 'image/png', 'application/pdf'",
            "5 * 1024 * 1024"),
        service(
            "append-only operational audit",
            "warehouse",
            "domain/AuditRecord.java",
            "Append-only operational audit record",
            "@Table(name = \"audit_records\")"));
  }

  private static Arguments identity(String name, String suffix, String... evidence) {
    return service(name, "identity", suffix, evidence);
  }

  private static Arguments service(String name, String service, String suffix, String... evidence) {
    String path =
        "services/%s-service/src/main/java/io/urbanflow/%s/%s".formatted(service, service, suffix);
    return control(name, path, evidence);
  }

  private static Arguments control(String name, String path, String... evidence) {
    return Arguments.of(name, path, List.of(evidence));
  }

  private static Path workspace() {
    return Path.of(System.getProperty("workspace.root")).toAbsolutePath().normalize();
  }
}
