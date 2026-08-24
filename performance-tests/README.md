# k6 performance suite

The suite defines ten independently measured service and business-read scenarios. Every scenario has
availability, p95 latency, and semantic check thresholds. One script supports `smoke`, `load`,
`stress`, `spike`, and `soak` workload profiles without embedding credentials.

```powershell
# Validate the bundle without sending traffic
k6 inspect performance-tests/k6/urbanflow.js

# Short validation against a running local stack
k6 run -e PROFILE=smoke performance-tests/k6/urbanflow.js

# Load example; service origins can be overridden independently
k6 run -e PROFILE=load -e RATE=5 -e SHIPMENT_URL=http://localhost:8082 performance-tests/k6/urbanflow.js
```

`ACCESS_TOKEN` and `TRACKING_NUMBER` are optional synthetic-test values supplied through the
environment. Never point stress, spike, or soak profiles at production.
