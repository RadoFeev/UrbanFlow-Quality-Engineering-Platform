package io.urbanflow.visual;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.urbanflow.testcore.context.TestContext;
import io.urbanflow.testcore.junit.PortalServerExtension;
import io.urbanflow.testcore.junit.UrbanFlowTest;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/** Pixel-based browser regression checks for stable responsive portal entry states. */
@UrbanFlowTest
@Tag("visual")
@Epic("Delivery portal design system")
@Feature("Responsive visual baselines")
@ExtendWith(PortalServerExtension.class)
class PortalVisualRegressionTest {

  private static final int CHANNEL_TOLERANCE = 12;
  private static final double DEFAULT_MAX_DIFF = 0.001;

  @ParameterizedTest(name = "{0} matches baseline at {1}x{2}")
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
  void portalMatchesReviewedBaseline(String portal, int width, int height, TestContext context)
      throws IOException {
    WebDriver driver = context.drivers().current();
    driver.manage().window().setSize(new Dimension(width, height));
    driver.get(portalUrl(portal, context).toString());
    ((JavascriptExecutor) driver)
        .executeScript(
            "document.documentElement.insertAdjacentHTML('beforeend',"
                + "'<style>*{animation:none!important;transition:none!important}</style>')");

    BufferedImage actual =
        ImageIO.read(
            new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
    String baselineName = "%s-%sx%s.png".formatted(portal, width, height);
    Path baseline = baselineDirectory().resolve(baselineName);
    if (Boolean.getBoolean("visual.update")) {
      Files.createDirectories(baseline.getParent());
      ImageIO.write(actual, "png", baseline.toFile());
    }
    assertThat(baseline)
        .as("missing visual baseline; regenerate with -Dvisual.update=true")
        .exists();

    BufferedImage expected = ImageIO.read(baseline.toFile());
    assertThat(actual.getWidth()).as("screenshot width").isEqualTo(expected.getWidth());
    assertThat(actual.getHeight()).as("screenshot height").isEqualTo(expected.getHeight());
    Comparison comparison = compare(expected, actual);
    if (comparison.ratio() > maxDiff()) {
      writeEvidence(baselineName, actual, comparison.diff());
    }
    assertThat(comparison.ratio())
        .as("visual diff ratio for %s; evidence: target/visual-diffs", baselineName)
        .isLessThanOrEqualTo(maxDiff());
  }

  private static Comparison compare(BufferedImage expected, BufferedImage actual) {
    BufferedImage diff =
        new BufferedImage(expected.getWidth(), expected.getHeight(), BufferedImage.TYPE_INT_ARGB);
    long changed = 0;
    for (int y = 0; y < expected.getHeight(); y++) {
      for (int x = 0; x < expected.getWidth(); x++) {
        Color left = new Color(expected.getRGB(x, y), true);
        Color right = new Color(actual.getRGB(x, y), true);
        boolean different =
            channelDelta(left.getRed(), right.getRed())
                || channelDelta(left.getGreen(), right.getGreen())
                || channelDelta(left.getBlue(), right.getBlue())
                || channelDelta(left.getAlpha(), right.getAlpha());
        if (different) {
          changed++;
          diff.setRGB(x, y, 0xFFFF00FF);
        } else {
          diff.setRGB(x, y, 0x22000000);
        }
      }
    }
    double pixels = (double) expected.getWidth() * expected.getHeight();
    return new Comparison(changed / pixels, diff);
  }

  private static boolean channelDelta(int left, int right) {
    return Math.abs(left - right) > CHANNEL_TOLERANCE;
  }

  private static void writeEvidence(String name, BufferedImage actual, BufferedImage diff)
      throws IOException {
    Path evidence = workspace().resolve("visual-tests/target/visual-diffs");
    Files.createDirectories(evidence);
    ImageIO.write(actual, "png", evidence.resolve("actual-" + name).toFile());
    ImageIO.write(diff, "png", evidence.resolve("diff-" + name).toFile());
  }

  private static URI portalUrl(String portal, TestContext context) {
    return switch (portal) {
      case "customer" -> context.environment().customerPortalUrl();
      case "courier" -> context.environment().courierPortalUrl();
      case "operations" -> context.environment().operationsDashboardUrl();
      default -> throw new IllegalArgumentException("unknown portal: " + portal);
    };
  }

  private static Path baselineDirectory() {
    return workspace().resolve("visual-tests/src/test/resources/baselines");
  }

  private static Path workspace() {
    return Path.of(System.getProperty("workspace.root")).toAbsolutePath().normalize();
  }

  private static double maxDiff() {
    return Double.parseDouble(
        System.getProperty("visual.max-diff", String.valueOf(DEFAULT_MAX_DIFF)));
  }

  private record Comparison(double ratio, BufferedImage diff) {}
}
