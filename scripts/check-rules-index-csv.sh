#!/usr/bin/env bash
# check-rules-index-csv.sh — validate rules-index.csv ↔ rule file consistency
#
# Per `.claude/rules/meta-csv-index-pattern.md`: CSV is canonical for rule
# enumeration (name, priority, version, dates pointer). Every rule file MUST
# have a CSV row; every CSV row MUST point to an existing rule file.
#
# Schema: name,priority,version,created,last_reviewed,file
# Enums: CRITICAL | MANDATORY | ADVISORY
#
# Note: this validator covers the INDEX layer only. Rule frontmatter content
# (Last-Reviewed ≤ today, etc.) is enforced by `scripts/check-rule-frontmatter.sh`.
#
# Exit codes:
#   0 — pass
#   1 — coverage gap or file missing
#   2 — malformed CSV / invalid enum
#   3 — duplicate row

set -euo pipefail

CSV=".claude/rules/rules-index.csv"
RULES_DIR=".claude/rules"

VALID_PRIORITIES="CRITICAL MANDATORY ADVISORY"

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found"
  exit 1
fi

ROWS=$(grep -v '^#' "$CSV" | grep -v '^name,' | grep -v '^$' || true)
ROW_COUNT=$(echo "$ROWS" | grep -c '^[a-z]' || echo 0)

if [[ "$ROW_COUNT" -eq 0 ]]; then
  echo "FAIL: $CSV has no data rows"
  exit 2
fi

echo "Checking $ROW_COUNT rule CSV rows..."

ERRORS=0
SEEN_NAMES=""

while IFS=, read -r NAME PRIORITY VERSION CREATED REVIEWED FILE; do
  [[ "$NAME" =~ ^[a-z] ]] || continue

  # Duplicate name check
  if [[ " $SEEN_NAMES " == *" $NAME "* ]]; then
    echo "FAIL: duplicate rule name $NAME in CSV"
    ERRORS=$((ERRORS + 1))
    continue
  fi
  SEEN_NAMES="$SEEN_NAMES $NAME"

  # File existence
  FILE_PATH="$RULES_DIR/$FILE"
  if [[ ! -f "$FILE_PATH" ]]; then
    echo "FAIL: $NAME — file not found ($FILE)"
    ERRORS=$((ERRORS + 1))
    continue
  fi

  # Priority enum
  if [[ " $VALID_PRIORITIES " != *" $PRIORITY "* ]]; then
    echo "FAIL: $NAME — invalid priority '$PRIORITY' (allowed: $VALID_PRIORITIES)"
    ERRORS=$((ERRORS + 1))
  fi

  # Version semver-ish (X.Y or X.Y.Z)
  if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
    echo "FAIL: $NAME — bad version '$VERSION' (expect X.Y.Z)"
    ERRORS=$((ERRORS + 1))
  fi

  # Date formats
  if ! [[ "$CREATED" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    echo "FAIL: $NAME — bad created date '$CREATED'"
    ERRORS=$((ERRORS + 1))
  fi
  if ! [[ "$REVIEWED" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    echo "FAIL: $NAME — bad last_reviewed date '$REVIEWED'"
    ERRORS=$((ERRORS + 1))
  fi

  # last_reviewed >= created sanity
  if [[ "$REVIEWED" < "$CREATED" ]]; then
    echo "FAIL: $NAME — last_reviewed ($REVIEWED) before created ($CREATED)"
    ERRORS=$((ERRORS + 1))
  fi
done <<< "$ROWS"

# Coverage check: every rule file has a CSV row
RULE_FILES=$(find "$RULES_DIR" -maxdepth 1 -name "*.md" -type f | xargs -n1 basename)
for FILE in $RULE_FILES; do
  if ! awk -F, -v f="$FILE" '$6==f {found=1} END {exit !found}' "$CSV"; then
    echo "FAIL: $FILE missing CSV row (100%-coverage mode)"
    ERRORS=$((ERRORS + 1))
  fi
done

if [[ "$ERRORS" -gt 0 ]]; then
  echo "FAIL: $ERRORS error(s)"
  exit 1
fi

echo "PASS: $ROW_COUNT rule rows validated"
exit 0
