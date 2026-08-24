package io.urbanflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserAccountTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Test
  void registersNormalizedActiveCustomer() {
    UserAccount account = account();

    assertThat(account.email()).isEqualTo("customer@example.test");
    assertThat(account.displayName()).isEqualTo("Customer One");
    assertThat(account.role()).isEqualTo(Role.CUSTOMER);
    assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(account.createdAt()).isEqualTo(NOW);
    assertThat(account.updatedAt()).isEqualTo(NOW);
    assertThat(account.version()).isZero();
  }

  @Test
  void locksAtConfiguredFailureThreshold() {
    UserAccount account = account();
    Instant lockExpiry = NOW.plusSeconds(900);

    account.recordFailedLogin(2, lockExpiry, NOW.plusSeconds(1));
    assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);

    account.recordFailedLogin(2, lockExpiry, NOW.plusSeconds(2));
    assertThat(account.status()).isEqualTo(AccountStatus.LOCKED);
    assertThat(account.failedLoginAttempts()).isEqualTo(2);
    assertThat(account.lockedUntil()).isEqualTo(lockExpiry);
    assertThat(account.canAuthenticate(NOW.plusSeconds(899))).isFalse();
    assertThat(account.canAuthenticate(lockExpiry)).isTrue();
  }

  @Test
  void successfulLoginClearsExpiredLockAndFailures() {
    UserAccount account = account();
    Instant lockExpiry = NOW.plusSeconds(5);
    account.recordFailedLogin(1, lockExpiry, NOW);

    account.recordSuccessfulLogin(lockExpiry);

    assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(account.failedLoginAttempts()).isZero();
    assertThat(account.lockedUntil()).isNull();
  }

  @Test
  void passwordChangeClearsTemporaryLock() {
    UserAccount account = account();
    account.recordFailedLogin(1, NOW.plusSeconds(60), NOW);

    account.changePassword("new-hash", NOW.plusSeconds(1));

    assertThat(account.passwordHash()).isEqualTo("new-hash");
    assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(account.failedLoginAttempts()).isZero();
  }

  @Test
  void roleChangeUpdatesRoleAndTimestamp() {
    UserAccount account = account();

    account.changeRole(Role.OPERATOR, NOW.plusSeconds(10));

    assertThat(account.role()).isEqualTo(Role.OPERATOR);
    assertThat(account.updatedAt()).isEqualTo(NOW.plusSeconds(10));
  }

  @Test
  void rejectsBlankRegistrationFields() {
    assertThatThrownBy(() -> UserAccount.register(UUID.randomUUID(), " ", "Name", "hash", NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email");
    assertThatThrownBy(() -> UserAccount.register(UUID.randomUUID(), "a@b.test", " ", "hash", NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("displayName");
  }

  private static UserAccount account() {
    return UserAccount.register(
        UUID.fromString("f7b23f0f-038c-46f9-a662-7621704c4f12"),
        " Customer@Example.Test ",
        " Customer One ",
        "bcrypt-hash",
        NOW);
  }
}
