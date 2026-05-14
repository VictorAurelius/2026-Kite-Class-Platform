#!/usr/bin/env bash
# test-check-meta-csv-indexes.sh — fixture tests cho CI job `meta-csv-indexes`
# (wrapper job chạy cả check-adrs-index-csv.sh + check-rules-index-csv.sh).
#
# Test logic: chạy SẺcript adrs + rules sequentially, aggregate exit:
#   - cả hai PASS → exit 0
#   - bất kỳ FAIL → exit ≥ 1
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
ADRS_SCRIPT="$REPO_ROOT/scripts/check-adrs-index-csv.sh"
RULES_SCRIPT="$REPO_ROOT/scripts/check-rules-index-csv.sh"
FIXTURES="$REPO_ROOT/scripts/tests/fixtures/check-meta-csv-indexes"
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

run_both() {
    local fixture_root="$1"
    set +e
    (
        cd "$fixture_root" && \
        bash "$ADRS_SCRIPT" >/dev/null 2>&1 && \
        bash "$RULES_SCRIPT" >/dev/null 2>&1
    )
    local rc=$?
    set -e
    echo "$rc"
}

echo "=== test-check-meta-csv-indexes ==="

# Test 1 — both CSVs valid → exit 0
rc=$(run_both "$FIXTURES/good")
assert_exit "good fixture (both indexes valid) should PASS" 0 "$rc"

# Test 2 — rules CSV broken → aggregate FAIL
rc=$(run_both "$FIXTURES/bad-rules-only")
assert_exit "bad-rules-only should FAIL" 1 "$rc"

# Test 3 — ADRs CSV broken → aggregate FAIL
rc=$(run_both "$FIXTURES/bad-adrs-only")
assert_exit "bad-adrs-only should FAIL" 1 "$rc"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" = "0" ]
