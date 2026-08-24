package io.urbanflow.notification.persistence;

import io.urbanflow.notification.domain.Notification;
import io.urbanflow.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for notifications and delivery work. */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  /** Checks source-event idempotency. */
  boolean existsBySourceEventId(UUID sourceEventId);

  /** Finds a bounded due delivery batch. */
  List<Notification> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
      Collection<NotificationStatus> statuses, Instant now);

  /** Returns a customer's newest notifications with pagination. */
  Page<Notification> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
}
