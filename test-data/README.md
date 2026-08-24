# Test Data

`test-data` contains synthetic immutable records, fluent builders, object mothers, valid and invalid
equivalence partitions, boundary datasets, deterministic random generation, unique reserved-domain
emails, unique tracking numbers, and log masking. It never uses real personal information.

Record the seed from `RandomTestDataFactory.seed()` in test evidence. Recreating a factory with the
same seed and run identifier reproduces the same sequence, while run-scoped atomic generators keep
parallel data unique.

```powershell
.\mvnw.cmd -pl :test-data -am -Pquality clean verify
```
