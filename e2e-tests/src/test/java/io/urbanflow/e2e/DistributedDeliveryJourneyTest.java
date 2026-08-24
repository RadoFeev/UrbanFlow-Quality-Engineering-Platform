package io.urbanflow.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.urbanflow.e2e.support.DeployedJourneyClient;
import io.urbanflow.e2e.support.DeployedJourneyClient.JourneyResult;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Twenty unique deployed-stack journeys with automatic session cleanup. */
@Tag("e2e")
@Epic("Distributed delivery journey")
@Feature("Identity-to-shipment consistency")
class DistributedDeliveryJourneyTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("journeys")
  void verifiesDistributedInvariant(String name, Consumer<JourneyResult> assertion) {
    Assumptions.assumeTrue(Boolean.getBoolean("e2e.enabled"), "requires deployed disposable stack");
    URI identity = URI.create(System.getProperty("identity.url", "http://localhost:8081"));
    URI shipment = URI.create(System.getProperty("shipment.url", "http://localhost:8082"));
    try (DeployedJourneyClient client = new DeployedJourneyClient(identity, shipment)) {
      assertion.accept(client.execute(name.replace(' ', '-')));
    }
  }

  private static Stream<Arguments> journeys() {
    return Stream.of(
        Arguments.of("01 customer identity", check(r -> uuid(r.customerId()))),
        Arguments.of("02 canonical email", check(r -> r.email().endsWith("@example.test"))),
        Arguments.of("03 access token", check(r -> !r.accessToken().isBlank())),
        Arguments.of("04 refresh token", check(r -> !r.refreshToken().isBlank())),
        Arguments.of(
            "05 registration no-store",
            check(r -> noStore(r.registration().header("Cache-Control")))),
        Arguments.of("06 login no-store", check(r -> noStore(r.login().header("Cache-Control")))),
        Arguments.of(
            "07 shipment identity", check(r -> uuid(r.creation().jsonPath().getString("id")))),
        Arguments.of(
            "08 tracking format",
            check(
                r ->
                    r.creation().jsonPath().getString("trackingNumber").matches("UF[0-9A-F]{18}"))),
        Arguments.of(
            "09 initial status",
            check(r -> "CREATED".equals(r.creation().jsonPath().getString("status")))),
        Arguments.of(
            "10 initial version",
            check(r -> Integer.valueOf(0).equals(r.creation().jsonPath().getInt("version")))),
        Arguments.of("11 entity tag", check(r -> r.creation().header("ETag") != null)),
        Arguments.of(
            "12 correlation propagation",
            check(r -> r.correlationId().equals(r.creation().header("X-Correlation-ID")))),
        Arguments.of("13 trace propagation", check(r -> r.creation().header("X-Trace-ID") != null)),
        Arguments.of("14 idempotent identity", check(r -> same(r, "id"))),
        Arguments.of("15 idempotent tracking", check(r -> same(r, "trackingNumber"))),
        Arguments.of(
            "16 read-after-write",
            check(
                r ->
                    r.creation()
                        .jsonPath()
                        .getString("id")
                        .equals(r.byId().jsonPath().getString("id")))),
        Arguments.of(
            "17 tracking consistency",
            check(
                r ->
                    r.creation()
                        .jsonPath()
                        .getString("id")
                        .equals(r.tracked().jsonPath().getString("id")))),
        Arguments.of(
            "18 customer ownership",
            check(r -> r.customerId().equals(r.byId().jsonPath().getString("customerId")))),
        Arguments.of(
            "19 parcel consistency",
            check(
                r ->
                    Integer.valueOf(2500)
                        .equals(r.byId().jsonPath().getInt("parcel.weightGrams")))),
        Arguments.of(
            "20 timestamp contract",
            check(r -> instant(r.creation().jsonPath().getString("createdAt")))));
  }

  private static Consumer<JourneyResult> check(
      java.util.function.Predicate<JourneyResult> predicate) {
    return result -> assertThat(predicate.test(result)).isTrue();
  }

  private static boolean same(JourneyResult result, String path) {
    return result
        .creation()
        .jsonPath()
        .getString(path)
        .equals(result.replay().jsonPath().getString(path));
  }

  private static boolean noStore(String value) {
    return value != null && value.contains("no-store");
  }

  private static boolean uuid(String value) {
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private static boolean instant(String value) {
    try {
      Instant.parse(value);
      return true;
    } catch (java.time.format.DateTimeParseException exception) {
      return false;
    }
  }
}
