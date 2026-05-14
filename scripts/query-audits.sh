#!/usr/bin/env bash
# query-audits.sh — fast audit queries from audits-index.csv (per `.claude/rules/meta-csv-index-pattern.md`)
#
# Usage:
#   bash scripts/query-audits.sh                         # show all rows (pretty)
#   bash scripts/query-audits.sh security                # filter by category
#   bash scripts/query-audits.sh "" 2026-05              # filter by date prefix
#   bash scripts/query-audits.sh security 2026-05        # category + date
#   bash scripts/query-audits.sh --count meta            # count only
#   bash scripts/query-audits.sh --grep wave-40          # id/filename/note substring (case-insensitive)
#
# Token cost: ~50 tokens per call vs ~500+ tokens per audit file read.

set -euo pipefail

CSV="documents/04-quality/audits/audits-index.csv"

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found"
  exit 1
fi

CATEGORY=""
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
CATEGORY="${POSITIONAL[0]:-}"
DATE_PREFIX="${POSITIONAL[1]:-}"

# Strip header + comments
ROWS=$(grep -v '^#' "$CSV" | grep -v '^id,' | grep -v '^$' || true)

# Apply filters
[[ -n "$CATEGORY" ]]    && ROWS=$(echo "$ROWS" | awk -F, -v c="$CATEGORY" '$3==c')
[[ -n "$DATE_PREFIX" ]] && ROWS=$(echo "$ROWS" | awk -F, -v d="$DATE_PREFIX" 'index($6,d)==1')
if [[ -n "$GREP_TERM" ]]; then
  ROWS=$(echo "$ROWS" | grep -i -- "$GREP_TERM" || true)
fi

if [[ "$COUNT" == "true" ]]; then
  echo "$ROWS" | grep -c '^AUDIT-' || echo 0
  exit 0
fi

# Pretty print: id | category | date | wave | status | score
echo "$ROWS" | awk -F, '{printf "%-60s | %-22s | %-10s | %-6s | %-10s | %s\n", $1, $3, $6, $5, $7, $8}'
