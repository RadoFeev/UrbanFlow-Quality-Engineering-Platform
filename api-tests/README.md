# API test suite

The API suite validates every published OpenAPI operation and exercises the real Spring HTTP adapters
with REST Assured. Contract checks fail when an operation disappears, loses its stable identifier,
success response, documented failure response, or ownership tag. Adapter checks cover serialization,
validation, malformed input, unsupported media/methods, stable errors, cache controls, and propagated
correlation and trace identifiers without requiring shared database state.

```powershell
.\mvnw.cmd -pl :api-tests -am -Dgroups=api clean verify
```
