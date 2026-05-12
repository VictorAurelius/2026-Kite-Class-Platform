#!/usr/bin/env bash
# query-adrs.sh — fast ADR queries from adrs-index.csv (per `.claude/rules/meta-csv-index-pattern.md`)
#
# Usage:
#   bash scripts/query-adrs.sh                        # show all rows (pretty)
#   bash scripts/query-adrs.sh ACCEPTED               # filter by status
#   bash scripts/query-adrs.sh "" 2026-05             # filter by date prefix
#   bash scripts/query-adrs.sh --count ACCEPTED       # count only
#   bash scripts/query-adrs.sh --grep outbox          # title substring (case-insensitive)
#
# Token cost: ~30 tokens per call vs ~500+ tokens per ADR file read.

set -euo pipefail

CSV="documents/02-architecture/adr/adrs-index.csv"

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found"
  exit 1
fi

STATUS=""
DATE_PREFIX=""
COUNT=false
GREP_TERM=""

POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --count) COUNT=true; shift ;;
    --grep) GREP_TERM="$2"; shift 2 ;;
    *) POSITIONAL+=("$1"); shift ;;
  esac
done
STATUS="${POSITIONAL[0]:-}"
DATE_PREFIX="${POSITIONAL[1]:-}"

# Strip header + comments
ROWS=$(grep -v '^#' "$CSV" | grep -v '^number,' | grep -v '^$' || true)

# Apply filters
[[ -n "$STATUS" ]]      && ROWS=$(echo "$ROWS" | awk -F, -v s="$STATUS" '$2==s')
[[ -n "$DATE_PREFIX" ]] && ROWS=$(echo "$ROWS" | awk -F, -v d="$DATE_PREFIX" 'index($3,d)==1')
if [[ -n "$GREP_TERM" ]]; then
  ROWS=$(echo "$ROWS" | grep -i -- "$GREP_TERM" || true)
fi

if [[ "$COUNT" == "true" ]]; then
  echo "$ROWS" | grep -c '^[0-9]' || echo 0
  exit 0
fi

# Pretty print
echo "$ROWS" | awk -F, '{printf "ADR-%-3s | %-10s | %-10s | %s\n", $1, $2, $3, $4}'
