# Shipment Service

The shipment service owns the shipment aggregate, parcel and address snapshots, lifecycle policy, customer-address cutoff, idempotent creation, optimistic locking, status history, and transactional outbox staging.

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

## Run against PostgreSQL

Set `URBANFLOW_DB_USERNAME` and `URBANFLOW_DB_PASSWORD`; optionally set `URBANFLOW_DB_URL` and `URBANFLOW_SHIPMENT_PORT`. Then run:

```bash
java -jar services/shipment-service/target/shipment-service-0.1.0.jar
```

Readiness is exposed at `/actuator/health/readiness`, Prometheus metrics at `/actuator/prometheus`, generated OpenAPI at `/openapi`, and Swagger UI at `/swagger-ui`.

Authentication enforcement and Kafka publication of pending outbox events are subsequent Phase 2 slices. The current OpenAPI contract explicitly declares unauthenticated behavior so it remains truthful to the executable service.
