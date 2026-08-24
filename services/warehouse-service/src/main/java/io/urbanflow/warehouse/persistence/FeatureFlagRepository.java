package io.urbanflow.warehouse.persistence;

import io.urbanflow.warehouse.domain.FeatureFlag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Feature-flag persistence port. */
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

  /** Finds a feature flag by stable key. */
  Optional<FeatureFlag> findByKey(String key);

  /** Lists feature flags by stable key. */
  List<FeatureFlag> findAllByOrderByKeyAsc();
}
