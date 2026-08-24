# Scripts

Scripts provide small, cross-platform developer and CI entry points for environment checks, stack lifecycle, suite orchestration, report assembly, link/YAML validation, and release verification. Scripts must fail fast, preserve exit codes, avoid logging secrets, use resolved narrow paths for file operations, and expose a help flag.

Build and test behavior remains in Maven or purpose-built tools; scripts do not duplicate business logic or hide required quality gates.
