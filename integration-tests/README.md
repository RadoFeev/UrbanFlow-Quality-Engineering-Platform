# Integration test suite

The integration suite starts the production shipment Spring application context against PostgreSQL
18.6. It validates the transactional aggregate, status-history, idempotency, optimistic-locking, and
outbox collaboration using real Spring Data repositories and Flyway migrations. Kafka publication is
disabled here because broker behavior belongs to the event suite; the durable outbox boundary remains
fully active.

```powershell
.\mvnw.cmd -pl :integration-tests -am -Dgroups=integration clean verify
```
