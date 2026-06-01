#!/usr/bin/env bash
# test-seed-data-vn-content.sh — assert kitehub/scripts/seed-data.sh sử dụng VN sample data
#
# Per .claude/rules/vn-localization-audit-checklist.md §2 row 3 (VN sample data)
# Per GAP-538 AC8 (sample seed data Vietnamese-friendly)
# Per wave-2026-06-01-onboarding-polish-2-execute.md §3 Bucket D
#
# Asserts:
#   1. Students array contains ≥3 VN-diacritic names (markers: Nguyễn/Trần/Lê/Phạm/Hoàng/...)
#   2. Teachers array contains ≥2 VN names
#   3. Courses array contains ≥3 VN course tokens (Toán/Văn/Anh/Lý/Tin/Sử/Địa/Hóa/Sinh)
#   4. ZERO English-only placeholder course names (English Basics, IELTS Preparation, etc.)
#
# Usage:
#   bash scripts/tests/test-seed-data-vn-content.sh

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SEED_SCRIPT="$REPO_ROOT/kitehub/scripts/seed-data.sh"

PASS_COUNT=0
FAIL_COUNT=0

assert_pass() {
  local name="$1"
  PASS_COUNT=$((PASS_COUNT + 1))
  echo "  ✅ $name"
}

assert_fail() {
  local name="$1"
  local detail="$2"
  FAIL_COUNT=$((FAIL_COUNT + 1))
  echo "  ❌ $name"
  echo "      $detail"
}

echo ""
echo "=============================================="
echo "  Test: seed-data.sh VN sample content"
echo "=============================================="

if [[ ! -f "$SEED_SCRIPT" ]]; then
  echo "FATAL: $SEED_SCRIPT not found"
  exit 2
fi

# Test 1 — student VN names
vn_student_count=$(grep -cE 'Nguyễn|Trần|Lê|Phạm|Hoàng|Đặng|Vũ|Bùi|Đỗ|Ngô' "$SEED_SCRIPT")

if [[ "$vn_student_count" -ge 5 ]]; then
  assert_pass "Test 1: VN diacritic name markers ≥5 (found $vn_student_count)"
else
  assert_fail "Test 1: VN name markers <5" "found $vn_student_count, expected ≥5"
fi

# Test 2 — VN K-12 / language course tokens
vn_course_tokens=$(grep -cE '"name":"(Toán|Văn|Tiếng Anh|Vật lý|Tin học|Lịch sử|Địa lý|Hóa|Sinh|Khoa học|Anh văn)' "$SEED_SCRIPT")

if [[ "$vn_course_tokens" -ge 3 ]]; then
  assert_pass "Test 2: VN course name tokens ≥3 (found $vn_course_tokens)"
else
  assert_fail "Test 2: VN course tokens <3" "found $vn_course_tokens, expected ≥3"
fi

# Test 3 — zero English-only placeholder course names (banned per VN audit)
banned_course_names=(
  '"name":"English Basics"'
  '"name":"IELTS Preparation"'
  '"name":"Business English"'
  '"name":"Kids English"'
)

english_placeholder_hits=0
for banned in "${banned_course_names[@]}"; do
  if grep -qF "$banned" "$SEED_SCRIPT"; then
    english_placeholder_hits=$((english_placeholder_hits + 1))
    echo "      banned placeholder still present: $banned"
  fi
done

if [[ "$english_placeholder_hits" -eq 0 ]]; then
  assert_pass "Test 3: zero banned English placeholder course names"
else
  assert_fail "Test 3: banned English course placeholders present" "$english_placeholder_hits hits"
fi

# Test 4 — teachers array has VN names
vn_teacher_lines=$(awk '/^  local teachers=\(/,/^  \)/' "$SEED_SCRIPT" \
  | grep -cE 'Nguyễn|Trần|Lê|Phạm|Hoàng|Đặng|Vũ|Bùi')

if [[ "$vn_teacher_lines" -ge 2 ]]; then
  assert_pass "Test 4: teachers array has ≥2 VN names (found $vn_teacher_lines)"
else
  assert_fail "Test 4: teachers array <2 VN names" "found $vn_teacher_lines"
fi

echo ""
echo "=============================================="
echo "  Result: $PASS_COUNT passed, $FAIL_COUNT failed"
echo "=============================================="

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  exit 1
fi

exit 0
