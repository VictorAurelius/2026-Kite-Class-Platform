#!/usr/bin/env bash
#
# test-deploy-bootstrap-guards.sh — verify env guards on deploy-bootstrap.sh
# GAP-506 Bucket F AC F-AC1 self-test
#
# Tests:
#   1. Refuses to run when KITE_FIRST_APPLY unset (exit 2)
#   2. Refuses to run when KITE_FIRST_APPLY=false (exit 2)
#   3. Refuses to run when KITE_BOOTSTRAP_DONE=true (exit 3)
#
# Does NOT test the actual bootstrap path (clone, SSM PutParameter) — those
# require EC2 + AWS perms. Tests env-guard logic only.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPT="$SCRIPT_DIR/deploy-bootstrap.sh"

if [[ ! -x "$SCRIPT" ]]; then
  chmod +x "$SCRIPT" 2>/dev/null || true
fi
if [[ ! -f "$SCRIPT" ]]; then
  echo "FAIL: $SCRIPT not found"
  exit 1
fi

FAIL_COUNT=0
PASS_COUNT=0

run_test() {
  local name="$1"
  local expected_exit="$2"
  shift 2
  # Capture exit; suppress stderr/stdout noise
  set +e
  ( "$@" ) >/dev/null 2>&1
  local actual_exit=$?
  set -e
  if [[ "$actual_exit" -eq "$expected_exit" ]]; then
    echo "PASS: $name (exit $actual_exit)"
    PASS_COUNT=$((PASS_COUNT + 1))
  else
    echo "FAIL: $name — expected exit $expected_exit, got $actual_exit"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
}

echo "=== test-deploy-bootstrap-guards.sh ==="
echo

# Test 1: KITE_FIRST_APPLY unset → exit 2
run_test "KITE_FIRST_APPLY unset rejected" 2 \
  env -u KITE_FIRST_APPLY -u KITE_BOOTSTRAP_DONE bash "$SCRIPT"

# Test 2: KITE_FIRST_APPLY=false → exit 2
run_test "KITE_FIRST_APPLY=false rejected" 2 \
  env KITE_FIRST_APPLY=false bash "$SCRIPT"

# Test 3: KITE_BOOTSTRAP_DONE=true → exit 3 (override the SSM lookup)
run_test "KITE_BOOTSTRAP_DONE=true rejected" 3 \
  env KITE_FIRST_APPLY=true KITE_BOOTSTRAP_DONE=true bash "$SCRIPT"

echo
echo "=== Summary: $PASS_COUNT passed, $FAIL_COUNT failed ==="
if [[ "$FAIL_COUNT" -gt 0 ]]; then
  exit 1
fi
exit 0
