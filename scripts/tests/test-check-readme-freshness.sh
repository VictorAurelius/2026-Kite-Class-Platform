#!/usr/bin/env bash
# test-check-readme-freshness.sh — fixture tests cho scripts/check-readme-freshness.sh
#
# Uses --paths flag to test individual fixture files (avoid full-repo scan).
# Fresh fixture written with today's date dynamically (tmpfile) to avoid drift.
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
SCRIPT="$REPO_ROOT/scripts/check-readme-freshness.sh"
FIXTURES="$REPO_ROOT/scripts/tests/fixtures/check-readme-freshness"
PASS=0
FAIL=0

# tmpdir for dynamic-date fixtures (cleanup at exit)
TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

# Write a fresh fixture with today's date
TODAY=$(date +%Y-%m-%d)
cat > "$TMPDIR/fresh.md" <<EOF
# Fresh fixture
**Last Updated:** $TODAY
EOF

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
    local file="$1"
    shift
    set +e
    bash "$SCRIPT" --paths "$file" "$@" >/dev/null 2>&1
    local rc=$?
    set -e
    echo "$rc"
}

echo "=== test-check-readme-freshness ==="

# Test 1 — exempt fixture → exit 0 (silent EXEMPT)
rc=$(run_check "$FIXTURES/exempt.md")
assert_exit "exempt.md should PASS" 0 "$rc"

# Test 2 — no-date fixture → exit 0 (WARN default mode)
rc=$(run_check "$FIXTURES/no-date.md")
assert_exit "no-date.md should PASS in default mode (WARN only)" 0 "$rc"

# Test 3 — no-date fixture in --strict mode → exit 1
rc=$(run_check "$FIXTURES/no-date.md" --strict)
assert_exit "no-date.md should FAIL in --strict mode" 1 "$rc"

# Test 4 — stale >90 day → exit 1 (FAIL)
rc=$(run_check "$FIXTURES/stale-over-90d.md")
assert_exit "stale-over-90d.md should FAIL" 1 "$rc"

# Test 5 — dynamic-date fresh fixture → exit 0
rc=$(run_check "$TMPDIR/fresh.md")
assert_exit "fresh.md (today's date) should PASS" 0 "$rc"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" = "0" ]
