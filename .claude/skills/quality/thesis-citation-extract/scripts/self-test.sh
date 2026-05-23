#!/usr/bin/env bash
# self-test.sh — Synthetic fixture validation for extract + verify scripts.
#
# Builds 3 synthetic fixture sets in a tmp dir:
#   1. good     — body cites [1] + bib has entry [1]            → expect PASS exit 0
#   2. bad-orphan-body — body cites [99] + bib has NO [99]      → expect FAIL exit 1
#   3. bad-orphan-bib  — body uses [1], bib has [1] AND orphan [5] → expect WARN exit 2
#
# Plus parser correctness check on range + list patterns.
#
# Exit codes:
#   0 — all 3 fixtures behave as expected
#   1 — at least 1 fixture mismatch (skill logic bug)

set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
EXTRACT="$SCRIPT_DIR/extract-citations.sh"
VERIFY="$SCRIPT_DIR/verify-citations.sh"

TMP_DIR="$(mktemp -d -t thesis-cite-selftest-XXXXXX)"
trap 'rm -rf "$TMP_DIR"' EXIT

PASS=0
FAIL=0
FAIL_LINES=""

assert() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [ "$expected" = "$actual" ]; then
    PASS=$((PASS + 1))
    printf '  [PASS] %s (expected=%s, actual=%s)\n' "$label" "$expected" "$actual"
  else
    FAIL=$((FAIL + 1))
    FAIL_LINES="${FAIL_LINES}  [FAIL] $label (expected=$expected, actual=$actual)\n"
    printf '  [FAIL] %s (expected=%s, actual=%s)\n' "$label" "$expected" "$actual"
  fi
}

echo "## Thesis Citation Extract — Self-Test ($(date '+%Y-%m-%d %H:%M:%S'))"
echo "Tmp dir: $TMP_DIR"
echo

# ---------- Fixture 1: GOOD ----------
echo "### Fixture 1: good (body cites [1], bib has [1])"
cat > "$TMP_DIR/good-chapter.md" <<'EOF'
# Good fixture chapter
Theo nghiên cứu [1], multi-tenant SaaS có advantage cost.
EOF

cat > "$TMP_DIR/good-bib.md" <<'EOF'
# Bibliography

[1] A. Author, "Sample Paper," Journal X, 2024.
EOF

set +e
bash "$VERIFY" "$TMP_DIR/good-chapter.md" "$TMP_DIR/good-bib.md" > "$TMP_DIR/good.out" 2>&1
EXIT_CODE=$?
set -e
assert "fixture-good exit code" "0" "$EXIT_CODE"
grep -q "PASS (0 orphan" "$TMP_DIR/good.out" && assert "fixture-good verdict line" "found" "found" \
  || assert "fixture-good verdict line" "found" "missing"
echo

# ---------- Fixture 2: BAD orphan-body ----------
echo "### Fixture 2: bad-orphan-body (body cites [99], bib has NO [99])"
cat > "$TMP_DIR/bad-body-chapter.md" <<'EOF'
# Bad fixture orphan-body
Theo nghiên cứu [99], có broken reference.
EOF

cat > "$TMP_DIR/bad-body-bib.md" <<'EOF'
# Bibliography

[1] A. Author, "Sample Paper," Journal X, 2024.
EOF

set +e
bash "$VERIFY" "$TMP_DIR/bad-body-chapter.md" "$TMP_DIR/bad-body-bib.md" > "$TMP_DIR/bad-body.out" 2>&1
EXIT_CODE=$?
set -e
assert "fixture-bad-orphan-body exit code" "1" "$EXIT_CODE"
grep -q "orphan-body" "$TMP_DIR/bad-body.out" && grep -q "99" "$TMP_DIR/bad-body.out" \
  && assert "fixture-bad-orphan-body reported key 99" "found" "found" \
  || assert "fixture-bad-orphan-body reported key 99" "found" "missing"
echo

# ---------- Fixture 3: BAD orphan-bib ----------
echo "### Fixture 3: bad-orphan-bib (bib has [1]+[5], body cites only [1])"
cat > "$TMP_DIR/bad-bib-chapter.md" <<'EOF'
# Bad fixture orphan-bib
Theo nghiên cứu [1], có entry orphan trong bib không được cite ở body.
EOF

cat > "$TMP_DIR/bad-bib-bib.md" <<'EOF'
# Bibliography

[1] A. Author, "Sample Paper," Journal X, 2024.

[5] B. Author, "Orphan Paper," Journal Y, 2024.
EOF

set +e
bash "$VERIFY" "$TMP_DIR/bad-bib-chapter.md" "$TMP_DIR/bad-bib-bib.md" > "$TMP_DIR/bad-bib.out" 2>&1
EXIT_CODE=$?
set -e
assert "fixture-bad-orphan-bib exit code" "2" "$EXIT_CODE"
grep -q "orphan-bib" "$TMP_DIR/bad-bib.out" && grep -q "WARN (orphan-bib" "$TMP_DIR/bad-bib.out" \
  && assert "fixture-bad-orphan-bib WARN verdict" "found" "found" \
  || assert "fixture-bad-orphan-bib WARN verdict" "found" "missing"
echo

# ---------- Parser correctness ----------
echo "### Parser correctness (extract logic)"
cat > "$TMP_DIR/parser-fixture.md" <<'EOF'
# Parser fixture
Single cite [1]. Multiple [2, 3]. Range [5]–[7]. Hyphen range [10]-[12].
Markdown link [google](https://google.com) should NOT be cited.
```
code block [99] should be skipped
```
Mixed list comma noskip [4,8]. Trailing space [11 , 14].
EOF

EXTRACT_OUT=$(bash "$EXTRACT" "$TMP_DIR/parser-fixture.md" | awk -F':' '{print $NF}' | sort -un)

# Expected keys: 1, 2, 3, 5, 6, 7, 10, 11, 12, 4, 8, 14
# Sorted: 1 2 3 4 5 6 7 8 10 11 12 14
EXPECTED="1 2 3 4 5 6 7 8 10 11 12 14"
ACTUAL=$(echo "$EXTRACT_OUT" | tr '\n' ' ' | sed 's/ $//')
assert "parser extracted keys" "$EXPECTED" "$ACTUAL"

# Verify [99] in code block was NOT extracted
if echo "$EXTRACT_OUT" | grep -q '^99$'; then
  assert "parser skip code block [99]" "skipped" "EXTRACTED (bug)"
else
  assert "parser skip code block [99]" "skipped" "skipped"
fi

# Verify markdown link [google] was NOT extracted (non-numeric inner)
if grep -q ':google$' <(bash "$EXTRACT" "$TMP_DIR/parser-fixture.md"); then
  assert "parser skip markdown link [google]" "skipped" "EXTRACTED (bug)"
else
  assert "parser skip markdown link [google]" "skipped" "skipped"
fi
echo

# ---------- Summary ----------
echo "## Summary"
printf 'PASS: %d\n' "$PASS"
printf 'FAIL: %d\n' "$FAIL"
echo

if [ "$FAIL" -gt 0 ]; then
  echo "Failures:"
  printf '%b' "$FAIL_LINES"
  exit 1
fi

echo "Self-test PASS — extract + verify logic working correctly."
exit 0
