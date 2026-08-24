# Observability assets

The OpenTelemetry Collector accepts OTLP metrics, traces, and logs; metrics are exported for
Prometheus while local trace/log output remains deliberately minimal. Prometheus scrapes all service
actuators and evaluates eight SLO-oriented alerts. Grafana provisions the data source and the
versioned `UrbanFlow Quality Engineering` dashboard without manual clicks.

Start only this layer with `docker compose -f infrastructure/compose.yml --profile observability up
-d`, or include it through the `full` profile. Telemetry follows
[docs/OBSERVABILITY.md](../docs/OBSERVABILITY.md): identifiers never become metric labels and
credentials or customer payloads are not collected.
