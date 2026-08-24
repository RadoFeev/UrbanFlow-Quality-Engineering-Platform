package io.urbanflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.urbanflow.identity.domain.Role;
import io.urbanflow.identity.domain.UserAccount;
import io.urbanflow.identity.persistence.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");
  private static final UUID USER_ID = UUID.fromString("098ba688-e15c-423f-8fae-c9b3fa2f2bf4");

  @Mock private UserAccountRepository users;

  @Test
  void returnsSafeProfile() {
    when(users.findById(USER_ID)).thenReturn(Optional.of(account()));

    UserProfile profile = service().profile(USER_ID);

    assertThat(profile.id()).isEqualTo(USER_ID);
    assertThat(profile.email()).isEqualTo("operator@example.test");
  }

  @Test
  void changesRoleWhenVersionMatches() {
    UserAccount account = account();
    when(users.findById(USER_ID)).thenReturn(Optional.of(account));
    when(users.save(account)).thenReturn(account);

    UserProfile profile = service().changeRole(USER_ID, Role.OPERATOR, 0);

    assertThat(profile.role()).isEqualTo(Role.OPERATOR);
    assertThat(account.updatedAt()).isEqualTo(NOW);
    verify(users).save(account);
  }

  @Test
  void rejectsMissingUserAndVersionConflict() {
    when(users.findById(USER_ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service().profile(USER_ID)).isInstanceOf(UserNotFoundException.class);

    when(users.findById(USER_ID)).thenReturn(Optional.of(account()));
    assertThatThrownBy(() -> service().changeRole(USER_ID, Role.ADMIN, 9))
        .isInstanceOf(VersionConflictException.class)
        .hasMessageContaining("expected 9");
  }

  private UserAdministrationService service() {
    return new UserAdministrationService(users, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static UserAccount account() {
    return UserAccount.register(
        USER_ID, "operator@example.test", "Operator", "hash", NOW.minusSeconds(10));
  }
}
