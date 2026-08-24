package io.urbanflow.e2e.support;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Executes one unique black-box identity-to-shipment journey against a deployed environment. */
public final class DeployedJourneyClient implements AutoCloseable {

  private static final String PASSWORD = "Journey!Quality25";
  private final URI identityUrl;
  private final URI shipmentUrl;
  private String refreshToken;

  /** Creates a client for explicit service origins. */
  public DeployedJourneyClient(URI identityUrl, URI shipmentUrl) {
    this.identityUrl = identityUrl;
    this.shipmentUrl = shipmentUrl;
  }

  /** Executes registration, login, creation, replay, ID read, and tracking read. */
  public JourneyResult execute(String scenario) {
    String runId = UUID.randomUUID().toString();
    String email = "e2e-" + runId + "@example.test";
    Response registration =
        post(
            identityUrl,
            "/api/v1/auth/register",
            Map.of("email", email, "displayName", "E2E " + scenario, "password", PASSWORD));
    registration.then().statusCode(201);
    Response login =
        post(identityUrl, "/api/v1/auth/login", Map.of("email", email, "password", PASSWORD));
    login.then().statusCode(200);
    String customerId = registration.jsonPath().getString("id");
    String accessToken = login.jsonPath().getString("accessToken");
    refreshToken = login.jsonPath().getString("refreshToken");
    String idempotencyKey = "e2e:" + runId;
    Map<String, Object> request = shipmentRequest(customerId, scenario);
    String correlationId = UUID.randomUUID().toString();
    Response creation = createShipment(request, idempotencyKey, correlationId, accessToken);
    creation.then().statusCode(201);
    Response replay = createShipment(request, idempotencyKey, correlationId, accessToken);
    replay.then().statusCode(201);
    String shipmentId = creation.jsonPath().getString("id");
    String trackingNumber = creation.jsonPath().getString("trackingNumber");
    Response byId = get(shipmentUrl, "/api/v1/shipments/" + shipmentId, accessToken);
    byId.then().statusCode(200);
    Response tracked = get(shipmentUrl, "/api/v1/tracking/" + trackingNumber, null);
    tracked.then().statusCode(200);
    return new JourneyResult(
        email,
        customerId,
        accessToken,
        refreshToken,
        idempotencyKey,
        correlationId,
        registration,
        login,
        creation,
        replay,
        byId,
        tracked);
  }

  private Response createShipment(
      Map<String, Object> body, String idempotencyKey, String correlationId, String token) {
    return given()
        .baseUri(shipmentUrl.toString())
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + token)
        .header("Idempotency-Key", idempotencyKey)
        .header("X-Correlation-ID", correlationId)
        .body(body)
        .post("/api/v1/shipments");
  }

  private static Response post(URI base, String path, Object body) {
    return given().baseUri(base.toString()).contentType(ContentType.JSON).body(body).post(path);
  }

  private static Response get(URI base, String path, String token) {
    var request = given().baseUri(base.toString()).accept(ContentType.JSON);
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return request.get(path);
  }

  private static Map<String, Object> shipmentRequest(String customerId, String scenario) {
    Map<String, Object> address = new LinkedHashMap<>();
    address.put("contactName", "Synthetic Recipient");
    address.put("line1", "42 " + scenario + " Street");
    address.put("line2", null);
    address.put("city", "Sofia");
    address.put("postalCode", "1000");
    address.put("countryCode", "BG");
    address.put("phone", "+359888123456");
    return Map.of(
        "customerId",
        customerId,
        "pickupAddress",
        address,
        "deliveryAddress",
        new LinkedHashMap<>(address),
        "parcel",
        Map.of(
            "weightGrams",
            2500,
            "lengthCm",
            30,
            "widthCm",
            20,
            "heightCm",
            15,
            "description",
            "Synthetic E2E parcel " + scenario),
        "deliveryType",
        "STANDARD");
  }

  /** Revokes the journey's refresh session even when an assertion fails. */
  @Override
  public void close() {
    if (refreshToken != null) {
      post(identityUrl, "/api/v1/auth/logout", Map.of("refreshToken", refreshToken));
      refreshToken = null;
    }
  }

  /** Complete HTTP evidence returned by one journey. */
  public record JourneyResult(
      String email,
      String customerId,
      String accessToken,
      String refreshToken,
      String idempotencyKey,
      String correlationId,
      Response registration,
      Response login,
      Response creation,
      Response replay,
      Response byId,
      Response tracked) {}
}
