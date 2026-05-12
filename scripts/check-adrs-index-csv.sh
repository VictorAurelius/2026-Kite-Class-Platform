#!/usr/bin/env bash
# check-adrs-index-csv.sh — validate adrs-index.csv ↔ ADR file consistency
#
# Per `.claude/rules/meta-csv-index-pattern.md`: CSV is canonical for ADR
# enumeration (number, status, date, title pointer). Every ADR file MUST have
# a CSV row; every CSV row MUST point to an existing ADR file.
#
# Schema: number,status,date,title,file
# Enums: PROPOSED | ACCEPTED | DEPRECATED | SUPERSEDED | REJECTED
#
# Exit codes:
#   0 — all checks pass
#   1 — CSV row points to missing file OR ADR file lacks CSV row
#   2 — CSV malformed (missing column, invalid enum)
#   3 — duplicate number in CSV

set -euo pipefail

CSV="documents/02-architecture/adr/adrs-index.csv"
ADR_DIR="documents/02-architecture/adr"

VALID_STATUSES="PROPOSED ACCEPTED DEPRECATED SUPERSEDED REJECTED"

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found"
  exit 1
fi

ROWS=$(grep -v '^#' "$CSV" | grep -v '^number,' | grep -v '^$' || true)
ROW_COUNT=$(echo "$ROWS" | grep -c '^[0-9]' || echo 0)

if [[ "$ROW_COUNT" -eq 0 ]]; then
  echo "FAIL: $CSV has no data rows"
  exit 2
fi

echo "Checking $ROW_COUNT ADR CSV rows..."

ERRORS=0
SEEN_NUMBERS=""

while IFS=, read -r NUMBER STATUS DATE TITLE FILE; do
  [[ "$NUMBER" =~ ^[0-9]+$ ]] || continue

  # Duplicate number check
  if [[ " $SEEN_NUMBERS " == *" $NUMBER "* ]]; then
    echo "FAIL: duplicate ADR number $NUMBER in CSV"
    ERRORS=$((ERRORS + 1))
    continue
  fi
  SEEN_NUMBERS="$SEEN_NUMBERS $NUMBER"

  # File existence
  FILE_PATH="$ADR_DIR/$FILE"
  if [[ ! -f "$FILE_PATH" ]]; then
    echo "FAIL: ADR-$NUMBER — file not found ($FILE)"
    ERRORS=$((ERRORS + 1))
    continue
  fi

  # Status enum
  if [[ " $VALID_STATUSES " != *" $STATUS "* ]]; then
    echo "FAIL: ADR-$NUMBER — invalid status '$STATUS' (allowed: $VALID_STATUSES)"
    ERRORS=$((ERRORS + 1))
  fi

  # Date format
  if ! [[ "$DATE" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    echo "FAIL: ADR-$NUMBER — bad date format '$DATE' (expect YYYY-MM-DD)"
    ERRORS=$((ERRORS + 1))
  fi

  # Title non-empty
  if [[ -z "$TITLE" ]]; then
    echo "FAIL: ADR-$NUMBER — empty title"
    ERRORS=$((ERRORS + 1))
  fi
done <<< "$ROWS"

# Coverage check: every ADR-NNN-*.md file has a CSV row
ADR_FILES=$(find "$ADR_DIR" -maxdepth 1 -name "ADR-[0-9]*.md" -type f | xargs -n1 basename)
for FILE in $ADR_FILES; do
  if ! awk -F, -v f="$FILE" '$5==f {found=1} END {exit !found}' "$CSV"; then
    echo "FAIL: $FILE missing CSV row (100%-coverage mode)"
    ERRORS=$((ERRORS + 1))
  fi
done

if [[ "$ERRORS" -gt 0 ]]; then
  echo "FAIL: $ERRORS error(s)"
  exit 1
fi

echo "PASS: $ROW_COUNT ADR rows validated"
exit 0
