package io.urbanflow.testcore.evidence;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.regex.Pattern;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;

/** Captures and masks browser console evidence. */
public final class LogAttachmentService {

  private static final Pattern SENSITIVE =
      Pattern.compile("(?i)(authorization|password|token|secret)=([^&\\s]+)");

  /** Returns chronological masked browser console entries as UTF-8. */
  public byte[] browserConsole(WebDriver driver) {
    String content =
        driver.manage().logs().get(LogType.BROWSER).getAll().stream()
            .sorted(Comparator.comparingLong(org.openqa.selenium.logging.LogEntry::getTimestamp))
            .map(entry -> entry.getLevel() + " " + mask(entry.getMessage()))
            .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
    return content.getBytes(StandardCharsets.UTF_8);
  }

  /** Masks common key-value credential forms before evidence attachment. */
  public String mask(String value) {
    if (value == null) {
      return "";
    }
    return SENSITIVE.matcher(value).replaceAll("$1=[REDACTED]");
  }
}
