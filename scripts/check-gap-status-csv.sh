#!/usr/bin/env bash
# check-gap-status-csv.sh — validate gap-status.csv ↔ gap file consistency
#
# Pilot mode (Phase 1 — gap-architecture-v2.md): only validates rows present in CSV,
# does NOT require every gap file to have a CSV row yet (bulk migration follow-up).
#
# Phase 2 (post bulk-migrate): flip GAP_FILES_OPTIONAL=false → require 100% coverage.
#
# Exit codes:
#   0 — all checks pass
#   1 — CSV row points to missing file
#   2 — CSV malformed (missing required columns, bad status/priority enum)
#   3 — duplicate id in CSV

set -euo pipefail

CSV="documents/04-quality/gaps/gap-status.csv"
GAPS_DIR="documents/04-quality/gaps"
GAP_FILES_OPTIONAL=true  # Phase 1 pilot — flip to false post bulk-migrate

# Enum validation
VALID_STATUSES="OPEN PARTIAL PLANNED IN_PROGRESS DONE WONTFIX"
VALID_PRIORITIES="P0 P1 P2 P3"
VALID_PHASES="phase-1-beta phase-1.5-paid phase-2 phase-3 n/a"

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found"
  exit 1
fi

# Strip comment lines + header
ROWS=$(grep -v '^#' "$CSV" | grep -v '^id,' | grep -v '^$' || true)
ROW_COUNT=$(echo "$ROWS" | grep -c '^GAP-' || echo 0)

if [[ "$ROW_COUNT" -eq 0 ]]; then
  echo "FAIL: $CSV has no data rows"
  exit 2
fi

echo "Checking $ROW_COUNT CSV rows..."

ERRORS=0
SEEN_IDS=""

while IFS=, read -r ID FILENAME TITLE STATUS PRIORITY DOMAIN PHASE COMPLETION FOUND LAST_VERIFIED NOTES; do
  [[ "$ID" =~ ^GAP- ]] || continue

  # Duplicate ID check
  if [[ " $SEEN_IDS " == *" $ID "* ]]; then
    echo "FAIL: duplicate id $ID in CSV"
    ERRORS=$((ERRORS + 1))
    continue
  fi
  SEEN_IDS="$SEEN_IDS $ID"

  # File existence check (also check closed/ subfolder)
  FILE_PATH="$GAPS_DIR/$FILENAME"
  CLOSED_PATH="$GAPS_DIR/closed/$FILENAME"
  if [[ ! -f "$FILE_PATH" && ! -f "$CLOSED_PATH" ]]; then
    echo "FAIL: $ID — file not found ($FILENAME)"
    ERRORS=$((ERRORS + 1))
    continue
  fi

  # Enum checks
  if [[ " $VALID_STATUSES " != *" $STATUS "* ]]; then
    echo "FAIL: $ID — invalid status '$STATUS' (allowed: $VALID_STATUSES)"
    ERRORS=$((ERRORS + 1))
  fi
  if [[ " $VALID_PRIORITIES " != *" $PRIORITY "* ]]; then
    echo "FAIL: $ID — invalid priority '$PRIORITY' (allowed: $VALID_PRIORITIES)"
    ERRORS=$((ERRORS + 1))
  fi
  if [[ " $VALID_PHASES " != *" $PHASE "* ]]; then
    echo "FAIL: $ID — invalid phase '$PHASE' (allowed: $VALID_PHASES)"
    ERRORS=$((ERRORS + 1))
  fi

  # Completion sanity
  if ! [[ "$COMPLETION" =~ ^[0-9]+$ ]] || [[ "$COMPLETION" -gt 100 ]]; then
    echo "FAIL: $ID — completion_pct must be 0-100, got '$COMPLETION'"
    ERRORS=$((ERRORS + 1))
  fi

  # Status ↔ completion consistency
  case "$STATUS" in
    OPEN)        [[ "$COMPLETION" == "0" ]] || echo "WARN: $ID — OPEN should have completion_pct=0 (got $COMPLETION)" ;;
    DONE)        [[ "$COMPLETION" == "100" ]] || echo "WARN: $ID — DONE should have completion_pct=100 (got $COMPLETION)" ;;
    PARTIAL|IN_PROGRESS) [[ "$COMPLETION" -gt 0 && "$COMPLETION" -lt 100 ]] || echo "WARN: $ID — PARTIAL/IN_PROGRESS should have 0<completion_pct<100 (got $COMPLETION)" ;;
  esac

  # Date format
  if ! [[ "$FOUND" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    echo "FAIL: $ID — bad found_date format '$FOUND' (expect YYYY-MM-DD)"
    ERRORS=$((ERRORS + 1))
  fi
  if ! [[ "$LAST_VERIFIED" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    echo "FAIL: $ID — bad last_verified format '$LAST_VERIFIED' (expect YYYY-MM-DD)"
    ERRORS=$((ERRORS + 1))
  fi
done <<< "$ROWS"

# Phase 2 check: every active gap file has CSV row
if [[ "$GAP_FILES_OPTIONAL" == "false" ]]; then
  ACTIVE_FILES=$(find "$GAPS_DIR" -maxdepth 1 -name "GAP-*.md" -type f | xargs -n1 basename)
  for FILE in $ACTIVE_FILES; do
    ID=$(echo "$FILE" | sed -E 's/^(GAP-[0-9]+[a-z]*).*/\1/')
    if ! grep -q "^$ID," "$CSV"; then
      echo "FAIL: $FILE missing CSV row (Phase 2 100%-coverage mode)"
      ERRORS=$((ERRORS + 1))
    fi
  done
fi

if [[ "$ERRORS" -gt 0 ]]; then
  echo "FAIL: $ERRORS error(s)"
  exit 1
fi

echo "PASS: $ROW_COUNT CSV rows validated"
exit 0
