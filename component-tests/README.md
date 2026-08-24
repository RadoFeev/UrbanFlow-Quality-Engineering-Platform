# Component test suite

The component suite runs production adapters with only their external boundary replaced. The first
slice exercises `HttpNotificationGateway` against a real in-process WireMock HTTP server and verifies
request method, path, JSON mapping, idempotency, provider acknowledgements, error propagation, UTF-8,
and data minimization for all shipment notification event families.

```powershell
.\mvnw.cmd -pl :component-tests -am -Dgroups=component clean verify
```
