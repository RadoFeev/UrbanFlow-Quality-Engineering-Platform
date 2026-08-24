package io.urbanflow.tracking.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.urbanflow.tracking.application.TrackingTimelineItem;
import io.urbanflow.tracking.domain.TrackingStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RedisTrackingTimelineCacheTest {

  private static final String TRACKING_NUMBER = "UF1123456789ABCDEF01";
  private static final String KEY = "tracking:timeline:" + TRACKING_NUMBER;

  @Mock private StringRedisTemplate redis;
  @Mock private ValueOperations<String, String> values;

  private ObjectMapper objectMapper;
  private SimpleMeterRegistry meterRegistry;
  private RedisTrackingTimelineCache cache;

  @BeforeEach
  void setUp() {
    objectMapper = JsonMapper.builder().findAndAddModules().build();
    meterRegistry = new SimpleMeterRegistry();
    when(redis.opsForValue()).thenReturn(values);
    cache =
        new RedisTrackingTimelineCache(
            redis,
            objectMapper,
            new TrackingCacheProperties("tracking:timeline:", Duration.ofSeconds(30)),
            meterRegistry);
  }

  @Test
  void returnsMissWhenKeyIsAbsent() {
    when(values.get(KEY)).thenReturn(null);

    assertThat(cache.get(TRACKING_NUMBER)).isEmpty();
    assertThat(metric("miss")).isEqualTo(1.0);
  }

  @Test
  void deserializesImmutableTimelineOnHit() throws Exception {
    when(values.get(KEY)).thenReturn(objectMapper.writeValueAsString(List.of(item())));

    List<TrackingTimelineItem> result = cache.get(TRACKING_NUMBER).orElseThrow();

    assertThat(result).containsExactly(item());
    assertThat(metric("hit")).isEqualTo(1.0);
  }

  @Test
  void fallsBackAndDeletesCorruptCachedJson() {
    when(values.get(KEY)).thenReturn("not-json");

    assertThat(cache.get(TRACKING_NUMBER)).isEmpty();

    verify(redis).delete(KEY);
    assertThat(metric("fallback")).isEqualTo(1.0);
  }

  @Test
  void fallsBackWhenRedisReadFails() {
    when(values.get(KEY)).thenThrow(new IllegalStateException("redis unavailable"));

    assertThat(cache.get(TRACKING_NUMBER)).isEmpty();
    assertThat(metric("fallback")).isEqualTo(1.0);
  }

  @Test
  void serializesTimelineWithConfiguredTtl() throws Exception {
    cache.put(TRACKING_NUMBER, List.of(item()));

    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(values).set(eq(KEY), json.capture(), eq(Duration.ofSeconds(30)));
    assertThat(objectMapper.readTree(json.getValue()).get(0).get("status").asString())
        .isEqualTo("CONFIRMED");
  }

  @Test
  void containsRedisWriteAndEvictionFailures() {
    doThrow(new IllegalStateException("redis unavailable"))
        .when(values)
        .set(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Duration.class));
    doThrow(new IllegalStateException("redis unavailable")).when(redis).delete(KEY);

    cache.put(TRACKING_NUMBER, List.of(item()));
    cache.evict(TRACKING_NUMBER);

    assertThat(metric("fallback")).isEqualTo(2.0);
  }

  private double metric(String result) {
    return meterRegistry
        .get("urbanflow.tracking.cache.operations")
        .tag("result", result)
        .counter()
        .count();
  }

  private static TrackingTimelineItem item() {
    return new TrackingTimelineItem(
        UUID.fromString("e213efc2-99d4-4a59-b19e-7376553a4f70"),
        TrackingStatus.CONFIRMED,
        1,
        Instant.parse("2026-08-24T12:15:00Z"),
        "corr-tracking-004",
        "00112233445566778899aabbccddeeff");
  }
}
