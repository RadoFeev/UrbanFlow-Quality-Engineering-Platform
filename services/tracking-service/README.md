# Tracking Service

The tracking service owns the customer-facing timeline projection. It consumes version 1 shipment events, persists each unique event in PostgreSQL, rejects stale aggregate versions, invalidates Redis after successful projection, and serves an ordered cache-aside timeline.

## Runtime behavior

- Kafka key ordering is supplied by the shipment UUID.
- Event IDs make duplicate delivery an acknowledged no-op.
- Unsupported schema major versions and malformed events fail consumption for retry/DLT handling.
- Redis failure never prevents a PostgreSQL projection or read; cache operations emit bounded hit, miss, and fallback metrics.
- Tracking lookup is available at `GET /api/v1/tracking/{trackingNumber}`.

## Configuration

Required database variables are `URBANFLOW_TRACKING_DB_USERNAME` and `URBANFLOW_TRACKING_DB_PASSWORD`. Kafka, Redis, database URL, port, group, topic, TTL, and consumer enablement have environment-specific overrides documented in `application.yml`.

From the repository root with JDK 25:

```bash
./mvnw -pl :tracking-service -am -Pquality clean verify
java -jar services/tracking-service/target/tracking-service-0.1.0-exec.jar
```

The reviewed HTTP contract is [tracking-service.yaml](src/main/resources/openapi/tracking-service.yaml). Real broker/database/cache validation requires the integration profile delivered with the infrastructure milestone.
