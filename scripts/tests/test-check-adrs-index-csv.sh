#!/usr/bin/env bash
# test-check-adrs-index-csv.sh — fixture tests cho scripts/check-adrs-index-csv.sh
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
SCRIPT="$REPO_ROOT/scripts/check-adrs-index-csv.sh"
FIXTURES="$REPO_ROOT/scripts/tests/fixtures/check-adrs-index-csv"
PASS=0
FAIL=0

assert_exit() {
    local name="$1" expected="$2" actual="$3"
    if [ "$actual" = "$expected" ]; then
        echo "  PASS — $name (exit=$actual)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL — $name (expected exit $expected, got $actual)"
        FAIL=$((FAIL + 1))
    fi
}

run_check() {
    local fixture_root="$1"
    set +e
    (cd "$fixture_root" && bash "$SCRIPT" >/dev/null 2>&1)
    local rc=$?
    set -e
    echo "$rc"
}

echo "=== test-check-adrs-index-csv ==="

# Test 1 — good fixture → exit 0
rc=$(run_check "$FIXTURES/good")
assert_exit "good fixture should PASS" 0 "$rc"

# Test 2 — CSV row points to non-existent ADR → exit 1
rc=$(run_check "$FIXTURES/bad-missing-file")
assert_exit "bad-missing-file should FAIL" 1 "$rc"

# Test 3 — invalid status enum → exit 1
rc=$(run_check "$FIXTURES/bad-invalid-status")
assert_exit "bad-invalid-status should FAIL" 1 "$rc"

# Test 4 — ADR file exists without CSV row → exit 1
rc=$(run_check "$FIXTURES/bad-coverage-gap")
assert_exit "bad-coverage-gap should FAIL" 1 "$rc"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" = "0" ]
