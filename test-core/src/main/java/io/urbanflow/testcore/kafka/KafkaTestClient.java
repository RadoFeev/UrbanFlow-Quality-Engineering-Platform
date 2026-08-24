package io.urbanflow.testcore.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

/** Test-owned Kafka producer/consumer client with broker acknowledgment and bounded polls. */
public final class KafkaTestClient<K, V> implements AutoCloseable {

  private final Producer<K, V> producer;
  private final Consumer<K, V> consumer;
  private final Duration acknowledgmentTimeout;

  /** Creates the client from independently configured Kafka clients. */
  public KafkaTestClient(
      Producer<K, V> producer, Consumer<K, V> consumer, Duration acknowledgmentTimeout) {
    this.producer = Objects.requireNonNull(producer);
    this.consumer = Objects.requireNonNull(consumer);
    this.acknowledgmentTimeout = Objects.requireNonNull(acknowledgmentTimeout);
  }

  /** Subscribes the test consumer to the supplied topics. */
  public void subscribe(String... topics) {
    consumer.subscribe(List.of(topics));
  }

  /** Publishes a record and waits for broker acknowledgment. */
  public void publish(ProducerRecord<K, V> record) {
    try {
      producer
          .send(record)
          .get(acknowledgmentTimeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new KafkaTestException("Kafka publication was interrupted", exception);
    } catch (ExecutionException | java.util.concurrent.TimeoutException exception) {
      throw new KafkaTestException("Kafka publication was not acknowledged", exception);
    }
  }

  /** Polls a bounded batch and returns immutable records. */
  public List<ConsumerRecord<K, V>> poll(Duration timeout) {
    ConsumerRecords<K, V> records = consumer.poll(timeout);
    List<ConsumerRecord<K, V>> values = new ArrayList<>();
    records.forEach(values::add);
    return List.copyOf(values);
  }

  /** Commits processed offsets synchronously. */
  public void commit() {
    consumer.commitSync();
  }

  /** Returns end offsets for currently assigned partitions. */
  public Map<org.apache.kafka.common.TopicPartition, Long> endOffsets() {
    return Map.copyOf(consumer.endOffsets(consumer.assignment()));
  }

  /** Flushes publication and closes both test-owned clients. */
  @Override
  public void close() {
    RuntimeException failure = null;
    try {
      producer.flush();
      producer.close(Duration.ofSeconds(5));
    } catch (RuntimeException exception) {
      failure = exception;
    }
    try {
      consumer.close(org.apache.kafka.clients.consumer.CloseOptions.timeout(Duration.ofSeconds(5)));
    } catch (RuntimeException exception) {
      if (failure == null) {
        failure = exception;
      } else {
        failure.addSuppressed(exception);
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  /** Normalizes Kafka test-client failures. */
  public static class KafkaTestException extends RuntimeException {

    /** Creates a Kafka failure. */
    public KafkaTestException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
