#!/usr/bin/env bash
# test-check-be-fe-url-contract.sh — fixture tests cho scripts/check-be-fe-url-contract.sh
#
# Per GAP-802 cơ chế #2. Mirrors the 3-fixture self-test pattern from
# scripts/tests/test-audit-env-coverage.sh.
#
# Fixtures (CONTRACT_ROOT-driven synthetic repo layouts):
#   pass/  — BE FE-path builders that all resolve to real FE routes, including
#            route-group-stripped paths ((auth), (public)). → expect exit 0.
#   fail/  — BE builds "/signup/beta" but only "/beta-signup/code" route exists
#            (GAP-801 reproduction). → expect exit 1.
#
# Also asserts:
#   - PASS fixture output names the route-group-stripped FE route (proves the
#     "(auth)" / "(public)" parens were removed during matching).
#   - --json mode shape.
#   - path_matches() wildcard logic handles dynamic [id] segments.
#
# Exit codes:
#   0 — all assertions pass
#   1 — at least 1 assertion failed
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
SCRIPT="$REPO_ROOT/scripts/check-be-fe-url-contract.sh"
FIXTURES="$REPO_ROOT/scripts/tests/fixtures/be-fe-url-contract"
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

assert_contains() {
    local name="$1" needle="$2" haystack="$3"
    if printf '%s' "$haystack" | grep -qF "$needle"; then
        echo "  PASS — $name"
        PASS=$((PASS + 1))
    else
        echo "  FAIL — $name (output missing: $needle)"
        FAIL=$((FAIL + 1))
    fi
}

run_exit() {
    local fixture_root="$1"
    local -a args=()
    [ -n "${2:-}" ] && args+=("$2")
    set +e
    CONTRACT_ROOT="$fixture_root" bash "$SCRIPT" "${args[@]}" >/dev/null 2>&1
    local rc=$?
    set -e
    echo "$rc"
}

run_out() {
    local fixture_root="$1"
    local -a args=()
    [ -n "${2:-}" ] && args+=("$2")
    set +e
    CONTRACT_ROOT="$fixture_root" bash "$SCRIPT" "${args[@]}" 2>&1
    set -e
}

echo "=== test-check-be-fe-url-contract ==="

# Test 1 — PASS fixture: every BE FE-path resolves (incl. route-group stripping).
rc=$(run_exit "$FIXTURES/pass")
assert_exit "Fixture pass: all BE paths resolve → exit 0" 0 "$rc"

# Test 2 — route-group stripping: (auth)/beta-signup/code → /beta-signup/code
pass_out=$(run_out "$FIXTURES/pass")
assert_contains "Route-group (auth) stripped: FOUND /beta-signup/code" \
    "FOUND /beta-signup/code" "$pass_out"
assert_contains "Route-group (public) stripped: FOUND /staff/accept-invite" \
    "FOUND /staff/accept-invite" "$pass_out"

# Test 3 — FAIL fixture: GAP-801 /signup/beta has no matching route → exit 1.
rc=$(run_exit "$FIXTURES/fail")
assert_exit "Fixture fail: GAP-801 /signup/beta missing → exit 1" 1 "$rc"
fail_out=$(run_out "$FIXTURES/fail")
assert_contains "FAIL fixture reports /signup/beta MISSING" \
    "BE path /signup/beta" "$fail_out"
assert_contains "FAIL fixture marks it MISSING" "MISSING" "$fail_out"

# Test 4 — --json mode shape on both fixtures.
json_pass=$(run_out "$FIXTURES/pass" "--json")
assert_contains "PASS fixture --json reports zero missing" '"missing":0' "$json_pass"
json_fail=$(run_out "$FIXTURES/fail" "--json")
assert_contains "FAIL fixture --json reports one missing" '"missing":1' "$json_fail"

# Test 5 — wildcard dynamic-segment matching (path_matches with [id] → "*").
# Source the script in a sub-shell with no main execution side effects by
# stubbing find/collect — instead just import the function via `source` guard:
# the script runs to completion (it's idempotent + read-only), so we re-test the
# pure matcher by sourcing into a function-only context.
(
  # shellcheck disable=SC1090
  # Extract only the path_matches function definition to avoid running main.
  eval "$(sed -n '/^path_matches() {/,/^}/p' "$SCRIPT")"
  set +e
  path_matches "/instances/123" "/instances/*"; rc_match=$?
  path_matches "/instances/123/extra" "/instances/*"; rc_len=$?
  path_matches "/instances/123" "/billing/*"; rc_diff=$?
  set -e
  [ "$rc_match" = "0" ] && echo "  PASS — wildcard matches /instances/123 vs /instances/*" || echo "  FAIL — wildcard should match /instances/123"
  [ "$rc_len" = "1" ] && echo "  PASS — segment-count mismatch rejected (/instances/123/extra)" || echo "  FAIL — should reject differing segment count"
  [ "$rc_diff" = "1" ] && echo "  PASS — different static prefix rejected" || echo "  FAIL — should reject /billing/* for /instances/123"
) | tee /tmp/wildcard-assert.out
# Fold the sub-shell results into PASS/FAIL counters.
PASS=$((PASS + $(grep -c '  PASS' /tmp/wildcard-assert.out)))
FAIL=$((FAIL + $(grep -c '  FAIL' /tmp/wildcard-assert.out)))
rm -f /tmp/wildcard-assert.out

echo
echo "=== Summary ==="
echo "PASS: $PASS"
echo "FAIL: $FAIL"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
