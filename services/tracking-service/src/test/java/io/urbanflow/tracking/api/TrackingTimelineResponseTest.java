package io.urbanflow.tracking.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.urbanflow.tracking.domain.TrackingStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrackingTimelineResponseTest {

  @Test
  void rejectsEmptyTimeline() {
    assertThatThrownBy(
            () ->
                new TrackingTimelineResponse(
                    "UF1123456789ABCDEF01",
                    TrackingStatus.CREATED,
                    List.of(),
                    "corr-response-001",
                    "0123456789abcdef0123456789abcdef"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("events");
  }
}
