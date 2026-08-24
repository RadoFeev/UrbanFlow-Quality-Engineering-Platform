package io.urbanflow.identity.persistence;

import io.urbanflow.identity.domain.RefreshSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for refresh-token sessions. */
public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

  /** Looks up a session by its irreversible credential hash. */
  Optional<RefreshSession> findByTokenHash(String tokenHash);

  /** Lists live sessions so a password reset can revoke them atomically. */
  List<RefreshSession> findAllByUserIdAndRevokedAtIsNull(UUID userId);
}
