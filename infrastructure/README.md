# Local Infrastructure

This directory owns Dockerfiles, Compose files, health checks, initialization assets, and the `minimal`, `integration`, `grid`, `observability`, and `full` profile definitions delivered in Milestone 8.

Infrastructure changes must pin production image versions or digests, avoid embedded secrets, expose readiness-based dependencies, isolate the Compose network/project, declare persistent vs ephemeral volumes, and define appropriate restart and resource policies. `docker compose config --quiet` and a clean startup/health/shutdown rehearsal are required before merge.
