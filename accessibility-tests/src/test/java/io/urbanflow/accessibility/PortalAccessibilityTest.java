package io.urbanflow.accessibility;

import static org.assertj.core.api.Assertions.assertThat;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.urbanflow.testcore.context.TestContext;
import io.urbanflow.testcore.junit.PortalServerExtension;
import io.urbanflow.testcore.junit.UrbanFlowTest;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/** Runs the axe-core WCAG gate for every portal across supported responsive breakpoints. */
@UrbanFlowTest
@Tag("accessibility")
@Epic("Inclusive delivery experience")
@Feature("WCAG 2.2 AA automated gate")
@ExtendWith(PortalServerExtension.class)
class PortalAccessibilityTest {

  @ParameterizedTest(name = "{0} has no critical/serious axe violations at {1}x{2}")
  @CsvSource({
    "customer,375,812",
    "customer,768,1024",
    "customer,1024,768",
    "customer,1440,1000",
    "customer,1920,1080",
    "courier,375,812",
    "courier,768,1024",
    "courier,1024,768",
    "courier,1440,1000",
    "courier,1920,1080",
    "operations,375,812",
    "operations,768,1024",
    "operations,1024,768",
    "operations,1440,1000",
    "operations,1920,1080"
  })
  void responsivePortalHasNoBlockingViolations(
      String portal, int width, int height, TestContext context) {
    WebDriver driver = context.drivers().current();
    driver.manage().window().setSize(new Dimension(width, height));
    driver.get(portalUrl(portal, context).toString());
    ((JavascriptExecutor) driver)
        .executeScript(
            "document.documentElement.insertAdjacentHTML('beforeend',"
                + "'<style>*{animation:none!important;transition:none!important}</style>')");

    Results results =
        new AxeBuilder()
            .withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa"))
            .analyze(driver);
    List<Rule> blockers =
        results.getViolations().stream()
            .filter(violation -> isBlocking(violation.getImpact()))
            .toList();

    assertThat(blockers)
        .as("axe violations for %s at %sx%s: %s", portal, width, height, blockers)
        .isEmpty();
  }

  private static URI portalUrl(String portal, TestContext context) {
    return switch (portal) {
      case "customer" -> context.environment().customerPortalUrl();
      case "courier" -> context.environment().courierPortalUrl();
      case "operations" -> context.environment().operationsDashboardUrl();
      default -> throw new IllegalArgumentException("unknown portal: " + portal);
    };
  }

  private static boolean isBlocking(String impact) {
    String normalized = impact == null ? "" : impact.toLowerCase(Locale.ROOT);
    return normalized.equals("critical") || normalized.equals("serious");
  }
}
