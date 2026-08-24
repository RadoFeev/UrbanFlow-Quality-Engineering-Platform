package io.urbanflow.identity.persistence;

import io.urbanflow.identity.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for identity accounts. */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

  /** Looks up an account using canonical case-insensitive email semantics. */
  Optional<UserAccount> findByEmailIgnoreCase(String email);

  /** Returns whether an email is already registered. */
  boolean existsByEmailIgnoreCase(String email);
}
