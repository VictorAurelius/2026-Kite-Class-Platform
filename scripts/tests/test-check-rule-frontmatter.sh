#!/usr/bin/env bash
# test-check-rule-frontmatter.sh — fixture-based tests cho scripts/check-rule-frontmatter.sh
#
# Per Wave 76 Bucket B (test coverage extension for scripts/check-*.sh).
# Mỗi test invoke check script với --paths trỏ tới fixture file, assert exit code.
#
# Fixture naming:
#   good-*.md  → expected exit 0 (PASS)
#   bad-*.md   → expected exit 1 (FAIL)
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
SCRIPT="$REPO_ROOT/scripts/check-rule-frontmatter.sh"
FIXTURES="$REPO_ROOT/scripts/tests/fixtures/check-rule-frontmatter"
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
    # Capture exit code without aborting the script (set +e during this call)
    local fixture="$1"
    set +e
    bash "$SCRIPT" --all --paths "$fixture" >/dev/null 2>&1
    local rc=$?
    set -e
    echo "$rc"
}

echo "=== test-check-rule-frontmatter ==="

# Test 1 — good fixture → exit 0
rc=$(run_check "$FIXTURES/good-compliant.md")
assert_exit "good-compliant.md should PASS" 0 "$rc"

# Test 2 — missing Version → exit 1
rc=$(run_check "$FIXTURES/bad-missing-version.md")
assert_exit "bad-missing-version.md should FAIL" 1 "$rc"

# Test 3 — missing Last-Reviewed → exit 1
rc=$(run_check "$FIXTURES/bad-missing-last-reviewed.md")
assert_exit "bad-missing-last-reviewed.md should FAIL" 1 "$rc"

# Test 4 — future Last-Reviewed → exit 1
rc=$(run_check "$FIXTURES/bad-future-last-reviewed.md")
assert_exit "bad-future-last-reviewed.md should FAIL" 1 "$rc"

# Test 5 — missing Reviewer-Approver → exit 1
rc=$(run_check "$FIXTURES/bad-missing-reviewer.md")
assert_exit "bad-missing-reviewer.md should FAIL" 1 "$rc"

# Test 6 — missing Log section → exit 1
rc=$(run_check "$FIXTURES/bad-missing-log.md")
assert_exit "bad-missing-log.md should FAIL" 1 "$rc"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" = "0" ]
