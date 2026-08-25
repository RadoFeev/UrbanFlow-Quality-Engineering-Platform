#!/usr/bin/env bash
set -uo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: run-with-infra-retry.sh '<command>'" >&2
  exit 2
fi

attempt_dir="$(mktemp -d)"
publish_attempts() {
  mkdir -p target/ci-attempts
  cp "$attempt_dir"/attempt-*.log target/ci-attempts/ 2>/dev/null || true
  rm -f "$attempt_dir"/attempt-1.log "$attempt_dir"/attempt-2.log
  rmdir "$attempt_dir" 2>/dev/null || true
}
trap publish_attempts EXIT

command_text="$1"
set +e
bash -lc "$command_text" 2>&1 | tee "$attempt_dir/attempt-1.log"
first_exit=${PIPESTATUS[0]}
set -e
if [[ $first_exit -eq 0 ]]; then
  exit 0
fi

signature='SessionNotCreatedException|Could not start a new session|ContainerLaunchException|Could not find a valid Docker environment|Ryuk.*(failed|timeout)|service .* is unhealthy'
if ! grep -Eiq "$signature" "$attempt_dir/attempt-1.log"; then
  echo "Failure is not an allowlisted infrastructure signature; retry denied." >&2
  exit "$first_exit"
fi

echo "Allowlisted infrastructure failure detected; preserving attempt 1 and retrying once."
set +e
bash -lc "$command_text" 2>&1 | tee "$attempt_dir/attempt-2.log"
second_exit=${PIPESTATUS[0]}
set -e
exit "$second_exit"
