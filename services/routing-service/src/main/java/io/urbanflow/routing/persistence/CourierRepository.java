package io.urbanflow.routing.persistence;

import io.urbanflow.routing.domain.Courier;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Persistence port for courier capacity. */
public interface CourierRepository extends JpaRepository<Courier, UUID> {

  /** Selects available capacity deterministically by current load and identifier. */
  @Query(
      """
      select c from Courier c
      where c.status =
        io.urbanflow.routing.domain.CourierStatus.AVAILABLE
        and c.activeAssignments < c.maximumAssignments
      order by c.activeAssignments asc, c.id asc
      """)
  List<Courier> findAssignable(Pageable pageable);
}
