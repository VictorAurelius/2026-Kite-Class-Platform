#!/usr/bin/env bash
# test-check-fe-be-api-contract.sh — fixture tests cho scripts/check-fe-be-api-contract.sh
#
# Per GAP-1070 cơ chế #3. Mirror 2-fixture self-test pattern từ
# scripts/tests/test-check-be-fe-url-contract.sh.
#
# Fixtures (CONTRACT_ROOT-driven synthetic repo layouts):
#   case-a/  — pre-GAP-1069: FE GET /api/v1/classes collection, BE chỉ /{id}
#              → expect ≥1 drift finding cho GET /api/v1/classes.
#   case-b/  — post-fix: BE thêm GET /api/v1/classes flat list
#              → expect 0 drift cho GET /api/v1/classes (pass cho classes).
#
# Detector luôn exit 0 (WARN-mode) → assert dựa trên --json {"checked","drift"}
# + grep finding text, KHÔNG dựa exit code.
#
# Exit codes:
#   0 — all assertions pass
#   1 — at least 1 assertion failed
set -uo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
SCRIPT="$REPO_ROOT/scripts/check-fe-be-api-contract.sh"
FIXTURES="$REPO_ROOT/scripts/tests/fixtures/fe-be-api-contract"
PASS=0
FAIL=0

assert_eq() {
  local name="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then
    echo "  PASS — $name (got '$actual')"
    PASS=$((PASS + 1))
  else
    echo "  FAIL — $name (expected '$expected', got '$actual')"
    FAIL=$((FAIL + 1))
  fi
}

assert_contains() {
  local name="$1" needle="$2" haystack="$3"
  if printf '%s' "$haystack" | grep -qF "$needle"; then
    echo "  PASS — $name (found '$needle')"
    PASS=$((PASS + 1))
  else
    echo "  FAIL — $name (missing '$needle')"
    FAIL=$((FAIL + 1))
  fi
}

assert_not_contains() {
  local name="$1" needle="$2" haystack="$3"
  if printf '%s' "$haystack" | grep -qF "$needle"; then
    echo "  FAIL — $name (unexpected '$needle')"
    FAIL=$((FAIL + 1))
  else
    echo "  PASS — $name (absent '$needle')"
    PASS=$((PASS + 1))
  fi
}

echo "=== Case A — pre-GAP-1069 (FE GET /classes collection, BE chỉ /{id}) ==="
A_JSON=$(CONTRACT_ROOT="$FIXTURES/case-a" bash "$SCRIPT" --json)
A_HUMAN=$(CONTRACT_ROOT="$FIXTURES/case-a" bash "$SCRIPT")
echo "  json: $A_JSON"
# checked=2 (listClasses + getClass), drift≥1 (GET /api/v1/classes không có BE flat list)
A_DRIFT=$(printf '%s' "$A_JSON" | sed -E 's/.*"drift":([0-9]+).*/\1/')
if [ "$A_DRIFT" -ge 1 ]; then
  echo "  PASS — Case A drift ≥1 (got $A_DRIFT)"; PASS=$((PASS + 1))
else
  echo "  FAIL — Case A expected drift ≥1, got $A_DRIFT"; FAIL=$((FAIL + 1))
fi
assert_contains "Case A flags GET /api/v1/classes" "GET /api/v1/classes (" "$A_HUMAN"

echo
echo "=== Case B — post-fix (BE thêm GET /api/v1/classes flat list) ==="
B_JSON=$(CONTRACT_ROOT="$FIXTURES/case-b" bash "$SCRIPT" --json)
B_HUMAN=$(CONTRACT_ROOT="$FIXTURES/case-b" bash "$SCRIPT")
echo "  json: $B_JSON"
assert_eq "Case B drift=0 (classes resolved)" '{"checked":2,"drift":0}' "$B_JSON"
assert_not_contains "Case B does NOT flag GET /api/v1/classes" "GET /api/v1/classes (" "$B_HUMAN"

echo
echo "=== Summary ==="
echo "  PASS: $PASS · FAIL: $FAIL"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
