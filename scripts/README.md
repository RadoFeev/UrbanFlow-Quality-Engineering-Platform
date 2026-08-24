# Scripts

- `check-prerequisites.ps1` reports required Java/Git and optional Docker, kubectl, Helm, and k6.
- `stack.ps1` validates, starts, inspects, logs, or stops one explicit Compose profile.
- `run-suite.ps1` builds and invokes the typed test orchestrator in preview or execute mode.
- `validate-repository.ps1` parses delivery manifests, checks Git whitespace, and inspects the k6
  bundle.
- `ci/run-with-infra-retry.sh` preserves the original log and allows one retry only for a narrow list
  of browser/container provisioning signatures.

Examples:

```powershell
.\scripts\check-prerequisites.ps1
.\scripts\stack.ps1 -Action Validate -Profile full
.\scripts\run-suite.ps1 -Suite api
.\scripts\run-suite.ps1 -Suite ui -Browser chrome -Execute
.\scripts\validate-repository.ps1
```

Scripts fail fast, preserve exit codes, resolve workspace paths, and never print secret values.
