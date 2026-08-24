package io.urbanflow.notification.api;

import io.urbanflow.notification.domain.Notification;
import io.urbanflow.notification.persistence.NotificationRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer notification feed HTTP API. */
@Validated
@RestController
@RequestMapping("/api/v1/customers/{customerId}/notifications")
public class NotificationController {

  private final NotificationRepository notifications;

  /** Creates the notification query adapter. */
  public NotificationController(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  /** Returns a bounded newest-first notification page. */
  @GetMapping
  public ResponseEntity<NotificationPage> list(
      @PathVariable UUID customerId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    Page<Notification> result =
        notifications.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(page, size));
    NotificationPage response =
        new NotificationPage(
            result.stream().map(NotificationResponse::from).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages());
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
  }
}
