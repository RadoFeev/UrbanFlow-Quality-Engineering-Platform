package io.urbanflow.identity.application;

import io.urbanflow.identity.domain.Role;
import io.urbanflow.identity.domain.UserAccount;
import io.urbanflow.identity.persistence.UserAccountRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Profile reads and version-checked administrative role changes. */
@Service
public class UserAdministrationService {

  private final UserAccountRepository users;
  private final Clock clock;

  /** Creates the user administration workflow service. */
  public UserAdministrationService(UserAccountRepository users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  /** Returns the safe profile for an authenticated subject. */
  @Transactional(readOnly = true)
  public UserProfile profile(UUID id) {
    return UserProfile.from(find(id));
  }

  /** Assigns a role only when the supplied version matches the current row. */
  @Transactional
  public UserProfile changeRole(UUID id, Role role, long expectedVersion) {
    UserAccount user = find(id);
    if (user.version() != expectedVersion) {
      throw new VersionConflictException(expectedVersion, user.version());
    }
    user.changeRole(role, clock.instant());
    return UserProfile.from(users.save(user));
  }

  private UserAccount find(UUID id) {
    return users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }
}
