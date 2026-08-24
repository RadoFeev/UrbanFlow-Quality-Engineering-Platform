package io.urbanflow.notification.api;

import java.util.List;

/** Stable paginated customer notification feed. */
public record NotificationPage(
    List<NotificationResponse> items, int page, int size, long totalElements, int totalPages) {

  /** Defensively copies the page items. */
  public NotificationPage {
    items = List.copyOf(items);
  }
}
