#!/usr/bin/env bash
set -uo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: run-with-infra-retry.sh '<command>'" >&2
  exit 2
fi

mkdir -p target/ci-attempts
command_text="$1"
set +e
bash -lc "$command_text" 2>&1 | tee target/ci-attempts/attempt-1.log
first_exit=${PIPESTATUS[0]}
set -e
if [[ $first_exit -eq 0 ]]; then
  exit 0
fi

signature='SessionNotCreatedException|Could not start a new session|ContainerLaunchException|Could not find a valid Docker environment|Ryuk.*(failed|timeout)|service .* is unhealthy'
if ! grep -Eiq "$signature" target/ci-attempts/attempt-1.log; then
  echo "Failure is not an allowlisted infrastructure signature; retry denied." >&2
  exit "$first_exit"
fi

echo "Allowlisted infrastructure failure detected; preserving attempt 1 and retrying once."
set +e
bash -lc "$command_text" 2>&1 | tee target/ci-attempts/attempt-2.log
second_exit=${PIPESTATUS[0]}
set -e
exit "$second_exit"
