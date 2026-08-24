package io.urbanflow.testcore.observability;

import java.security.SecureRandom;
import java.util.HexFormat;

/** Thread-confined W3C trace identifier lifecycle. */
public final class TraceIdManager {

  private final SecureRandom random;
  private final ThreadLocal<String> current = new ThreadLocal<>();

  /** Creates a manager backed by a cryptographically strong generator. */
  public TraceIdManager() {
    this(new SecureRandom());
  }

  /** Creates a manager with an injectable generator. */
  public TraceIdManager(SecureRandom random) {
    this.random = random;
  }

  /** Returns the current 32-character trace ID, creating one when absent. */
  public String current() {
    String value = current.get();
    if (value == null) {
      byte[] bytes = new byte[16];
      do {
        random.nextBytes(bytes);
        value = HexFormat.of().formatHex(bytes);
      } while (value.chars().allMatch(character -> character == '0'));
      current.set(value);
    }
    return value;
  }

  /** Removes the current thread value after a test. */
  public void clear() {
    current.remove();
  }
}
