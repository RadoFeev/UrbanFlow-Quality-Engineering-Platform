package io.urbanflow.testcore.evidence;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Downloads bounded Selenium Grid video evidence over HTTP. */
public final class VideoEvidenceService {

  private static final int MAXIMUM_BYTES = 100 * 1024 * 1024;
  private final HttpClient client;
  private final Duration timeout;

  /** Creates the video downloader. */
  public VideoEvidenceService(HttpClient client, Duration timeout) {
    this.client = client;
    this.timeout = timeout;
  }

  /** Downloads one MP4 recording with scheme, status, and size validation. */
  public byte[] download(URI videoUrl) {
    if (videoUrl == null
        || !("http".equalsIgnoreCase(videoUrl.getScheme())
            || "https".equalsIgnoreCase(videoUrl.getScheme()))) {
      throw new IllegalArgumentException("video URL must use HTTP or HTTPS");
    }
    try {
      HttpRequest request = HttpRequest.newBuilder(videoUrl).timeout(timeout).GET().build();
      HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() != 200) {
        throw new EvidenceException("video endpoint returned status " + response.statusCode());
      }
      if (response.body().length > MAXIMUM_BYTES) {
        throw new EvidenceException("video evidence exceeds 100 MB");
      }
      return response.body();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new EvidenceException("video download was interrupted", exception);
    } catch (java.io.IOException exception) {
      throw new EvidenceException("video download failed", exception);
    }
  }

  /** Normalizes evidence transport failures. */
  public static class EvidenceException extends RuntimeException {

    /** Creates an evidence failure. */
    public EvidenceException(String message) {
      super(message);
    }

    /** Creates an evidence failure with cause. */
    public EvidenceException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
