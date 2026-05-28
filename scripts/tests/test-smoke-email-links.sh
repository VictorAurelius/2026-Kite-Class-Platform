#!/usr/bin/env bash
# test-smoke-email-links.sh — fixture tests cho scripts/smoke-email-links.sh
#
# Per GAP-802 cơ chế #1. Drives the smoke check offline + deterministic by
# injecting MailHog payloads via MAILHOG_FIXTURE and link statuses via
# LINK_CHECK_FIXTURE (no live MailHog, no network).
#
# Fixtures (under scripts/tests/fixtures/smoke-email-links/):
#   good          — all links localhost + resolvable        → PASS (exit 0)
#   bad-404       — wrong FE route /signup/beta returns 404  → FAIL (exit 1)
#   bad-proddomain— local email links to kitehub.me prod     → FAIL (exit 1) in --local
#                   same payload                             → PASS (exit 0) in --prod
#
# Per `pre-handoff-self-test-completeness.md` §5.2 self-test mandate.
#
# Exit codes:
#   0 — all fixtures behave as expected
#   1 — at least 1 fixture failed its assertion
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
SCRIPT="$REPO_ROOT/scripts/smoke-email-links.sh"
FIXTURES="$REPO_ROOT/scripts/tests/fixtures/smoke-email-links"
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

# run_check <messages.json> <linkmap.txt> [extra flags...]
run_check() {
    local messages="$1" linkmap="$2"; shift 2
    set +e
    MAILHOG_FIXTURE="$messages" LINK_CHECK_FIXTURE="$linkmap" \
        bash "$SCRIPT" "$@" >/dev/null 2>&1
    local rc=$?
    set -e
    echo "$rc"
}

echo "=== test-smoke-email-links ==="

# Test 1 — known-good (localhost links, all resolvable) → PASS
rc=$(run_check "$FIXTURES/good-messages.json" "$FIXTURES/good-linkmap.txt" --local)
assert_exit "Fixture 1: good (localhost + resolvable) should PASS in --local" 0 "$rc"

# Test 2 — 404 link (wrong FE route /signup/beta) → FAIL (GAP-801 part 1/2)
rc=$(run_check "$FIXTURES/bad-404-messages.json" "$FIXTURES/bad-404-linkmap.txt" --local)
assert_exit "Fixture 2: 404 link should FAIL (catch wrong FE route)" 1 "$rc"

# Test 3 — local email links to prod domain → FAIL (GAP-801 part 3 dead-link)
rc=$(run_check "$FIXTURES/bad-proddomain-messages.json" "$FIXTURES/bad-proddomain-linkmap.txt" --local)
assert_exit "Fixture 3: prod-domain link on local should FAIL (--local)" 1 "$rc"

# Test 4 — same prod-domain payload in --prod mode → domain check skipped → PASS
rc=$(run_check "$FIXTURES/bad-proddomain-messages.json" "$FIXTURES/bad-proddomain-linkmap.txt" --prod)
assert_exit "Fixture 4: prod-domain link should PASS in --prod (domain check skipped)" 0 "$rc"

# Test 5 — --json output shape sanity (good fixture)
set +e
json_out=$(MAILHOG_FIXTURE="$FIXTURES/good-messages.json" LINK_CHECK_FIXTURE="$FIXTURES/good-linkmap.txt" \
    bash "$SCRIPT" --local --json 2>/dev/null)
json_rc=$?
set -e
if echo "$json_out" | grep -qE '^\{"checked":[0-9]+,"failed":0\}$' && [ "$json_rc" = "0" ]; then
    echo "  PASS — Fixture 5: --json output well-formed for good fixture ($json_out)"
    PASS=$((PASS + 1))
else
    echo "  FAIL — Fixture 5: --json output unexpected (got '$json_out', rc=$json_rc)"
    FAIL=$((FAIL + 1))
fi

echo
echo "=== Summary ==="
echo "PASS: $PASS"
echo "FAIL: $FAIL"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
