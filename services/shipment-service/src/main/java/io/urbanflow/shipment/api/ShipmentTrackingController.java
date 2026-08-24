package io.urbanflow.shipment.api;

import io.urbanflow.shipment.application.ShipmentApplicationService;
import io.urbanflow.shipment.domain.Shipment;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes public tracking lookup without overlapping versioned shipment command routes. */
@Validated
@RestController
@RequestMapping("/api/v1/tracking")
public class ShipmentTrackingController {

  private final ShipmentApplicationService shipmentService;

  /** Creates a tracking controller backed by the shipment application service. */
  public ShipmentTrackingController(ShipmentApplicationService shipmentService) {
    this.shipmentService = shipmentService;
  }

  /** Returns a shipment by public tracking number. */
  @GetMapping("/{trackingNumber}")
  public ResponseEntity<ShipmentResponse> track(
      @PathVariable @Pattern(regexp = "UF[0-9A-F]{18}") String trackingNumber) {
    Shipment shipment = shipmentService.findByTrackingNumber(trackingNumber);
    return ResponseEntity.ok()
        .eTag(Long.toString(shipment.version()))
        .body(ShipmentResponse.from(shipment));
  }
}
