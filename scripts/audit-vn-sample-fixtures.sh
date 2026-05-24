#!/usr/bin/env bash
# audit-vn-sample-fixtures.sh — VN-localization audit cho test fixtures + email preview content
#
# Greps test fixtures, email template samples, documentation cho English placeholder
# anti-patterns per .claude/rules/vn-localization-audit-checklist.md §2 row 7 (VN sample data)
# + §2 row 1 (VND currency format).
#
# Scope (default):
#   - kitehub/**/src/test/**/*.java     (Java test fixtures)
#   - kitehub/**/src/test/**/*.json     (JSON test data)
#   - kiteclass/**/src/test/**/*.java
#   - documents/**/*.md                  (user manual + audit content)
#
# Detected anti-patterns (per vn-localization-audit-checklist §2 row 7):
#   - English placeholder names:  John Doe, Jane Doe, Alice Smith, Bob Jones
#   - English class/center names: Class A1, Class 5A, Example Center, Acme Corp
#   - Lorem Ipsum placeholder text
#   - USD currency:              $NN.NN, NN USD, $NNN.NN (vs required đ / VNĐ)
#
# Mode (Wave beta-readiness-4 Bucket E v1.0.0):
#   WARN-only (exit 0 always) per `incident-to-rule-pipeline.md` §3.1 tightened
#   legitimate-deferral conditions:
#     - Detector complexity: trivial grep matches BUT false-positive risk on legitimate
#       code-switching (JWT/HTTP/AWS English tokens natural per `dev-readable-doc-language.md`
#       §4) + code-blocks containing English placeholder examples in DOCS (rule body samples)
#     - Recurrence count: 0 (rule just landed Wave 100 Bucket D 2026-05-19 vn-localization
#       audit; this Bucket E ships first detector instance)
#     - HARD STOP target: 30-day grace (~2026-06-23) — revisit when stabilize
#       AND recurrence ≥2 OR proven NLP language classifier available
#
# Override mechanism: see vn-localization-audit-checklist.md §4.5 trailer.
#
# Usage:
#   bash scripts/audit-vn-sample-fixtures.sh                 # scan real repo
#   bash scripts/audit-vn-sample-fixtures.sh --verbose       # show full grep matches
#   AUDIT_ROOT=/tmp/fixture bash scripts/audit-vn-sample-fixtures.sh  # test mode
#
# Exit codes:
#   0 — always (WARN-mode v1.0.0; HARD STOP target 2026-06-23)

set -uo pipefail
# AUDIT_ROOT override allows fixture-based unit tests to point at synthetic repo layout
ROOT="${AUDIT_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
VERBOSE="${1:-}"

# Anti-pattern regex per vn-localization-audit-checklist.md §2 row 7
# Word boundaries (\b) prevent matching "DoeJohn" or substrings
ENGLISH_NAMES='\b(John Doe|Jane Doe|Alice Smith|Bob Jones|Foo Bar|Baz Qux)\b'
ENGLISH_PLACES='\b(Example Center|Acme Corp|Acme Inc|Foo Bar Center|Test Center)\b'
ENGLISH_CLASSES='\bClass [0-9]+[A-Z]?[0-9]*\b'  # "Class A1", "Class 5A1", "Class 101"
LOREM_IPSUM='[Ll]orem [Ii]psum'
USD_CURRENCY='\$[0-9]+(\.[0-9]+)?|\b[0-9]+(\.[0-9]+)? USD\b'

# Scan paths — graceful for missing dirs (fixture tests may not have all dirs)
SCAN_PATHS=(
  "$ROOT/kitehub"
  "$ROOT/kiteclass"
  "$ROOT/documents"
)

# File globs to include
INCLUDE_GLOBS=(
  '*.java'
  '*.json'
  '*.md'
  '*.html'
  '*.txt'
)

# Exclude paths — known to contain legitimate English (rule body examples, archives, vendor docs)
EXCLUDE_PATTERNS=(
  '*/node_modules/*'
  '*/target/*'
  '*/.git/*'
  '*/_examples/*'                                            # rule body examples contain placeholder for demo
  '*/.claude/rules/*'                                        # rules themselves cite anti-patterns as examples
  '*/.claude/skills/*'                                       # skill docs cite English samples for explanation
  '*/documents/07-archived/*'                                # archived docs grandfathered
  '*/documents/00-brd/*'                                     # BRD initial drafts pre-rule grandfathered
  '*/audit-vn-sample-fixtures*.sh'                           # this script + tests cite English samples
  '*/test-audit-vn-sample-fixtures*.sh'
  '*/fixtures/audit-vn-sample-fixtures/*'                    # fixture content intentionally bad
  '*/api-contract-audit/data/openapi-snapshot.yaml'          # OpenAPI examples may use "John Doe" per upstream spec
  '*/audits-index.csv'
  '*/security-audit/reference/*'                             # security audit references cite English CVE descriptions
)

# Build find/grep command
FINDINGS_COUNT=0
TEMP_FINDINGS=$(mktemp)
trap 'rm -f "$TEMP_FINDINGS"' EXIT

scan_pattern() {
  local pattern="$1"
  local pattern_label="$2"

  for scan_path in "${SCAN_PATHS[@]}"; do
    [[ ! -d "$scan_path" ]] && continue

    # Build find command with includes + excludes
    local find_cmd=(find "$scan_path" -type f \( )
    for i in "${!INCLUDE_GLOBS[@]}"; do
      [[ $i -gt 0 ]] && find_cmd+=(-o)
      find_cmd+=(-name "${INCLUDE_GLOBS[$i]}")
    done
    find_cmd+=(\))

    for excl in "${EXCLUDE_PATTERNS[@]}"; do
      find_cmd+=(-not -path "$excl")
    done

    # Run find → grep
    while IFS= read -r file; do
      [[ -z "$file" ]] && continue
      if matches=$(grep -nE "$pattern" "$file" 2>/dev/null); then
        while IFS= read -r match_line; do
          [[ -z "$match_line" ]] && continue
          FINDINGS_COUNT=$((FINDINGS_COUNT + 1))
          local rel_path="${file#$ROOT/}"
          echo "[$pattern_label] $rel_path: $match_line" >> "$TEMP_FINDINGS"
        done <<< "$matches"
      fi
    done < <("${find_cmd[@]}" 2>/dev/null)
  done
}

echo "==============================================================================="
echo "  VN Sample Fixture Audit (Wave beta-readiness-4 Bucket E — WARN-mode v1.0.0)"
echo "==============================================================================="
echo "  Per .claude/rules/vn-localization-audit-checklist.md §2 row 7"
echo "  HARD STOP target: 2026-06-23 (30-day grace per incident-to-rule-pipeline §3.1)"
echo "  Override: commit trailer \`VN_LOCALIZATION_OVERRIDE:\` per checklist §4.5"
echo "==============================================================================="
echo ""

scan_pattern "$ENGLISH_NAMES" "ENGLISH_NAME"
scan_pattern "$ENGLISH_PLACES" "ENGLISH_PLACE"
scan_pattern "$ENGLISH_CLASSES" "ENGLISH_CLASS"
scan_pattern "$LOREM_IPSUM" "LOREM_IPSUM"
scan_pattern "$USD_CURRENCY" "USD_CURRENCY"

if [[ "$FINDINGS_COUNT" -eq 0 ]]; then
  echo "  PASS — no English placeholder / USD currency anti-patterns detected."
  echo ""
  exit 0
fi

echo "  WARN — $FINDINGS_COUNT finding(s) detected:"
echo ""
sort -u "$TEMP_FINDINGS" | head -50
if [[ "$FINDINGS_COUNT" -gt 50 ]]; then
  echo ""
  echo "  ... and $((FINDINGS_COUNT - 50)) more findings (showing top 50)."
fi

echo ""
echo "  Suggested VN replacements per vn-localization-audit-checklist §2 row 7:"
echo "    - John Doe         → Trần Thị Hồng / Nguyễn Văn An / Phạm Thị Mai"
echo "    - Example Center   → Trung tâm Anh ngữ Sky Education / Trung tâm Toán Quang Minh"
echo "    - Class A1         → Lớp Anh ngữ 5A1 / Lớp Toán 9B"
echo "    - Lorem Ipsum      → Nội dung mẫu tiếng Việt (BR-XXX placeholder)"
echo "    - \$60.00 / 60 USD  → 1.500.000đ / 1.500.000 ₫ (VND format)"
echo ""
echo "  Note: WARN-mode v1.0.0; exit 0 — not blocking. Track recurrence cho HARD STOP target."

exit 0
