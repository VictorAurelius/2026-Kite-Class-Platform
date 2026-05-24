#!/usr/bin/env bash
# test-audit-vn-sample-fixtures.sh — self-test cho audit-vn-sample-fixtures.sh
#
# Per .claude/rules/incident-to-rule-pipeline.md §2 Stage 4 self-test mandate.
# Tests 3 fixture scenarios:
#   1. CLEAN          — VN sample data only → expected 0 findings (PASS)
#   2. KNOWN-BAD      — English placeholders + USD currency → expected ≥4 findings (WARN)
#   3. ACCEPTABLE-EN  — English technical tokens + brand names → expected 0 findings (PASS)
#
# Usage:
#   bash scripts/tests/test-audit-vn-sample-fixtures.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FIXTURE_DIR="$SCRIPT_DIR/fixtures/audit-vn-sample-fixtures"
AUDIT_SCRIPT="$REPO_ROOT/scripts/audit-vn-sample-fixtures.sh"

PASS_COUNT=0
FAIL_COUNT=0

setup_synthetic_repo() {
  local fixture_name="$1"
  local synthetic_root="$2"

  mkdir -p "$synthetic_root/kitehub/kitehub-email/src/test/java/com/kitehub/email"
  mkdir -p "$synthetic_root/documents"

  # Copy fixture content into expected scan paths (mimic real repo layout)
  if [[ -d "$FIXTURE_DIR/$fixture_name" ]]; then
    for f in "$FIXTURE_DIR/$fixture_name"/*.java; do
      [[ -e "$f" ]] || continue
      cp "$f" "$synthetic_root/kitehub/kitehub-email/src/test/java/com/kitehub/email/"
    done
    for f in "$FIXTURE_DIR/$fixture_name"/*.md; do
      [[ -e "$f" ]] || continue
      cp "$f" "$synthetic_root/documents/"
    done
  fi
}

run_test() {
  local fixture_name="$1"
  local expected_outcome="$2"   # PASS or WARN
  local expected_min_findings="$3"  # for WARN, minimum count expected

  echo ""
  echo "--- Test: $fixture_name (expected $expected_outcome, ≥$expected_min_findings findings if WARN)"

  local synthetic_root
  synthetic_root=$(mktemp -d)
  trap "rm -rf '$synthetic_root'" RETURN

  setup_synthetic_repo "$fixture_name" "$synthetic_root"

  local output
  output=$(AUDIT_ROOT="$synthetic_root" bash "$AUDIT_SCRIPT" 2>&1)
  local rc=$?

  # Always expect exit 0 in WARN-mode v1.0.0
  if [[ $rc -ne 0 ]]; then
    echo "  ❌ FAIL: script exited $rc (expected 0 in WARN-mode)"
    FAIL_COUNT=$((FAIL_COUNT + 1))
    rm -rf "$synthetic_root"
    return
  fi

  # Count "[ENGLISH_NAME]" etc. finding lines (excluding banner / suggestion text)
  local finding_count
  finding_count=$(echo "$output" | grep -cE '^\[(ENGLISH_NAME|ENGLISH_PLACE|ENGLISH_CLASS|LOREM_IPSUM|USD_CURRENCY)\]' || true)

  if [[ "$expected_outcome" == "PASS" ]]; then
    if [[ "$finding_count" -eq 0 ]]; then
      echo "  ✅ PASS: 0 findings as expected"
      PASS_COUNT=$((PASS_COUNT + 1))
    else
      echo "  ❌ FAIL: expected 0 findings, got $finding_count"
      echo "$output" | grep -E '^\[' | head -10
      FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
  elif [[ "$expected_outcome" == "WARN" ]]; then
    if [[ "$finding_count" -ge "$expected_min_findings" ]]; then
      echo "  ✅ PASS: $finding_count findings detected (≥$expected_min_findings expected)"
      PASS_COUNT=$((PASS_COUNT + 1))
    else
      echo "  ❌ FAIL: expected ≥$expected_min_findings findings, got $finding_count"
      echo "$output" | tail -20
      FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
  fi

  rm -rf "$synthetic_root"
}

echo "==============================================================================="
echo "  Self-test: audit-vn-sample-fixtures.sh (Wave beta-readiness-4 Bucket E)"
echo "==============================================================================="

# Fixture 1: CLEAN — VN sample data only
run_test "clean" "PASS" "0"

# Fixture 2: KNOWN-BAD — English placeholders + USD currency
#   Expected findings minimum: 4 (John Doe, Jane Doe, Example Center, Class A1, $60.00, 720 USD, Lorem ipsum)
#   Threshold 4 is conservative — actual ~6-7
run_test "known-bad" "WARN" "4"

# Fixture 3: ACCEPTABLE-ENGLISH — English technical tokens + brand names
#   Expected 0 — JWT/HTTP/OAuth/brand names should NOT trigger anti-patterns
run_test "acceptable-english" "PASS" "0"

echo ""
echo "==============================================================================="
echo "  Self-test summary: $PASS_COUNT PASS / $FAIL_COUNT FAIL"
echo "==============================================================================="

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  exit 1
fi
exit 0
