#!/usr/bin/env bash
#
# test-deploy-prod-guards.sh — verify env guards on deploy-prod.sh
# GAP-506 Bucket F AC F-AC1 self-test
#
# Tests:
#   1. Refuses to run when KITE_FIRST_APPLY=true (exit 2)
#   2. Without KITE_FIRST_APPLY=true, fails differently (DEPLOY_DIR missing)
#      → demonstrates env guard runs BEFORE other checks
#
# Does NOT test full deploy path (ECR login, compose pull) — those require
# EC2 + AWS perms + docker. Tests env-guard logic only.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPT="$SCRIPT_DIR/deploy-prod.sh"

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

echo "=== test-deploy-prod-guards.sh ==="
echo

# Test 1: KITE_FIRST_APPLY=true → exit 2 (refuse — must use deploy-bootstrap.sh)
# Use isolated DEPLOY_DIR to avoid touching real /opt/kite-prod.
run_test "KITE_FIRST_APPLY=true rejected" 2 \
  env KITE_FIRST_APPLY=true DEPLOY_DIR=/tmp/nonexistent-kite-test bash "$SCRIPT"

# Test 2: KITE_FIRST_APPLY=false (or unset) → guard passes; next check (DEPLOY_DIR/.git
# missing) fires with exit 3. Demonstrates guard ordering: env guard runs BEFORE
# bootstrap-state check.
run_test "KITE_FIRST_APPLY unset proceeds past env guard" 3 \
  env -u KITE_FIRST_APPLY DEPLOY_DIR=/tmp/nonexistent-kite-test bash "$SCRIPT"

echo
echo "=== Summary: $PASS_COUNT passed, $FAIL_COUNT failed ==="
if [[ "$FAIL_COUNT" -gt 0 ]]; then
  exit 1
fi
exit 0
