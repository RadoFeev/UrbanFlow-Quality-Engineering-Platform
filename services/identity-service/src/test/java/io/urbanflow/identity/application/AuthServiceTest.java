package io.urbanflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.urbanflow.identity.config.IdentitySecurityProperties;
import io.urbanflow.identity.domain.AccountStatus;
import io.urbanflow.identity.domain.PasswordResetToken;
import io.urbanflow.identity.domain.RefreshSession;
import io.urbanflow.identity.domain.UserAccount;
import io.urbanflow.identity.persistence.PasswordResetTokenRepository;
import io.urbanflow.identity.persistence.RefreshSessionRepository;
import io.urbanflow.identity.persistence.UserAccountRepository;
import io.urbanflow.identity.security.AccessTokenService;
import io.urbanflow.identity.security.OpaqueTokenService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");
  private static final String EMAIL = "customer@example.test";
  private static final String PASSWORD = "Valid-Pass123";

  @Mock private UserAccountRepository users;
  @Mock private RefreshSessionRepository refreshSessions;
  @Mock private PasswordResetTokenRepository resetTokens;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AccessTokenService accessTokens;
  @Mock private OpaqueTokenService opaqueTokens;

  private SimpleMeterRegistry meters;
  private AuthService service;

  @BeforeEach
  void setUp() {
    meters = new SimpleMeterRegistry();
    service =
        new AuthService(
            users,
            refreshSessions,
            resetTokens,
            passwordEncoder,
            accessTokens,
            opaqueTokens,
            properties(),
            Clock.fixed(NOW, ZoneOffset.UTC),
            meters);
  }

  @Test
  void registersNormalizedCustomerWithEncodedPassword() {
    when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
    when(users.save(any(UserAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, UserAccount.class));

    UserProfile profile = service.register(" Customer@Example.Test ", "Customer One", PASSWORD);

    ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
    verify(users).save(saved.capture());
    assertThat(saved.getValue().email()).isEqualTo(EMAIL);
    assertThat(saved.getValue().passwordHash()).isEqualTo("encoded-password");
    assertThat(profile.email()).isEqualTo(EMAIL);
    assertThat(metric("register_success")).isEqualTo(1.0);
  }

  @Test
  void rejectsDuplicateRegistrationBeforeHashing() {
    when(users.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

    assertThatThrownBy(() -> service.register(EMAIL, "Customer", PASSWORD))
        .isInstanceOf(DuplicateEmailException.class);

    verifyNoInteractions(passwordEncoder);
    verify(users, never()).save(any());
  }

  @Test
  void rejectsWeakRegistrationBeforeRepositoryLookup() {
    assertThatThrownBy(() -> service.register(EMAIL, "Customer", "weak"))
        .isInstanceOf(WeakPasswordException.class);

    verifyNoInteractions(users, passwordEncoder);
  }

  @Test
  void loginUsesGenericFailureForUnknownEmail() {
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.login(EMAIL, "wrong-password"))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(passwordEncoder).matches(any(), any());
    assertThat(metric("login_rejected")).isEqualTo(1.0);
  }

  @Test
  void invalidPasswordRecordsFailureAndTemporaryLock() {
    UserAccount user = account();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", user.passwordHash())).thenReturn(false);

    for (int attempt = 0; attempt < 5; attempt++) {
      assertThatThrownBy(() -> service.login(EMAIL, "wrong-password"))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    assertThat(user.failedLoginAttempts()).isEqualTo(5);
    assertThat(user.status()).isEqualTo(AccountStatus.LOCKED);
    assertThat(user.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    verify(users, org.mockito.Mockito.times(5)).save(user);
  }

  @Test
  void lockedAccountCannotValidatePasswordBeforeExpiry() {
    UserAccount user = account();
    user.recordFailedLogin(1, NOW.plusSeconds(60), NOW);
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service.login(EMAIL, PASSWORD))
        .isInstanceOf(AccountUnavailableException.class);

    verifyNoInteractions(passwordEncoder, accessTokens, opaqueTokens);
  }

  @Test
  void successfulLoginIssuesAccessAndPersistedHashedRefreshSession() {
    UserAccount user = account();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(PASSWORD, user.passwordHash())).thenReturn(true);
    when(opaqueTokens.generate()).thenReturn("refresh-raw");
    when(opaqueTokens.hash("refresh-raw")).thenReturn("r".repeat(64));
    when(accessTokens.issue(user, NOW)).thenReturn("access-signed");

    AuthTokens tokens = service.login(EMAIL, PASSWORD);

    ArgumentCaptor<RefreshSession> session = ArgumentCaptor.forClass(RefreshSession.class);
    verify(refreshSessions).save(session.capture());
    assertThat(session.getValue().tokenHash()).isEqualTo("r".repeat(64));
    assertThat(session.getValue().expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(14)));
    assertThat(tokens.accessToken()).isEqualTo("access-signed");
    assertThat(tokens.refreshToken()).isEqualTo("refresh-raw");
    assertThat(tokens.accessExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    assertThat(metric("login_success")).isEqualTo(1.0);
  }

  @Test
  void refreshRotatesCurrentSessionAndRejectsReplay() {
    UserAccount user = account();
    RefreshSession current =
        RefreshSession.issue(
            UUID.randomUUID(), user.id(), "o".repeat(64), NOW.minusSeconds(1), NOW.plusSeconds(60));
    when(opaqueTokens.hash("old-refresh")).thenReturn("o".repeat(64));
    when(refreshSessions.findByTokenHash("o".repeat(64))).thenReturn(Optional.of(current));
    when(users.findById(user.id())).thenReturn(Optional.of(user));
    when(opaqueTokens.generate()).thenReturn("new-refresh");
    when(opaqueTokens.hash("new-refresh")).thenReturn("n".repeat(64));
    when(accessTokens.issue(user, NOW)).thenReturn("new-access");

    AuthTokens tokens = service.refresh("old-refresh");

    assertThat(current.revokedAt()).isEqualTo(NOW);
    assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
    assertThat(tokens.accessToken()).isEqualTo("new-access");
    verify(refreshSessions, org.mockito.Mockito.times(2)).save(any(RefreshSession.class));
  }

  @Test
  void rejectsUnknownRefreshToken() {
    when(opaqueTokens.hash("unknown")).thenReturn("u".repeat(64));
    when(refreshSessions.findByTokenHash("u".repeat(64))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.refresh("unknown"))
        .isInstanceOf(InvalidSessionException.class);

    verifyNoInteractions(users, accessTokens);
  }

  @Test
  void logoutRevokesKnownSessionAndHidesUnknownSession() {
    RefreshSession session =
        RefreshSession.issue(
            UUID.randomUUID(),
            account().id(),
            "k".repeat(64),
            NOW.minusSeconds(1),
            NOW.plusSeconds(60));
    when(opaqueTokens.hash("known")).thenReturn("k".repeat(64));
    when(refreshSessions.findByTokenHash("k".repeat(64))).thenReturn(Optional.of(session));

    service.logout("known");

    assertThat(session.revokedAt()).isEqualTo(NOW);
    verify(refreshSessions).save(session);
  }

  @Test
  void resetRequestReturnsRawTokenOnlyToDeliveryBoundaryAndStoresHash() {
    UserAccount user = account();
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
    when(opaqueTokens.generate()).thenReturn("reset-raw");
    when(opaqueTokens.hash("reset-raw")).thenReturn("x".repeat(64));

    assertThat(service.requestPasswordReset(EMAIL)).contains("reset-raw");

    ArgumentCaptor<PasswordResetToken> token = ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(resetTokens).save(token.capture());
    assertThat(token.getValue().tokenHash()).isEqualTo("x".repeat(64));
    assertThat(token.getValue().expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
  }

  @Test
  void resetRequestDoesNotRevealUnknownEmail() {
    when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

    assertThat(service.requestPasswordReset(EMAIL)).isEmpty();

    verifyNoInteractions(resetTokens);
  }

  @Test
  void passwordResetConsumesTokenAndRevokesEveryLiveSession() {
    UserAccount user = account();
    PasswordResetToken reset =
        PasswordResetToken.issue(
            UUID.randomUUID(), user.id(), "z".repeat(64), NOW.plus(Duration.ofMinutes(5)));
    RefreshSession first = session(user, "1");
    RefreshSession second = session(user, "2");
    when(opaqueTokens.hash("reset-raw")).thenReturn("z".repeat(64));
    when(resetTokens.findByTokenHash("z".repeat(64))).thenReturn(Optional.of(reset));
    when(users.findById(user.id())).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("New-Valid-Pass456")).thenReturn("new-password-hash");
    when(refreshSessions.findAllByUserIdAndRevokedAtIsNull(user.id()))
        .thenReturn(List.of(first, second));

    service.resetPassword("reset-raw", "New-Valid-Pass456");

    assertThat(user.passwordHash()).isEqualTo("new-password-hash");
    assertThat(reset.usedAt()).isEqualTo(NOW);
    assertThat(first.revokedAt()).isEqualTo(NOW);
    assertThat(second.revokedAt()).isEqualTo(NOW);
    verify(refreshSessions, org.mockito.Mockito.times(2)).save(any(RefreshSession.class));
  }

  private double metric(String result) {
    return meters.get("urbanflow.identity.authentication").tag("result", result).counter().count();
  }

  private static UserAccount account() {
    return UserAccount.register(
        UUID.fromString("3f3ddce0-b78a-4eb3-837b-53e7a5297930"),
        EMAIL,
        "Customer One",
        "stored-password-hash",
        NOW.minusSeconds(60));
  }

  private static RefreshSession session(UserAccount user, String suffix) {
    return RefreshSession.issue(
        UUID.randomUUID(), user.id(), suffix.repeat(64), NOW.minusSeconds(60), NOW.plusSeconds(60));
  }

  private static IdentitySecurityProperties properties() {
    return new IdentitySecurityProperties(
        "https://identity.urbanflow.test",
        "urbanflow-platform",
        "identity-test-secret-with-32-plus-characters",
        Duration.ofMinutes(15),
        Duration.ofDays(14),
        Duration.ofMinutes(30),
        Duration.ofMinutes(15),
        5);
  }
}
