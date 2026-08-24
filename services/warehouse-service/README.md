# Warehouse Service

The warehouse service owns physical warehouses, logistics hubs, operational incidents,
feature flags, and an append-only audit trail. Concurrent commands require strong
`If-Match` entity tags and produce an audit record within the same database transaction.

## Local configuration

Required variables:

- `WAREHOUSE_DATABASE_USER`
- `WAREHOUSE_DATABASE_PASSWORD`

Optional variables include `WAREHOUSE_DATABASE_URL` and `WAREHOUSE_SERVICE_PORT`.
No credentials or personal data are stored in this module.

## Verification

```powershell
.\mvnw.cmd -pl :warehouse-service -Pquality clean verify
```

The OpenAPI 3.1 contract is stored under `src/main/resources/openapi`.
