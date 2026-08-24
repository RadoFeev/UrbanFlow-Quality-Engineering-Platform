package io.urbanflow.identity.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.urbanflow.identity.application.AccountUnavailableException;
import io.urbanflow.identity.application.AuthService;
import io.urbanflow.identity.application.AuthTokens;
import io.urbanflow.identity.application.DuplicateEmailException;
import io.urbanflow.identity.application.InvalidCredentialsException;
import io.urbanflow.identity.application.UserProfile;
import io.urbanflow.identity.application.WeakPasswordException;
import io.urbanflow.identity.domain.AccountStatus;
import io.urbanflow.identity.domain.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Tag("api")
class IdentityHttpAdapterApiTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");
  private static final String VALID_EMAIL = "customer+api@example.test";
  private AuthService authService;

  @BeforeEach
  void configureHttpAdapter() {
    authService = mock(AuthService.class);
    MockMvc mockMvc =
        MockMvcBuilders.standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new IdentityExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC)))
            .addFilters(new RequestTracingFilter())
            .build();
    RestAssuredMockMvc.mockMvc(mockMvc);
  }

  @AfterEach
  void resetRestAssured() {
    RestAssuredMockMvc.reset();
  }

  @Test
  void registrationReturnsSafeProfileAndNoStore() {
    UUID userId = UUID.fromString("8b15eaa6-24d4-48bf-93c0-fb3b5019a608");
    when(authService.register(VALID_EMAIL, "Synthetic Customer", "Valid-Test-42!"))
        .thenReturn(
            new UserProfile(
                userId, VALID_EMAIL, "Synthetic Customer", Role.CUSTOMER, AccountStatus.ACTIVE, 0));

    given()
        .contentType(ContentType.JSON)
        .body(registration(VALID_EMAIL, "Synthetic Customer", "Valid-Test-42!"))
        .when()
        .post("/api/v1/auth/register")
        .then()
        .statusCode(201)
        .header("Cache-Control", equalTo("no-store"))
        .body("id", equalTo(userId.toString()))
        .body("role", equalTo("CUSTOMER"))
        .body("password", equalTo(null));
  }

  @Test
  void loginReturnsBearerCredentialsAndNoStore() {
    when(authService.login(VALID_EMAIL, "Valid-Test-42!"))
        .thenReturn(AuthTokens.bearer("access-token", "refresh-token", NOW.plusSeconds(900)));

    given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"" + VALID_EMAIL + "\",\"password\":\"Valid-Test-42!\"}")
        .when()
        .post("/api/v1/auth/login")
        .then()
        .statusCode(200)
        .header("Cache-Control", equalTo("no-store"))
        .body("tokenType", equalTo("Bearer"))
        .body("accessToken", equalTo("access-token"))
        .body("refreshToken", equalTo("refresh-token"));
  }

  @Test
  void refreshRotatesTheOpaqueCredential() {
    when(authService.refresh("refresh-token-old"))
        .thenReturn(AuthTokens.bearer("access-new", "refresh-new", NOW.plusSeconds(900)));
    given()
        .contentType(ContentType.JSON)
        .body("{\"refreshToken\":\"refresh-token-old\"}")
        .when()
        .post("/api/v1/auth/refresh")
        .then()
        .statusCode(200)
        .body("accessToken", equalTo("access-new"))
        .body("refreshToken", equalTo("refresh-new"));
  }

  @Test
  void logoutIsIdempotentAndHasNoResponseBody() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"refreshToken\":\"refresh-token\"}")
        .when()
        .post("/api/v1/auth/logout")
        .then()
        .statusCode(204)
        .header("Cache-Control", equalTo("no-store"));
    verify(authService).logout("refresh-token");
  }

  @Test
  void passwordResetRequestDoesNotRevealAccountExistence() {
    when(authService.requestPasswordReset(anyString())).thenReturn(Optional.empty());
    given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"missing@example.test\"}")
        .when()
        .post("/api/v1/auth/password-reset/request")
        .then()
        .statusCode(202)
        .header("Cache-Control", equalTo("no-store"));
  }

  @Test
  void passwordResetConfirmationConsumesTheToken() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"resetToken\":\"reset-token\",\"newPassword\":\"New-Test-42!\"}")
        .when()
        .post("/api/v1/auth/password-reset/confirm")
        .then()
        .statusCode(204);
    verify(authService).resetPassword("reset-token", "New-Test-42!");
  }

  @ParameterizedTest(name = "invalid registration partition {index}")
  @MethodSource("invalidRegistrations")
  void registrationRejectsInvalidEquivalencePartitions(String body) {
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/api/v1/auth/register")
        .then()
        .statusCode(400)
        .body("code", equalTo("invalid_request"))
        .body("status", equalTo(400));
    verifyNoInteractions(authService);
  }

  @Test
  void malformedJsonUsesTheStableErrorEnvelope() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"email\":")
        .when()
        .post("/api/v1/auth/register")
        .then()
        .statusCode(400)
        .body("code", equalTo("invalid_request"))
        .body("message", equalTo("request body is malformed"))
        .body("timestamp", equalTo(NOW.toString()));
  }

  @Test
  void duplicateRegistrationReturnsConflict() {
    when(authService.register(anyString(), anyString(), anyString()))
        .thenThrow(new DuplicateEmailException());
    given()
        .contentType(ContentType.JSON)
        .body(registration(VALID_EMAIL, "Synthetic Customer", "Valid-Test-42!"))
        .when()
        .post("/api/v1/auth/register")
        .then()
        .statusCode(409)
        .body("code", equalTo("email_conflict"));
  }

  @Test
  void invalidCredentialsAreEnumerationResistant() {
    when(authService.login(anyString(), anyString())).thenThrow(new InvalidCredentialsException());
    given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"missing@example.test\",\"password\":\"Wrong-Test-42!\"}")
        .when()
        .post("/api/v1/auth/login")
        .then()
        .statusCode(401)
        .header("WWW-Authenticate", equalTo("Bearer"))
        .body("code", equalTo("authentication_failed"));
  }

  @Test
  void lockedAccountUsesHttpLockedWithoutStateDisclosure() {
    when(authService.login(anyString(), anyString())).thenThrow(new AccountUnavailableException());
    given()
        .contentType(ContentType.JSON)
        .body("{\"email\":\"locked@example.test\",\"password\":\"Valid-Test-42!\"}")
        .when()
        .post("/api/v1/auth/login")
        .then()
        .statusCode(423)
        .body("code", equalTo("account_unavailable"));
  }

  @Test
  void weakPasswordUsesUnprocessableContent() {
    when(authService.register(anyString(), anyString(), anyString()))
        .thenThrow(new WeakPasswordException("password policy is not satisfied"));
    given()
        .contentType(ContentType.JSON)
        .body(registration(VALID_EMAIL, "Synthetic Customer", "weak"))
        .when()
        .post("/api/v1/auth/register")
        .then()
        .statusCode(422)
        .body("code", equalTo("weak_password"));
  }

  @Test
  void unsupportedContentTypeIsRejectedBeforeTheApplicationLayer() {
    given()
        .contentType(ContentType.TEXT)
        .body("not-json")
        .when()
        .post("/api/v1/auth/login")
        .then()
        .statusCode(415);
    verifyNoInteractions(authService);
  }

  @Test
  void unsupportedHttpMethodIsRejected() {
    given().when().delete("/api/v1/auth/login").then().statusCode(405);
  }

  @Test
  void validCorrelationIdentifierIsPropagatedToResponseAndError() {
    given()
        .header("X-Correlation-ID", "api-case-0001")
        .contentType(ContentType.JSON)
        .body("{\"email\":")
        .when()
        .post("/api/v1/auth/register")
        .then()
        .statusCode(400)
        .header("X-Correlation-ID", equalTo("api-case-0001"))
        .body("correlationId", equalTo("api-case-0001"));
  }

  @Test
  void invalidCorrelationIdentifierIsSafelyReplaced() {
    given()
        .header("X-Correlation-ID", "bad header value")
        .contentType(ContentType.JSON)
        .body("{\"email\":")
        .when()
        .post("/api/v1/auth/register")
        .then()
        .statusCode(400)
        .header("X-Correlation-ID", matchesPattern("[0-9a-f-]{36}"))
        .body("correlationId", matchesPattern("[0-9a-f-]{36}"));
  }

  @Test
  void validTraceparentIdentifierIsPropagated() {
    String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
    given()
        .header("traceparent", "00-" + traceId + "-00f067aa0ba902b7-01")
        .contentType(ContentType.JSON)
        .body("{\"email\":")
        .when()
        .post("/api/v1/auth/register")
        .then()
        .statusCode(400)
        .header("X-Trace-ID", equalTo(traceId))
        .body("traceId", equalTo(traceId));
  }

  private static Stream<Arguments> invalidRegistrations() {
    return Stream.of(
        Arguments.of(registration("", "Synthetic Customer", "Valid-Test-42!")),
        Arguments.of(registration("not-an-email", "Synthetic Customer", "Valid-Test-42!")),
        Arguments.of(registration(VALID_EMAIL, "", "Valid-Test-42!")),
        Arguments.of(registration(VALID_EMAIL, "Synthetic Customer", "")),
        Arguments.of("{}"),
        Arguments.of(registration(VALID_EMAIL, " ", "Valid-Test-42!")),
        Arguments.of(registration("@example.test", "Synthetic Customer", "Valid-Test-42!")),
        Arguments.of(registration(VALID_EMAIL, "Synthetic Customer", " ")));
  }

  private static String registration(String email, String name, String password) {
    return "{\"email\":\""
        + email
        + "\",\"displayName\":\""
        + name
        + "\",\"password\":\""
        + password
        + "\"}";
  }
}
