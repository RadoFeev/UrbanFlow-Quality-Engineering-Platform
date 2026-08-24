package io.urbanflow.identity.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Generates high-entropy opaque credentials and irreversible lookup hashes. */
@Component
public class OpaqueTokenService {

  private static final int TOKEN_BYTES = 32;
  private final SecureRandom secureRandom;

  /** Uses the platform cryptographic random source. */
  public OpaqueTokenService() {
    this(new SecureRandom());
  }

  OpaqueTokenService(SecureRandom secureRandom) {
    this.secureRandom = secureRandom;
  }

  /** Generates a URL-safe token with 256 bits of entropy. */
  public String generate() {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** Hashes a token for safe persistent lookup. */
  public String hash(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("token must not be blank");
    }
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
