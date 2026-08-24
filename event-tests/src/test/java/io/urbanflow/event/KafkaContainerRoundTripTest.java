package io.urbanflow.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.urbanflow.event.support.EventFixtures;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/** Real-broker acknowledgment and header round trip, enabled whenever Docker is available. */
@Tag("event")
@Testcontainers(disabledWithoutDocker = true)
class KafkaContainerRoundTripTest {

  @Container
  static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));

  @BeforeAll
  static void createTopics() throws Exception {
    Properties properties = new Properties();
    properties.put("bootstrap.servers", KAFKA.getBootstrapServers());
    try (AdminClient admin = AdminClient.create(properties)) {
      admin
          .createTopics(List.of(new NewTopic(EventFixtures.SHIPMENT_TOPIC, 1, (short) 1)))
          .all()
          .get();
    }
  }

  @Test
  void brokerAcknowledgesAndConsumerReceivesExactRecord() throws Exception {
    Properties producerProperties = new Properties();
    producerProperties.put("bootstrap.servers", KAFKA.getBootstrapServers());
    producerProperties.put("acks", "all");
    Properties consumerProperties = new Properties();
    consumerProperties.put("bootstrap.servers", KAFKA.getBootstrapServers());
    consumerProperties.put("group.id", "urbanflow-event-test");
    consumerProperties.put("auto.offset.reset", "earliest");
    var event = EventFixtures.event("ShipmentCreated");
    try (var producer =
            new KafkaProducer<>(
                producerProperties, new StringSerializer(), new StringSerializer());
        var consumer =
            new KafkaConsumer<>(
                consumerProperties, new StringDeserializer(), new StringDeserializer())) {
      consumer.subscribe(List.of(EventFixtures.SHIPMENT_TOPIC));
      producer.send(EventFixtures.record(event)).get();

      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(20))
          .untilAsserted(
              () ->
                  assertThat(consumer.poll(Duration.ofMillis(250)))
                      .anySatisfy(
                          record -> {
                            assertThat(record.key()).isEqualTo(event.aggregateId().toString());
                            assertThat(record.headers().lastHeader("traceId")).isNotNull();
                          }));
    }
  }
}
