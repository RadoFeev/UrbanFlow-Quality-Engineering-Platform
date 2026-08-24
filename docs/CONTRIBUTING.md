# Contributing

## Workflow

Create a focused branch from current `main`, make one coherent change, add risk-based tests, update affected contracts/docs, run the applicable local gates, and open a pull request using the template. Keep changes reviewable and avoid mixing refactors with behavior changes unless separation would be unsafe.

## Commit style

Use Conventional Commits: `feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `build:`, `ci:`, `perf:`, `chore:`. Add `!` and a `BREAKING CHANGE:` footer for incompatible API, schema, configuration, or test-framework changes.

## Code standards

- Java 25, immutable configuration, records/sealed types where they clarify the model.
- SOLID and composition over inheritance; abstractions must remove real duplication or isolate a volatile boundary.
- Public framework classes have concise JavaDoc.
- No global WebDriver, shared mutable test state, implicit waits, PageFactory, fixed sleeps, hardcoded credentials, real personal data, or test-order dependencies.
- Tests name the behavior and expected outcome, create their own data, and clean up safely.

## Local checks

```bash
./mvnw clean verify
./mvnw -Pquality verify
```

Run the smallest affected suites during development and the documented PR-equivalent checks before requesting review. Container, browser, performance, and security changes require their specialized validation.

## Review expectations

Reviewers verify requirement/risk traceability, boundary ownership, compatibility, negative/error behavior, concurrency and idempotency where relevant, evidence masking, test independence, cleanup, and documentation accuracy.
