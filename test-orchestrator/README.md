# Test Orchestrator

The orchestrator converts a validated execution plan into one Maven Wrapper process and prepares a
unified Allure result directory with environment, executor, and defect-category metadata. Preview is
the safe default; `--execute=true` starts the selected suite.

```powershell
java -jar test-orchestrator\target\test-orchestrator-0.1.0.jar `
  --environment=local --suite=api --browser=chrome --headless=true --parallelism=4 `
  --tags=smoke --retries=0 --video=false --cleanup=true --execute=false
```

Supported suites are `all`, `smoke`, `regression`, `api`, `ui`, `contract`, `component`,
`integration`, `event`, `database`, `e2e`, `accessibility`, `visual`, `performance`, and `security`.
Retries are limited to two and are consumed only by the infrastructure-failure policy in Test Core.
