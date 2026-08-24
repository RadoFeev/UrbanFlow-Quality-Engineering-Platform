# Consumer-driven contract suite

The suite creates version-4 Pact artifacts for the five owned consumer/provider boundaries. Every
interaction drives a real Pact mock server through an HTTP client and includes positive, negative,
versioning, and provider-state variants. Generated contracts are written to `target/pacts` and are
the inputs to provider verification and the CI compatibility gate. During `verify`, every generated
request method, path, and response status is checked against the owning provider's OpenAPI contract.

```powershell
.\mvnw.cmd -pl :contract-tests -Dgroups=contract clean verify
```

The infrastructure compose profile includes a local Pact Broker for publication, verification
results, and `can-i-deploy` checks without requiring a hosted account.
