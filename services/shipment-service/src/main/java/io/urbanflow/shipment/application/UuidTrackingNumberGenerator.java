package io.urbanflow.shipment.application;

import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Generates tracking numbers from cryptographically unpredictable UUID values. */
@Component
public final class UuidTrackingNumberGenerator implements TrackingNumberGenerator {

  @Override
  public String next() {
    String token = UUID.randomUUID().toString().replace("-", "");
    return "UF" + token.substring(0, 18).toUpperCase(Locale.ROOT);
  }
}
