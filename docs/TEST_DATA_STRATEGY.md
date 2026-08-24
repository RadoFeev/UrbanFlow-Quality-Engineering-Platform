# Test Data Strategy

## Principles

Data is synthetic, isolated, deterministic when reproducing a failure, unique when executing in parallel, traceable to a run, and safely removable. No real names, addresses, phone numbers, credentials, tracking numbers, or delivery media are used.

## Generation model

- A run seed is recorded in the environment manifest and Allure.
- A test derives a child seed from run ID and stable test ID.
- Builders enforce valid defaults and expose explicit domain choices.
- Object mothers represent named business personas; factories compose complete graphs.
- Invalid and boundary datasets are separate typed fixtures, not magic constants inside tests.
- Unique email and tracking generators combine a safe prefix, run ID, counter, and checksum.

## Ownership and cleanup

Created resources are registered immediately in a LIFO cleanup registry. API deletion is preferred because it exercises supported lifecycle rules; database cleanup is reserved for test-profile administration and infrastructure recovery. Cleanup retries only transient infrastructure errors, verifies deletion, and attaches unresolved resource IDs. Cleanup never uses broad table truncation in shared environments.

## Reference data

Immutable roles, permissions, delivery types, hubs, and feature defaults are versioned through migrations or environment provisioning. Tests may read but do not mutate shared reference records. Scenarios that test administration create uniquely named alternatives.

## Data protection

The evidence masker redacts passwords, tokens, cookies, authorization headers, secret query parameters, private keys, email local parts, phone numbers, address lines, and delivery-proof metadata. JSON masking follows field names plus content heuristics; HTTP headers are case-insensitive. Masking is tested before API/evidence clients are released.

## Reproduction

Reports retain run ID, test ID, seed, generator version, environment build, correlation ID, and trace ID. A local reproduction command accepts the seed and test selector without requiring any captured secret.
