# Shipment Service

The shipment service owns the shipment aggregate, parcel and address snapshots, lifecycle policy, customer-address cutoff, idempotent creation, optimistic locking, status history, and transactional outbox publication.

## Implemented endpoints

| Method | Path | Behavior |
|---|---|---|
| POST | `/api/v1/shipments` | Idempotent shipment creation; requires `Idempotency-Key` |
| GET | `/api/v1/shipments/{shipmentId}` | Read by UUID with version ETag |
| GET | `/api/v1/tracking/{trackingNumber}` | Public tracking-number lookup |
| POST | `/api/v1/shipments/{shipmentId}/transitions` | Actor/evidence-validated transition; requires `If-Match` |
| PUT | `/api/v1/shipments/{shipmentId}/delivery-address` | Versioned address update before pickup cutoff |

The reviewed contract is [shipment-service.yaml](src/main/resources/openapi/shipment-service.yaml).

## Build and test

From the repository root with JDK 25:

```bash
./mvnw -pl :shipment-service -am clean test
./mvnw -pl :shipment-service -am -Pquality clean verify
```

## Outbox publication

Shipment changes and event rows commit atomically in PostgreSQL. Publisher workers claim ready rows with `FOR UPDATE SKIP LOCKED` and a recoverable lease, dispatch the batch to Kafka, wait for broker acknowledgment, and only then mark rows published. The aggregate UUID is the Kafka key, preserving per-shipment ordering. Delivery is at-least-once: consumers must treat the globally unique `eventId` idempotently.

Failures release the lease with capped exponential backoff. Stored and logged failure data contains an exception category rather than broker messages or payloads. Prometheus exposes `urbanflow.outbox.publications` by bounded `result` and `urbanflow.outbox.publication.duration` without high-cardinality labels.

## Run against PostgreSQL and Kafka

Set `URBANFLOW_DB_USERNAME` and `URBANFLOW_DB_PASSWORD`; optionally set `URBANFLOW_DB_URL`, `URBANFLOW_KAFKA_BOOTSTRAP_SERVERS`, `URBANFLOW_SHIPMENT_EVENTS_TOPIC`, and `URBANFLOW_SHIPMENT_PORT`. Publication can be paused with `URBANFLOW_OUTBOX_ENABLED=false` while retaining durable pending rows. Then run:

```bash
java -jar services/shipment-service/target/shipment-service-0.1.0-exec.jar
```

Readiness is exposed at `/actuator/health/readiness`, Prometheus metrics at `/actuator/prometheus`, generated OpenAPI at `/openapi`, and Swagger UI at `/swagger-ui`.

Authentication enforcement, broker-backed integration validation, and downstream tracking projection are subsequent Phase 2 slices. The current OpenAPI contract explicitly declares unauthenticated behavior so it remains truthful to the executable service.
