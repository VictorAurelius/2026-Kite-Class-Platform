#!/usr/bin/env bash
# test-check-gap-status-csv.sh — fixture-based tests cho scripts/check-gap-status-csv.sh
#
# Script không có --paths flag → chạy từ fixture root subshell (cd FIXTURE/...).
# Mỗi fixture chứa documents/04-quality/gaps/{gap-status.csv,GAP-*.md,pending/...}
# CD vào fixture root → bash absolute SCRIPT path → assert exit.
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
SCRIPT="$REPO_ROOT/scripts/check-gap-status-csv.sh"
FIXTURES="$REPO_ROOT/scripts/tests/fixtures/check-gap-status-csv"
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

echo "=== test-check-gap-status-csv ==="

# Test 1 — good fixture → exit 0
rc=$(run_check "$FIXTURES/good")
assert_exit "good fixture should PASS" 0 "$rc"

# Test 2 — CSV row points to non-existent file → exit 1
rc=$(run_check "$FIXTURES/bad-missing-file")
assert_exit "bad-missing-file should FAIL" 1 "$rc"

# Test 3 — invalid status enum → exit 1
rc=$(run_check "$FIXTURES/bad-invalid-status")
assert_exit "bad-invalid-status should FAIL" 1 "$rc"

# Test 4 — gap file exists without CSV row (coverage gap) → exit 1
rc=$(run_check "$FIXTURES/bad-coverage-gap")
assert_exit "bad-coverage-gap should FAIL" 1 "$rc"

# Test 5 — duplicate ID → exit 1
rc=$(run_check "$FIXTURES/bad-duplicate-id")
assert_exit "bad-duplicate-id should FAIL" 1 "$rc"

# Test 6 — bad date format → exit 1
rc=$(run_check "$FIXTURES/bad-bad-date")
assert_exit "bad-bad-date should FAIL" 1 "$rc"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" = "0" ]
