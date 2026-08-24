package io.urbanflow.testcore.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Isolated UTF-8 Redis client for cache, TTL, counter, and cleanup assertions. */
public final class RedisTestClient implements AutoCloseable {

  private final RedisClient client;
  private final StatefulRedisConnection<String, String> connection;

  /** Opens a Redis connection with a bounded command timeout. */
  public RedisTestClient(RedisURI uri, Duration timeout) {
    Objects.requireNonNull(uri);
    this.client = RedisClient.create(uri);
    this.connection = client.connect();
    this.connection.setTimeout(timeout);
  }

  /** Stores a value with a positive TTL. */
  public void set(String key, String value, Duration ttl) {
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("Redis TTL must be positive");
    }
    connection.sync().set(key, value, SetArgs.Builder.ex(ttl));
  }

  /** Returns a cached value when present. */
  public Optional<String> get(String key) {
    return Optional.ofNullable(connection.sync().get(key));
  }

  /** Returns remaining TTL in seconds, preserving Redis negative sentinel values. */
  public long ttl(String key) {
    return connection.sync().ttl(key);
  }

  /** Atomically increments a counter. */
  public long increment(String key) {
    return connection.sync().incr(key);
  }

  /** Deletes test-owned keys. */
  public long delete(String... keys) {
    return connection.sync().del(keys);
  }

  /** Closes the connection and event-loop resources. */
  @Override
  public void close() {
    connection.close();
    client.shutdown();
  }
}
