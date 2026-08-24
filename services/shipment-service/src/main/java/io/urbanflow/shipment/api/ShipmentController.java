package io.urbanflow.shipment.api;

import io.urbanflow.shipment.application.RequestTraceContext;
import io.urbanflow.shipment.application.ShipmentApplicationService;
import io.urbanflow.shipment.domain.Shipment;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Exposes versioned shipment creation, query, transition, and address APIs. */
@Validated
@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

  private final ShipmentApplicationService shipmentService;

  /** Creates a controller backed by the shipment application service. */
  public ShipmentController(ShipmentApplicationService shipmentService) {
    this.shipmentService = shipmentService;
  }

  /** Creates a shipment idempotently and returns its version as an ETag. */
  @PostMapping
  public ResponseEntity<ShipmentResponse> create(
      @RequestHeader("Idempotency-Key")
          @Size(min = 8, max = 200)
          @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{7,199}")
          String idempotencyKey,
      @Valid @RequestBody CreateShipmentRequest request,
      HttpServletRequest servletRequest) {
    Shipment shipment =
        shipmentService.create(idempotencyKey, request.toCommand(), traceContext(servletRequest));
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(shipment.id())
            .toUri();
    return ResponseEntity.created(location)
        .eTag(Long.toString(shipment.version()))
        .body(ShipmentResponse.from(shipment));
  }

  /** Returns a shipment by UUID with its current version as an ETag. */
  @GetMapping("/{shipmentId}")
  public ResponseEntity<ShipmentResponse> findById(@PathVariable UUID shipmentId) {
    return response(shipmentService.findById(shipmentId));
  }

  /** Applies a state transition when the supplied entity version is current. */
  @PostMapping("/{shipmentId}/transitions")
  public ResponseEntity<ShipmentResponse> transition(
      @PathVariable UUID shipmentId,
      @RequestHeader(HttpHeaders.IF_MATCH) String entityTag,
      @Valid @RequestBody TransitionShipmentRequest request,
      HttpServletRequest servletRequest) {
    Shipment shipment =
        shipmentService.transition(
            shipmentId,
            EntityTagVersion.parse(entityTag),
            request.toCommand(),
            traceContext(servletRequest));
    return response(shipment);
  }

  /** Changes the delivery address before the shipment reaches the pickup cutoff. */
  @PutMapping("/{shipmentId}/delivery-address")
  public ResponseEntity<ShipmentResponse> changeDeliveryAddress(
      @PathVariable UUID shipmentId,
      @RequestHeader(HttpHeaders.IF_MATCH) String entityTag,
      @Valid @RequestBody ChangeDeliveryAddressRequest request,
      HttpServletRequest servletRequest) {
    Shipment shipment =
        shipmentService.changeDeliveryAddress(
            shipmentId,
            EntityTagVersion.parse(entityTag),
            request.deliveryAddress().toDomain(),
            traceContext(servletRequest));
    return response(shipment);
  }

  private static ResponseEntity<ShipmentResponse> response(Shipment shipment) {
    return ResponseEntity.ok()
        .eTag(Long.toString(shipment.version()))
        .body(ShipmentResponse.from(shipment));
  }

  private static RequestTraceContext traceContext(HttpServletRequest request) {
    return new RequestTraceContext(
        request.getAttribute(RequestTracingFilter.CORRELATION_ATTRIBUTE).toString(),
        request.getAttribute(RequestTracingFilter.TRACE_ATTRIBUTE).toString());
  }
}
