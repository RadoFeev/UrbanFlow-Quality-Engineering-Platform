package io.urbanflow.ui;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.urbanflow.testcore.context.TestContext;
import io.urbanflow.testcore.junit.UrbanFlowTest;
import io.urbanflow.ui.page.CustomerPortalPage;
import io.urbanflow.ui.support.PortalServerExtension;
import io.urbanflow.ui.support.PortalStateFixtures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Browser acceptance coverage for customer identity, session, and preference boundaries. */
@UrbanFlowTest
@Tag("ui")
@Epic("Customer delivery portal")
@Feature("Identity and session lifecycle")
@ExtendWith(PortalServerExtension.class)
class CustomerAuthenticationUiTest {

  @Test
  void customerCanRegisterWithTheSecurityPolicy(TestContext context) {
    CustomerPortalPage page = fresh(context);

    page.register("Quality Customer", "quality.customer@example.test", "Strong!Quality25")
        .awaitText("registration-message", "Account created");

    assertThat(page.idText("registration-message")).contains("Account created");
    assertThat(page.fieldValue("register-email")).isEmpty();
  }

  @ParameterizedTest(name = "registration rejects identity: {0}")
  @ValueSource(strings = {"missing-name", "invalid-email"})
  void registrationRejectsInvalidIdentity(String boundary, TestContext context) {
    CustomerPortalPage page = fresh(context);
    String name = boundary.equals("missing-name") ? " " : "Quality Customer";
    String email = boundary.equals("invalid-email") ? "not-an-email" : "valid@example.test";

    page.register(name, email, "Strong!Quality25");

    assertThat(page.idText("registration-message")).isEqualTo("Enter a valid name and email.");
  }

  @ParameterizedTest(name = "registration rejects weak password: {0}")
  @ValueSource(strings = {"short", "alllowercase123!", "NOLOWERCASE123!"})
  void registrationRejectsWeakPasswords(String password, TestContext context) {
    CustomerPortalPage page = fresh(context);

    page.register("Quality Customer", "quality.customer@example.test", password);

    assertThat(page.idText("registration-message"))
        .isEqualTo("Password does not meet the security policy.");
  }

  @Test
  void duplicateRegistrationIsRejected(TestContext context) {
    Map<String, Object> state =
        PortalStateFixtures.customer(false, false, System.currentTimeMillis());
    CustomerPortalPage page = fresh(context).state(state);

    page.register("Duplicate Customer", customerEmail(state), PortalStateFixtures.PASSWORD)
        .awaitText("registration-message", "already exists");

    assertThat(page.idText("registration-message")).contains("already exists");
  }

  @Test
  void validCustomerCanSignIn(TestContext context) {
    Map<String, Object> state =
        PortalStateFixtures.customer(false, false, System.currentTimeMillis());
    CustomerPortalPage page = fresh(context).state(state);

    page.login(customerEmail(state), PortalStateFixtures.PASSWORD).awaitView("create");

    assertThat(page.viewActive("create")).isTrue();
    assertThat(page.displayed("logout-button")).isTrue();
    assertThat(page.storage(PortalStateFixtures.CUSTOMER_STORE)).contains("\"role\":\"CUSTOMER\"");
  }

  @ParameterizedTest(name = "login rejects credential boundary: {0}")
  @ValueSource(strings = {"wrong-password", "unknown-user", "missing-credentials"})
  void invalidCredentialsDoNotCreateSession(String boundary, TestContext context) {
    Map<String, Object> state =
        PortalStateFixtures.customer(false, false, System.currentTimeMillis());
    CustomerPortalPage page = fresh(context).state(state);
    String email = boundary.equals("unknown-user") ? "unknown@example.test" : customerEmail(state);
    String password =
        boundary.equals("wrong-password") ? "Wrong!Password25" : PortalStateFixtures.PASSWORD;
    if (boundary.equals("missing-credentials")) {
      email = "";
      password = "";
    }

    page.login(email, password);

    assertThat(page.idText("login-message"))
        .isIn("The email or password is incorrect.", "Email and password are required.");
    assertThat(page.viewActive("auth")).isTrue();
  }

  @Test
  void accountLocksAfterFiveFailedAttempts(TestContext context) {
    Map<String, Object> state =
        PortalStateFixtures.customer(false, false, System.currentTimeMillis());
    CustomerPortalPage page = fresh(context).state(state);
    String email = customerEmail(state);

    for (int attempt = 0; attempt < 5; attempt++) {
      page.login(email, "Wrong!Password25").awaitText("login-message", "incorrect");
    }
    page.login(email, PortalStateFixtures.PASSWORD).awaitText("login-message", "locked");

    assertThat(page.idText("login-message")).contains("temporarily locked");
  }

  @Test
  void authenticatedCustomerCanSignOut(TestContext context) {
    CustomerPortalPage page = fresh(context).state(PortalStateFixtures.customer());

    page.logout();

    assertThat(page.viewActive("auth")).isTrue();
    assertThat(page.toast().activeText()).isEqualTo("Signed out.");
  }

  @Test
  void expiredSessionReturnsCustomerToSignIn(TestContext context) {
    long expired = System.currentTimeMillis() - java.time.Duration.ofMinutes(31).toMillis();
    CustomerPortalPage page =
        fresh(context).state(PortalStateFixtures.customer(true, false, expired));

    page.view("profile");

    assertThat(page.viewActive("auth")).isTrue();
    assertThat(page.idText("login-message")).isEqualTo("Sign in to continue.");
  }

  @Test
  void anonymousCustomerCannotOpenProtectedViews(TestContext context) {
    CustomerPortalPage page = fresh(context);

    page.view("shipments");

    assertThat(page.viewActive("auth")).isTrue();
    assertThat(page.idText("login-message")).isEqualTo("Sign in to continue.");
  }

  @ParameterizedTest(name = "password reset is enumeration-safe for {0}")
  @ValueSource(strings = {"known.customer@example.test", "unknown.customer@example.test"})
  void passwordResetDoesNotRevealAccountExistence(String email, TestContext context) {
    CustomerPortalPage page = fresh(context);

    page.reset(email);

    assertThat(page.idText("reset-message"))
        .isEqualTo("If the account exists, reset instructions have been sent.");
  }

  @Test
  void passwordResetRejectsMalformedEmail(TestContext context) {
    CustomerPortalPage page = fresh(context);

    page.reset("malformed-address");

    assertThat(page.idText("reset-message")).isEqualTo("Enter a valid email.");
  }

  @ParameterizedTest(name = "customer can switch language to {0}")
  @ValueSource(strings = {"bg", "en"})
  void languagePreferenceIsApplied(String language, TestContext context) {
    CustomerPortalPage page = fresh(context).state(PortalStateFixtures.customer());

    page.language(language);

    assertThat(page.pageLanguage()).isEqualTo(language);
    assertThat(page.storage(PortalStateFixtures.CUSTOMER_STORE))
        .contains("\"language\":\"" + language);
  }

  @Test
  void customerCanToggleDarkTheme(TestContext context) {
    CustomerPortalPage page = fresh(context);

    page.toggleTheme();

    assertThat(page.theme()).isEqualTo("dark");
    assertThat(page.attribute("theme-toggle", "aria-pressed")).isEqualTo("true");
  }

  @Test
  void corruptBrowserStateFallsBackToSafeAnonymousState(TestContext context) {
    CustomerPortalPage page =
        fresh(context).rawState(PortalStateFixtures.CUSTOMER_STORE, "{not-json");

    assertThat(page.viewActive("auth")).isTrue();
    assertThat(page.displayed("logout-button")).isFalse();
  }

  private static CustomerPortalPage fresh(TestContext context) {
    return new CustomerPortalPage(
            context.drivers().current(),
            context.environment().customerPortalUrl(),
            context.environment().timeout())
        .openFresh();
  }

  @SuppressWarnings("unchecked")
  private static String customerEmail(Map<String, Object> state) {
    return String.valueOf(((List<Map<String, Object>>) state.get("users")).getFirst().get("email"));
  }
}
