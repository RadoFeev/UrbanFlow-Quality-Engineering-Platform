# Helm Chart

This directory owns the parameterized UrbanFlow test-infrastructure chart delivered in Milestone 8. Values cover image digests, resources, storage, suite selection, browser/Grid capacity, evidence retention, observability, and secret references. Secret values are never shipped in defaults.

Chart changes require `helm lint`, `helm template` for minimal and full values, and a kind or minikube installation rehearsal.
