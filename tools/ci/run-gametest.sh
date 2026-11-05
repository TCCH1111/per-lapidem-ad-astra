#!/usr/bin/env bash
# PLA: CI P1 runner (w06 §4.2). Runs the NeoForge game-test server and tolerates exactly one
# retry when the *only* failures are on the known-flaky allowlist. Any other failure is never
# retried. Evidence: docs/reports/VERIFY-045; debt D7.
set -uo pipefail
cd "$(dirname "$0")/../.." || exit 3

LOG_DIR="build/ci-logs"
mkdir -p "$LOG_DIR" run/gametest
printf 'eula=true\n' > run/gametest/eula.txt

# Known non-deterministic upstream test (VERIFY-010/012; debt D7). Nothing else may retry.
ALLOWLIST="onefoodcannotfeedhalfofapair"

run_once() {
  local attempt="$1"
  ./gradlew runGameTestServer --console=plain --no-daemon 2>&1 \
    | tee "$LOG_DIR/gametest-$attempt.log"
  return "${PIPESTATUS[0]}"
}

failing_tests() {
  grep -aoE '[A-Za-z0-9_]+ failed at' "$1" | sed -E 's/[[:space:]]+failed at$//' | sort -u
}

if run_once 1; then
  echo "GAMETEST: passed on attempt 1"
  exit 0
fi

mapfile -t fails < <(failing_tests "$LOG_DIR/gametest-1.log")
if [ "${#fails[@]}" -eq 0 ]; then
  echo "GAMETEST: run failed with no parseable failing-test name; treating as a real failure"
  exit 1
fi

for name in "${fails[@]}"; do
  case " $ALLOWLIST " in
    *" $name "*) echo "GAMETEST: allowlisted flaky failure: $name" ;;
    *) echo "GAMETEST: non-allowlisted failure: $name"; exit 1 ;;
  esac
done

echo "GAMETEST: only allowlisted flaky tests failed; retrying once"
if run_once 2; then
  echo "GAMETEST: passed on retry (allowlisted flakiness)"
  exit 0
fi
echo "GAMETEST: still failing on retry"
exit 1
