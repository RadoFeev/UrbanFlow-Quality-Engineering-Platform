package io.urbanflow.identity.security;

import io.urbanflow.identity.config.IdentitySecurityProperties;
import io.urbanflow.identity.domain.Permission;
import io.urbanflow.identity.domain.Role;
import io.urbanflow.identity.domain.UserAccount;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Issues and verifies compact HS256 access tokens without persisting bearer credentials. */
@Component
public class AccessTokenService {

  private static final String ALGORITHM = "HmacSHA256";
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final ObjectMapper objectMapper;
  private final IdentitySecurityProperties properties;
  private final SecretKeySpec signingKey;

  /** Creates an access-token codec from the service mapper and validated security policy. */
  public AccessTokenService(ObjectMapper objectMapper, IdentitySecurityProperties properties) {
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.signingKey =
        new SecretKeySpec(properties.signingSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
  }

  /** Issues a signed short-lived token for the current user authorization state. */
  public String issue(UserAccount user, Instant now) {
    TokenHeader header = new TokenHeader("HS256", "JWT");
    Instant expiresAt = now.plus(properties.accessTtl());
    TokenPayload payload =
        new TokenPayload(
            properties.issuer(),
            properties.audience(),
            user.id(),
            user.email(),
            user.role(),
            user.role().permissions(),
            UUID.randomUUID(),
            now.getEpochSecond(),
            expiresAt.getEpochSecond());
    try {
      String encodedHeader = ENCODER.encodeToString(objectMapper.writeValueAsBytes(header));
      String encodedPayload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
      String signedContent = encodedHeader + "." + encodedPayload;
      return signedContent + "." + ENCODER.encodeToString(sign(signedContent));
    } catch (Exception exception) {
      throw new IllegalStateException("access token could not be encoded", exception);
    }
  }

  /** Verifies signature, issuer, audience, lifetime, and mandatory authorization claims. */
  public AccessTokenClaims verify(String token, Instant now) {
    try {
      String[] parts = token == null ? new String[0] : token.split("\\.", -1);
      if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
        throw new TokenValidationException("access token format is invalid");
      }
      String signedContent = parts[0] + "." + parts[1];
      byte[] suppliedSignature = DECODER.decode(parts[2]);
      if (!java.security.MessageDigest.isEqual(sign(signedContent), suppliedSignature)) {
        throw new TokenValidationException("access token signature is invalid");
      }
      TokenHeader header = objectMapper.readValue(DECODER.decode(parts[0]), TokenHeader.class);
      if (!"HS256".equals(header.alg()) || !"JWT".equals(header.typ())) {
        throw new TokenValidationException("access token header is invalid");
      }
      TokenPayload payload = objectMapper.readValue(DECODER.decode(parts[1]), TokenPayload.class);
      validatePayload(payload, now);
      return new AccessTokenClaims(
          payload.sub(),
          payload.email(),
          payload.role(),
          payload.permissions(),
          payload.jti(),
          Instant.ofEpochSecond(payload.iat()),
          Instant.ofEpochSecond(payload.exp()));
    } catch (TokenValidationException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new TokenValidationException("access token could not be verified", exception);
    }
  }

  private void validatePayload(TokenPayload payload, Instant now) {
    if (!properties.issuer().equals(payload.iss())
        || !properties.audience().equals(payload.aud())) {
      throw new TokenValidationException("access token issuer or audience is invalid");
    }
    if (payload.sub() == null
        || payload.email() == null
        || payload.email().isBlank()
        || payload.role() == null
        || payload.permissions() == null
        || payload.jti() == null) {
      throw new TokenValidationException("access token claims are incomplete");
    }
    Instant issuedAt = Instant.ofEpochSecond(payload.iat());
    Instant expiresAt = Instant.ofEpochSecond(payload.exp());
    if (!issuedAt.isBefore(expiresAt) || !now.isBefore(expiresAt)) {
      throw new TokenValidationException("access token is expired or has an invalid lifetime");
    }
    if (issuedAt.isAfter(now.plusSeconds(30))) {
      throw new TokenValidationException("access token issue time is in the future");
    }
    if (!payload.role().permissions().containsAll(payload.permissions())) {
      throw new TokenValidationException("access token permissions exceed the assigned role");
    }
  }

  private byte[] sign(String content) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(signingKey);
      return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
    }
  }

  private record TokenHeader(String alg, String typ) {}

  private record TokenPayload(
      String iss,
      String aud,
      UUID sub,
      String email,
      Role role,
      Set<Permission> permissions,
      UUID jti,
      long iat,
      long exp) {}
}
