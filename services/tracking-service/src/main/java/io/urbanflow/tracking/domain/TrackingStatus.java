package io.urbanflow.tracking.domain;

/** Shipment statuses accepted by the independently owned tracking projection. */
public enum TrackingStatus {
  CREATED,
  CONFIRMED,
  COURIER_ASSIGNED,
  PICKUP_IN_PROGRESS,
  PICKED_UP,
  AT_ORIGIN_HUB,
  IN_TRANSIT,
  AT_DESTINATION_HUB,
  OUT_FOR_DELIVERY,
  DELIVERY_ATTEMPTED,
  DELIVERED,
  CANCELLED,
  RETURN_IN_PROGRESS,
  RETURNED
}
