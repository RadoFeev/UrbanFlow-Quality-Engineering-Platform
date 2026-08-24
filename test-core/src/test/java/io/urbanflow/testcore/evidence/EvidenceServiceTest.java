package io.urbanflow.testcore.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

class EvidenceServiceTest {

  @Test
  void masksCredentialsEmailsAndPhones() {
    String masked =
        new LogAttachmentService()
            .mask("authorization=Bearer.Token password=secret user@example.test +359880000001");

    assertThat(masked)
        .contains("authorization=[REDACTED]", "password=[REDACTED]")
        .doesNotContain("Bearer.Token", "secret");
  }

  @Test
  void capturesScreenshotBytes() {
    WebDriver driver =
        mock(WebDriver.class, Mockito.withSettings().extraInterfaces(TakesScreenshot.class));
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES))
        .thenReturn(new byte[] {1, 2, 3});

    assertThat(new ScreenshotService().screenshot(driver)).containsExactly(1, 2, 3);
  }

  @Test
  void capturesPageSourceAsUtf8() {
    WebDriver driver = mock(WebDriver.class);
    when(driver.getPageSource()).thenReturn("<main>✓</main>");

    assertThat(new String(new ScreenshotService().pageSource(driver), StandardCharsets.UTF_8))
        .isEqualTo("<main>✓</main>");
  }

  @Test
  void rejectsNonScreenshotDriver() {
    assertThatThrownBy(() -> new ScreenshotService().screenshot(mock(WebDriver.class)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsUnsafeVideoSchemeBeforeNetworkCall() {
    VideoEvidenceService service =
        new VideoEvidenceService(
            java.net.http.HttpClient.newHttpClient(), java.time.Duration.ofSeconds(1));
    assertThatThrownBy(() -> service.download(java.net.URI.create("file:///video.mp4")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
