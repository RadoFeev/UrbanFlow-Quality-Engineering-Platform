# Distributed end-to-end suite

The suite contains 20 independently isolated black-box journeys. Each journey registers a unique
customer, obtains a real session, creates an idempotent shipment, reads it by ID and tracking number,
and verifies a different cross-service invariant. It is opt-in because it requires the deployed stack:

```powershell
.\mvnw.cmd -pl :e2e-tests -am -Dgroups=e2e -De2e.enabled=true test
```

Use `identity.url` and `shipment.url` to target non-default endpoints. Sessions are always revoked;
accounts and shipments are expected to run inside the disposable Compose/Kubernetes test namespace.
