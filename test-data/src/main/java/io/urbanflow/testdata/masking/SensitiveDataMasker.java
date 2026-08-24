package io.urbanflow.testdata.masking;

import java.util.List;
import java.util.regex.Pattern;

/** Masks synthetic or supplied sensitive values before logs and reports. */
public final class SensitiveDataMasker {

  private static final List<Pattern> PATTERNS =
      List.of(
          Pattern.compile("(?i)(authorization\\s*[:=]\\s*bearer\\s+)[A-Za-z0-9._-]+"),
          Pattern.compile("(?i)(password|refreshToken|accessToken|secret)(\\s*[:=]\\s*)[^,}\\s]+"),
          Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
          Pattern.compile("\\+?[1-9]\\d{7,14}"));

  /** Returns a masked copy suitable for evidence. */
  public String mask(String value) {
    if (value == null) {
      return "";
    }
    String masked = PATTERNS.get(0).matcher(value).replaceAll("$1[REDACTED]");
    masked = PATTERNS.get(1).matcher(masked).replaceAll("$1$2[REDACTED]");
    masked = PATTERNS.get(2).matcher(masked).replaceAll("[EMAIL_REDACTED]");
    return PATTERNS.get(3).matcher(masked).replaceAll("[PHONE_REDACTED]");
  }
}
