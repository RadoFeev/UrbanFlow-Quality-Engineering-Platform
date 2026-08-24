package io.urbanflow.identity.persistence;

import io.urbanflow.identity.domain.PasswordResetToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for single-use password reset tokens. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

  /** Looks up a reset authorization by its irreversible credential hash. */
  Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
