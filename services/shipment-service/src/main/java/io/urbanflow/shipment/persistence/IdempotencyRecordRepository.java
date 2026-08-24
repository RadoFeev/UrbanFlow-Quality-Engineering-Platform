package io.urbanflow.shipment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Provides persistence access to shipment idempotency records. */
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {}
