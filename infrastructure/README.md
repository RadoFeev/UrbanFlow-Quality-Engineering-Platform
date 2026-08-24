# Local infrastructure

The Compose stack packages all six services and three portals, PostgreSQL, Redis, Kafka, Kafka UI,
Pact Broker, Selenium Grid with Chrome/Firefox nodes, OpenTelemetry Collector, Prometheus, and Grafana.
Images are version-pinned, stateful dependencies own named volumes, services wait on health, and no
secret value is stored in the repository.

## First startup

1. Copy `.env.example` to `.env` and fill the blank password/signing-secret values.
2. Package executable applications with `.\mvnw.cmd -DskipTests package`.
3. Validate with `docker compose -f infrastructure/compose.yml --profile minimal config --quiet`.
4. Start with `docker compose -f infrastructure/compose.yml --profile minimal up -d --build --wait`.
5. Inspect with `docker compose -f infrastructure/compose.yml --profile minimal ps`.

The PowerShell wrapper combines these steps as `.\scripts\stack.ps1 -Action Up -Profile minimal
-Build`. Use `-Action Down` for a recoverable stop. Adding `--volumes` to a manual Compose shutdown
deletes local PostgreSQL, Kafka, Redis, Prometheus, and Grafana data.

## Profiles

| Profile | Contents |
|---|---|
| `minimal` | Datastores, messaging, all services, and all portals |
| `integration` | PostgreSQL, Redis, Kafka, Kafka UI, and Pact Broker |
| `grid` | Selenium Hub plus Chrome and Firefox nodes |
| `observability` | OpenTelemetry Collector, Prometheus, and Grafana |
| `full` | Every component |

Useful endpoints are customer `3001`, courier `3002`, operations `3003`, services `8081`–`8086`,
Kafka UI `8090`, Pact Broker `9292`, Grid `4444`, Prometheus `9090`, and Grafana `3000`.
