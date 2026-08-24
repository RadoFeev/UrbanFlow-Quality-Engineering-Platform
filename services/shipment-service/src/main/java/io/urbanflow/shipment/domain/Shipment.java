package io.urbanflow.shipment.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Shipment aggregate that enforces versioned lifecycle and address updates. */
@Entity
@Table(name = "shipments")
public class Shipment {

  @Id private UUID id;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "tracking_number", nullable = false, unique = true, length = 24)
  private String trackingNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private ShipmentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_type", nullable = false, length = 24)
  private DeliveryType deliveryType;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(
        name = "contactName",
        column = @Column(name = "pickup_contact_name", nullable = false, length = 120)),
    @AttributeOverride(
        name = "line1",
        column = @Column(name = "pickup_line_1", nullable = false, length = 160)),
    @AttributeOverride(name = "line2", column = @Column(name = "pickup_line_2", length = 160)),
    @AttributeOverride(
        name = "city",
        column = @Column(name = "pickup_city", nullable = false, length = 100)),
    @AttributeOverride(
        name = "postalCode",
        column = @Column(name = "pickup_postal_code", nullable = false, length = 20)),
    @AttributeOverride(
        name = "countryCode",
        column = @Column(name = "pickup_country_code", nullable = false, length = 2)),
    @AttributeOverride(
        name = "phone",
        column = @Column(name = "pickup_phone", nullable = false, length = 16))
  })
  private AddressSnapshot pickupAddress;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(
        name = "contactName",
        column = @Column(name = "delivery_contact_name", nullable = false, length = 120)),
    @AttributeOverride(
        name = "line1",
        column = @Column(name = "delivery_line_1", nullable = false, length = 160)),
    @AttributeOverride(name = "line2", column = @Column(name = "delivery_line_2", length = 160)),
    @AttributeOverride(
        name = "city",
        column = @Column(name = "delivery_city", nullable = false, length = 100)),
    @AttributeOverride(
        name = "postalCode",
        column = @Column(name = "delivery_postal_code", nullable = false, length = 20)),
    @AttributeOverride(
        name = "countryCode",
        column = @Column(name = "delivery_country_code", nullable = false, length = 2)),
    @AttributeOverride(
        name = "phone",
        column = @Column(name = "delivery_phone", nullable = false, length = 16))
  })
  private AddressSnapshot deliveryAddress;

  @Embedded private ParcelSnapshot parcel;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Constructor reserved for persistence materialization. */
  protected Shipment() {}

  /** Creates a new shipment in the `CREATED` state. */
  public static Shipment create(
      UUID id,
      UUID customerId,
      String trackingNumber,
      DeliveryType deliveryType,
      AddressSnapshot pickupAddress,
      AddressSnapshot deliveryAddress,
      ParcelSnapshot parcel,
      Instant now) {
    Shipment shipment = new Shipment();
    shipment.id = Objects.requireNonNull(id, "id must not be null");
    shipment.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
    shipment.trackingNumber = requireText(trackingNumber, "trackingNumber");
    shipment.deliveryType = Objects.requireNonNull(deliveryType, "deliveryType must not be null");
    shipment.pickupAddress =
        Objects.requireNonNull(pickupAddress, "pickupAddress must not be null");
    shipment.deliveryAddress =
        Objects.requireNonNull(deliveryAddress, "deliveryAddress must not be null");
    shipment.parcel = Objects.requireNonNull(parcel, "parcel must not be null");
    shipment.status = ShipmentStatus.CREATED;
    shipment.createdAt = Objects.requireNonNull(now, "now must not be null");
    shipment.updatedAt = now;
    return shipment;
  }

  /** Applies a transition after checking the expected version and central policy. */
  public void transition(
      long expectedVersion,
      ShipmentTransitionCommand command,
      ShipmentTransitionPolicy policy,
      Instant now) {
    requireVersion(expectedVersion);
    policy.validate(status, command);
    status = command.targetStatus();
    updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Changes the delivery address only before the configured lifecycle cutoff. */
  public void changeDeliveryAddress(long expectedVersion, AddressSnapshot newAddress, Instant now) {
    requireVersion(expectedVersion);
    if (!status.allowsDeliveryAddressChange()) {
      throw new DeliveryAddressChangeNotAllowedException(id, status);
    }
    deliveryAddress = Objects.requireNonNull(newAddress, "newAddress must not be null");
    updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Returns the shipment UUID. */
  public UUID id() {
    return id;
  }

  /** Returns the owning customer UUID. */
  public UUID customerId() {
    return customerId;
  }

  /** Returns the public tracking number. */
  public String trackingNumber() {
    return trackingNumber;
  }

  /** Returns the current lifecycle status. */
  public ShipmentStatus status() {
    return status;
  }

  /** Returns the selected delivery service level. */
  public DeliveryType deliveryType() {
    return deliveryType;
  }

  /** Returns the immutable pickup-address snapshot. */
  public AddressSnapshot pickupAddress() {
    return pickupAddress;
  }

  /** Returns the current delivery-address snapshot. */
  public AddressSnapshot deliveryAddress() {
    return deliveryAddress;
  }

  /** Returns the immutable parcel snapshot. */
  public ParcelSnapshot parcel() {
    return parcel;
  }

  /** Returns the optimistic-lock version. */
  public long version() {
    return version;
  }

  /** Returns the creation timestamp in UTC. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns the most recent business-update timestamp in UTC. */
  public Instant updatedAt() {
    return updatedAt;
  }

  private void requireVersion(long expectedVersion) {
    if (expectedVersion != version) {
      throw new ShipmentVersionConflictException(id, expectedVersion, version);
    }
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
