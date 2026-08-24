package io.urbanflow.identity.application;

import io.urbanflow.identity.domain.AccountStatus;
import io.urbanflow.identity.domain.Role;
import io.urbanflow.identity.domain.UserAccount;
import java.util.UUID;

/** Safe user representation that never exposes credential hashes. */
public record UserProfile(
    UUID id, String email, String displayName, Role role, AccountStatus status, long version) {

  /** Maps a persisted account to its public profile. */
  public static UserProfile from(UserAccount account) {
    return new UserProfile(
        account.id(),
        account.email(),
        account.displayName(),
        account.role(),
        account.status(),
        account.version());
  }
}
