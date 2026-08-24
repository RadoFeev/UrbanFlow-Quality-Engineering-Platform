package io.urbanflow.warehouse.application;

import java.util.UUID;

/** Indicates a missing operational entity. */
public class WarehouseNotFoundException extends RuntimeException {

  /** Creates a stable missing-entity error. */
  public WarehouseNotFoundException(String entity, UUID id) {
    super(entity + " not found: " + id);
  }
}
