#!/usr/bin/env bash
# check-audits-index-csv.sh — validate audits-index.csv ↔ audit file consistency
#
# Per `.claude/rules/meta-csv-index-pattern.md`: CSV is canonical for audit
# enumeration (id, filename, category, date, status, score). Every audit file
# under `documents/04-quality/audits/**/*.md` (excluding README/template) MUST
# have a CSV row; every CSV row MUST point to an existing audit file.
#
# Schema: id,filename,category,phase,wave,date,status,score,note
# Category enums: ui-review | quality-audit | security | performance | api-contract |
#                 ops-readiness | business-logic | meta | aws-verification |
#                 outside-in-benchmark
# Status enums: complete | partial | draft | superseded
#
# Exit codes:
#   0 — all checks pass
#   1 — CSV row points to missing file OR audit file lacks CSV row
#   2 — CSV malformed (missing column, invalid enum)
#   3 — duplicate id in CSV

set -euo pipefail

CSV="documents/04-quality/audits/audits-index.csv"
AUDIT_DIR="documents/04-quality/audits"

VALID_CATEGORIES="ui-review quality-audit security performance api-contract ops-readiness business-logic meta aws-verification cloudflare-verification outside-in-benchmark persona-review"
VALID_STATUSES="complete partial draft superseded"

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found"
  exit 1
fi

# Strip comments + header
ROWS=$(grep -v '^#' "$CSV" | grep -v '^id,' | grep -v '^$' || true)
ROW_COUNT=$(echo "$ROWS" | grep -c '^AUDIT-' || echo 0)

if [[ "$ROW_COUNT" -eq 0 ]]; then
  echo "FAIL: $CSV has no data rows"
  exit 2
fi

echo "Checking $ROW_COUNT audit CSV rows..."

ERRORS=0
SEEN_IDS=""

# shellcheck disable=SC2034  # PHASE/WAVE/SCORE/NOTE intentionally read for column position; not asserted
while IFS=, read -r ID FILENAME CATEGORY PHASE WAVE DATE STATUS SCORE NOTE; do
  [[ "$ID" =~ ^AUDIT- ]] || continue

  # Duplicate id check
  if [[ " $SEEN_IDS " == *" $ID "* ]]; then
    echo "FAIL: duplicate id $ID in CSV"
    ERRORS=$((ERRORS + 1))
    continue
  fi
  SEEN_IDS="$SEEN_IDS $ID"

  # File existence
  FILE_PATH="$AUDIT_DIR/$FILENAME"
  if [[ ! -f "$FILE_PATH" ]]; then
    echo "FAIL: $ID — file not found ($FILENAME)"
    ERRORS=$((ERRORS + 1))
    continue
  fi

  # Category enum
  if [[ " $VALID_CATEGORIES " != *" $CATEGORY "* ]]; then
    echo "FAIL: $ID — invalid category '$CATEGORY' (allowed: $VALID_CATEGORIES)"
    ERRORS=$((ERRORS + 1))
  fi

  # Status enum
  if [[ " $VALID_STATUSES " != *" $STATUS "* ]]; then
    echo "FAIL: $ID — invalid status '$STATUS' (allowed: $VALID_STATUSES)"
    ERRORS=$((ERRORS + 1))
  fi

  # Date format
  if ! [[ "$DATE" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    echo "FAIL: $ID — bad date format '$DATE' (expect YYYY-MM-DD)"
    ERRORS=$((ERRORS + 1))
  fi
done <<< "$ROWS"

# Coverage check: every audit *.md (excluding README/template) has a CSV row
AUDIT_FILES=$(find "$AUDIT_DIR" -name '*.md' -type f \
  ! -name 'README.md' ! -name '_TEMPLATE.md' ! -name '_REVIEW-TEMPLATE.md' \
  | sed "s|^$AUDIT_DIR/||" | sort)

while IFS= read -r FILE; do
  [[ -z "$FILE" ]] && continue
  # awk match on column 2 (filename) — escape commas/quotes simple compare
  if ! awk -F, -v f="$FILE" '
    BEGIN { found = 0 }
    {
      # strip surrounding quotes from field 2 if present
      val = $2
      gsub(/^"|"$/, "", val)
      if (val == f) found = 1
    }
    END { exit !found }
  ' "$CSV"; then
    echo "FAIL: $FILE missing CSV row (100%-coverage mode)"
    ERRORS=$((ERRORS + 1))
  fi
done <<< "$AUDIT_FILES"

if [[ "$ERRORS" -gt 0 ]]; then
  echo "FAIL: $ERRORS error(s)"
  exit 1
fi

echo "PASS: $ROW_COUNT audit rows validated"
exit 0
