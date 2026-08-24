# Kafka event suite

The fast layer verifies all owned event types, envelopes, topics, keys, headers, schemas, ordering,
duplicates, retry/DLT decisions, delayed delivery, restart recovery, and eventual consistency with
Kafka client test doubles and Awaitility. `KafkaContainerRoundTripTest` uses a real Kafka broker and
is automatically skipped when Docker is unavailable.

```powershell
.\mvnw.cmd -pl :event-tests -am -Dgroups=event test
```
