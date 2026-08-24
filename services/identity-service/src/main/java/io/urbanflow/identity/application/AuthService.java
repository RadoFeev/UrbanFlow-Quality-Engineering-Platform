package io.urbanflow.identity.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.urbanflow.identity.config.IdentitySecurityProperties;
import io.urbanflow.identity.domain.PasswordResetToken;
import io.urbanflow.identity.domain.RefreshSession;
import io.urbanflow.identity.domain.UserAccount;
import io.urbanflow.identity.persistence.PasswordResetTokenRepository;
import io.urbanflow.identity.persistence.RefreshSessionRepository;
import io.urbanflow.identity.persistence.UserAccountRepository;
import io.urbanflow.identity.security.AccessTokenService;
import io.urbanflow.identity.security.OpaqueTokenService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration, authentication, rotation, logout, lockout, and password-reset workflows. */
@Service
public class AuthService {

  private static final String DUMMY_PASSWORD_HASH =
      "$2a$11$rDtRbPU4dVAVV6GpZA7zheEO4jJq/lV0KK.8hPywFbgT5WJBMCA5e";

  private final UserAccountRepository users;
  private final RefreshSessionRepository refreshSessions;
  private final PasswordResetTokenRepository resetTokens;
  private final PasswordEncoder passwordEncoder;
  private final AccessTokenService accessTokens;
  private final OpaqueTokenService opaqueTokens;
  private final IdentitySecurityProperties properties;
  private final Clock clock;
  private final MeterRegistry meterRegistry;

  /** Creates the transactional authentication workflow service. */
  public AuthService(
      UserAccountRepository users,
      RefreshSessionRepository refreshSessions,
      PasswordResetTokenRepository resetTokens,
      PasswordEncoder passwordEncoder,
      AccessTokenService accessTokens,
      OpaqueTokenService opaqueTokens,
      IdentitySecurityProperties properties,
      Clock clock,
      MeterRegistry meterRegistry) {
    this.users = users;
    this.refreshSessions = refreshSessions;
    this.resetTokens = resetTokens;
    this.passwordEncoder = passwordEncoder;
    this.accessTokens = accessTokens;
    this.opaqueTokens = opaqueTokens;
    this.properties = properties;
    this.clock = clock;
    this.meterRegistry = meterRegistry;
  }

  /** Registers one canonical customer identity. */
  @Transactional
  public UserProfile register(String email, String displayName, String password) {
    PasswordPolicy.validate(password);
    String normalizedEmail = UserAccount.normalizeEmail(email);
    if (users.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new DuplicateEmailException();
    }
    UserAccount account =
        UserAccount.register(
            UUID.randomUUID(),
            normalizedEmail,
            displayName,
            passwordEncoder.encode(password),
            clock.instant());
    UserProfile profile = UserProfile.from(users.save(account));
    count("register_success");
    return profile;
  }

  /** Authenticates credentials while applying enumeration-resistant errors and lockout. */
  @Transactional
  public AuthTokens login(String email, String password) {
    Instant now = clock.instant();
    Optional<UserAccount> candidate =
        users.findByEmailIgnoreCase(UserAccount.normalizeEmail(email));
    if (candidate.isEmpty()) {
      passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
      count("login_rejected");
      throw new InvalidCredentialsException();
    }
    UserAccount user = candidate.orElseThrow();
    if (!user.canAuthenticate(now)) {
      count("login_unavailable");
      throw new AccountUnavailableException();
    }
    if (!passwordEncoder.matches(password, user.passwordHash())) {
      user.recordFailedLogin(
          properties.maximumLoginAttempts(), now.plus(properties.lockoutDuration()), now);
      users.save(user);
      count("login_rejected");
      throw new InvalidCredentialsException();
    }
    user.recordSuccessfulLogin(now);
    users.save(user);
    count("login_success");
    return issueSession(user, now);
  }

  /** Rotates an active refresh token and rejects replay of the previous credential. */
  @Transactional
  public AuthTokens refresh(String rawRefreshToken) {
    Instant now = clock.instant();
    RefreshSession current =
        refreshSessions
            .findByTokenHash(opaqueTokens.hash(rawRefreshToken))
            .filter(session -> session.isActive(now))
            .orElseThrow(() -> new InvalidSessionException("refresh token is invalid"));
    final UserAccount user =
        users
            .findById(current.userId())
            .filter(account -> account.canAuthenticate(now))
            .orElseThrow(() -> new InvalidSessionException("refresh token is invalid"));
    current.revoke(now);
    refreshSessions.save(current);
    count("refresh_success");
    return issueSession(user, now);
  }

  /** Revokes a refresh token without revealing whether it existed. */
  @Transactional
  public void logout(String rawRefreshToken) {
    Instant now = clock.instant();
    refreshSessions
        .findByTokenHash(opaqueTokens.hash(rawRefreshToken))
        .ifPresent(
            session -> {
              session.revoke(now);
              refreshSessions.save(session);
            });
    count("logout");
  }

  /** Creates a reset token only for an active account while allowing a generic HTTP response. */
  @Transactional
  public Optional<String> requestPasswordReset(String email) {
    Instant now = clock.instant();
    return users
        .findByEmailIgnoreCase(UserAccount.normalizeEmail(email))
        .filter(user -> user.canAuthenticate(now))
        .map(
            user -> {
              String rawToken = opaqueTokens.generate();
              resetTokens.save(
                  PasswordResetToken.issue(
                      UUID.randomUUID(),
                      user.id(),
                      opaqueTokens.hash(rawToken),
                      now.plus(properties.resetTtl())));
              count("reset_requested");
              return rawToken;
            });
  }

  /** Consumes a reset token, changes the password, and revokes all live refresh sessions. */
  @Transactional
  public void resetPassword(String rawResetToken, String newPassword) {
    PasswordPolicy.validate(newPassword);
    Instant now = clock.instant();
    PasswordResetToken resetToken =
        resetTokens
            .findByTokenHash(opaqueTokens.hash(rawResetToken))
            .filter(token -> token.isActive(now))
            .orElseThrow(() -> new InvalidSessionException("password reset token is invalid"));
    UserAccount user =
        users
            .findById(resetToken.userId())
            .orElseThrow(() -> new InvalidSessionException("password reset token is invalid"));
    user.changePassword(passwordEncoder.encode(newPassword), now);
    resetToken.consume(now);
    users.save(user);
    resetTokens.save(resetToken);
    refreshSessions
        .findAllByUserIdAndRevokedAtIsNull(user.id())
        .forEach(
            session -> {
              session.revoke(now);
              refreshSessions.save(session);
            });
    count("reset_completed");
  }

  private AuthTokens issueSession(UserAccount user, Instant now) {
    String refreshToken = opaqueTokens.generate();
    refreshSessions.save(
        RefreshSession.issue(
            UUID.randomUUID(),
            user.id(),
            opaqueTokens.hash(refreshToken),
            now,
            now.plus(properties.refreshTtl())));
    return AuthTokens.bearer(
        accessTokens.issue(user, now), refreshToken, now.plus(properties.accessTtl()));
  }

  private void count(String result) {
    meterRegistry.counter("urbanflow.identity.authentication", "result", result).increment();
  }
}
