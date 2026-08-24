package io.urbanflow.testcore.evidence;

import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Adds consistently named binary and textual evidence to the active Allure test. */
public final class AllureAttachmentService {

  /** Attaches UTF-8 text. */
  public void text(String name, String content) {
    Allure.addAttachment(name, "text/plain", content, ".txt");
  }

  /** Attaches JSON content. */
  public void json(String name, String content) {
    Allure.addAttachment(name, "application/json", content, ".json");
  }

  /** Attaches a PNG image. */
  public void png(String name, byte[] content) {
    Allure.addAttachment(name, "image/png", new ByteArrayInputStream(content), ".png");
  }

  /** Attaches arbitrary bytes with explicit media type and extension. */
  public void binary(String name, String mediaType, String extension, byte[] content) {
    Allure.addAttachment(name, mediaType, new ByteArrayInputStream(content), extension);
  }

  /** Converts UTF-8 evidence bytes to text for callers that captured raw output. */
  public String utf8(byte[] content) {
    return new String(content, StandardCharsets.UTF_8);
  }
}
