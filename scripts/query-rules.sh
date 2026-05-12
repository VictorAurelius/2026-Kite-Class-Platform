#!/usr/bin/env bash
# query-rules.sh — fast rule queries from rules-index.csv (per `.claude/rules/meta-csv-index-pattern.md`)
#
# Usage:
#   bash scripts/query-rules.sh                       # show all
#   bash scripts/query-rules.sh CRITICAL              # filter by priority
#   bash scripts/query-rules.sh "" 2026-05            # last_reviewed date prefix
#   bash scripts/query-rules.sh --count CRITICAL     # count only
#   bash scripts/query-rules.sh --grep aws            # name substring (case-insensitive)
#
# Token cost: ~30 tokens per call vs ~500+ tokens per rule file read.

set -euo pipefail

CSV=".claude/rules/rules-index.csv"

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found"
  exit 1
fi

PRIORITY=""
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
PRIORITY="${POSITIONAL[0]:-}"
DATE_PREFIX="${POSITIONAL[1]:-}"

ROWS=$(grep -v '^#' "$CSV" | grep -v '^name,' | grep -v '^$' || true)

[[ -n "$PRIORITY" ]]    && ROWS=$(echo "$ROWS" | awk -F, -v p="$PRIORITY" '$2==p')
[[ -n "$DATE_PREFIX" ]] && ROWS=$(echo "$ROWS" | awk -F, -v d="$DATE_PREFIX" 'index($5,d)==1')
if [[ -n "$GREP_TERM" ]]; then
  ROWS=$(echo "$ROWS" | grep -i -- "$GREP_TERM" || true)
fi

if [[ "$COUNT" == "true" ]]; then
  echo "$ROWS" | grep -c '^[a-z]' || echo 0
  exit 0
fi

# Pretty print: name | priority | version | last_reviewed
echo "$ROWS" | awk -F, '{printf "%-44s | %-9s | %-7s | %s\n", $1, $2, $3, $5}'
