# Flaky Test Policy

## Definition

A test is flaky when identical product build, test code, configuration, and relevant data can produce both pass and fail without an intentional environmental change. Intermittent product defects remain product defects even when they pass on retry.

## Classification

- Infrastructure: runner loss, container daemon failure, Grid session creation failure, network reset before request delivery.
- Product defect: inconsistent business outcome, race, stale state, crash, or invalid response.
- Assertion/test defect: wrong expectation, unsafe shared state, selector ambiguity, leaked resource.
- Timeout: product latency, environment capacity, or insufficient observable condition; cause must be assigned.

## Retry

Only allowlisted infrastructure classifications may retry once. Product assertions, HTTP business failures, accessibility violations, visual differences, security findings, and performance thresholds never retry automatically. Reports preserve both attempts and the classification confidence.

## Detection and quarantine

Historical analysis uses test ID, build, environment, browser, shard, duration, result, retry result, and signature. A test enters investigation when it changes outcome across comparable runs or exceeds a duration-variance threshold. Quarantine requires owner, defect, reason, date, and expiry; it excludes the test from blocking gates but keeps it scheduled and visible.

## Service levels

- Critical smoke tests: quarantine resolved within 2 business days or replaced by equivalent reliable coverage.
- Other tests: resolution within 10 business days.
- Suite flaky rate: below 2% across 20 comparable runs; critical smoke target is 0%.

Closing a flaky defect requires a reproduced cause, targeted fix, new regression evidence, and a stability observation window. Increasing timeouts or adding retries alone is not an accepted fix.
