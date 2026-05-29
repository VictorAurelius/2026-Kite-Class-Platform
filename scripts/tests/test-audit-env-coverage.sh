#!/usr/bin/env bash
# test-audit-env-coverage.sh — fixture tests cho scripts/audit-env-coverage.sh
#
# Per Wave br-4 Bucket A GAP-508 Phase 3 — codify 3-fixture self-test pattern
# (known good / known false-positive / known missing) để eliminate regression
# khi ACCEPTABLE_DEFAULTS hoặc is_overridden() logic được extend tương lai.
#
# Per `pre-handoff-self-test-completeness.md` §5.2 self-test mandate. Mỗi fixture
# represent một config-class scenario; failure cases catch regression early.
#
# Exit codes:
#   0 — all 3 fixtures pass expected behavior
#   1 — at least 1 fixture failed assertion
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
SCRIPT="$REPO_ROOT/scripts/audit-env-coverage.sh"
FIXTURES="$REPO_ROOT/scripts/tests/fixtures/audit-env-coverage"
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
    shift
    set +e
    AUDIT_ROOT="$fixture_root" bash "$SCRIPT" "$@" >/dev/null 2>&1
    local rc=$?
    set -e
    echo "$rc"
}

# Run with --json and extract a single integer key value (missing/ok/accepted/local_deadlink).
run_json_key() {
    local fixture_root="$1" key="$2"
    set +e
    local out
    out=$(AUDIT_ROOT="$fixture_root" bash "$SCRIPT" --json 2>/dev/null)
    set -e
    # Parse {"missing":N,...} → value for key (no jq dependency).
    echo "$out" | sed -E "s/.*\"$key\":([0-9]+).*/\1/"
}

assert_eq() {
    local name="$1" expected="$2" actual="$3"
    if [ "$actual" = "$expected" ]; then
        echo "  PASS — $name (got=$actual)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL — $name (expected $expected, got $actual)"
        FAIL=$((FAIL + 1))
    fi
}

echo "=== test-audit-env-coverage ==="

# Test 1 — known good config (RESEND_API_KEY overridden + EMAIL_PROVIDER=resend)
# Expected: PASS (exit 0) — all suspect defaults overridden OR accepted
rc=$(run_check "$FIXTURES/good")
assert_exit "Fixture 1: known-good config (all overridden) should PASS" 0 "$rc"

# Test 2 — known false-positive (server.port:8080 default — well-known port, not suspect)
# Expected: PASS (exit 0) — no suspect default present (well-known constant excluded)
rc=$(run_check "$FIXTURES/false-positive")
assert_exit "Fixture 2: well-known default (no suspect pattern) should PASS" 0 "$rc"

# Test 3 — known missing override (suspect default + no compose/fetch override)
# Expected: FAIL (exit 1) — missing production override surfaced
rc=$(run_check "$FIXTURES/missing-override")
assert_exit "Fixture 3: missing production override should FAIL (catch regression)" 1 "$rc"

# === GAP-802 cơ chế #5 — CHECK B "LOCAL dead-link risk" (prod-domain default,
#     no local override). Inverse direction of CHECK A. ===

# Test 4 — prod-domain default WITHOUT local override.
# Expected: WARN by default (exit 0) — local-deadlink is WARN-only unless --strict-local.
rc=$(run_check "$FIXTURES/local-deadlink")
assert_exit "Fixture 4: prod-domain default, no local override → WARN, default exit 0" 0 "$rc"

# Test 4a — same fixture surfaces exactly 1 local_deadlink finding in JSON.
v=$(run_json_key "$FIXTURES/local-deadlink" local_deadlink)
assert_eq "Fixture 4a: local-deadlink JSON count = 1" 1 "$v"

# Test 4b — same fixture FAILs (exit 1) under --strict-local.
rc=$(run_check "$FIXTURES/local-deadlink" --strict-local)
assert_exit "Fixture 4b: prod-domain default, no local override → FAIL under --strict-local" 1 "$rc"

# Test 5 — prod-domain default WITH local override in kitehub/docker-compose.kitehub.yml.
# Expected: PASS (exit 0) — local override removes dead-link risk; NOT flagged.
rc=$(run_check "$FIXTURES/local-override-present")
assert_exit "Fixture 5: prod-domain default WITH local override → PASS (not flagged)" 0 "$rc"

# Test 5a — local override present → zero local_deadlink findings.
v=$(run_json_key "$FIXTURES/local-override-present" local_deadlink)
assert_eq "Fixture 5a: local-override-present JSON local_deadlink count = 0" 0 "$v"

# Test 5b — local override present stays PASS even under --strict-local.
rc=$(run_check "$FIXTURES/local-override-present" --strict-local)
assert_exit "Fixture 5b: local override present → PASS even --strict-local" 0 "$rc"

echo
echo "=== Summary ==="
echo "PASS: $PASS"
echo "FAIL: $FAIL"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
