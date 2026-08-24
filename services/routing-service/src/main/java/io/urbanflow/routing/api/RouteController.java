package io.urbanflow.routing.api;

import io.urbanflow.routing.application.RouteLifecycleService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Route lifecycle and courier assignment HTTP API. */
@RestController
@RequestMapping("/api/v1")
public class RouteController {

  private final RouteLifecycleService routes;

  /** Creates the route HTTP adapter. */
  public RouteController(RouteLifecycleService routes) {
    this.routes = routes;
  }

  /** Returns one route with its optimistic entity tag. */
  @GetMapping("/routes/{id}")
  public ResponseEntity<RouteResponse> route(@PathVariable UUID id) {
    return response(RouteResponse.from(routes.find(id)));
  }

  /** Returns stable ordered assignments for a courier. */
  @GetMapping("/couriers/{courierId}/assignments")
  public ResponseEntity<List<RouteResponse>> assignments(@PathVariable UUID courierId) {
    List<RouteResponse> body =
        routes.assignments(courierId).stream().map(RouteResponse::from).toList();
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
  }

  /** Starts a planned route. */
  @PostMapping("/routes/{id}/start")
  public ResponseEntity<RouteResponse> start(
      @PathVariable UUID id, @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch) {
    return response(RouteResponse.from(routes.start(id, EntityTagVersion.parse(ifMatch))));
  }

  /** Completes a started route and releases courier capacity. */
  @PostMapping("/routes/{id}/complete")
  public ResponseEntity<RouteResponse> complete(
      @PathVariable UUID id, @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch) {
    return response(RouteResponse.from(routes.complete(id, EntityTagVersion.parse(ifMatch))));
  }

  private static ResponseEntity<RouteResponse> response(RouteResponse response) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .eTag(EntityTagVersion.format(response.version()))
        .body(response);
  }
}
