#!/usr/bin/env bash
# query-gaps.sh — fast gap queries from gap-status.csv (Design A)
#
# Usage:
#   bash scripts/query-gaps.sh                       # show all rows
#   bash scripts/query-gaps.sh P0                    # filter priority
#   bash scripts/query-gaps.sh P0 OPEN               # priority + status
#   bash scripts/query-gaps.sh "" PARTIAL phase-1-beta   # status + phase
#   bash scripts/query-gaps.sh --count P0            # count only
#   bash scripts/query-gaps.sh --domain Frontend     # filter domain
#
# Token cost: ~50 tokens per call (1 CSV row) vs ~500 tokens per gap file read.

set -euo pipefail

CSV="documents/04-quality/gaps/gap-status.csv"

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found"
  exit 1
fi

PRIORITY=""
STATUS=""
PHASE=""
DOMAIN=""
COUNT=false

# Parse flags + positional args
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --count) COUNT=true; shift ;;
    --domain) DOMAIN="$2"; shift 2 ;;
    *) POSITIONAL+=("$1"); shift ;;
  esac
done
PRIORITY="${POSITIONAL[0]:-}"
STATUS="${POSITIONAL[1]:-}"
PHASE="${POSITIONAL[2]:-}"

# Strip header + comments
ROWS=$(grep -v '^#' "$CSV" | grep -v '^id,' | grep -v '^$' || true)

# Apply filters
[[ -n "$PRIORITY" ]] && ROWS=$(echo "$ROWS" | awk -F, -v p="$PRIORITY" '$5==p')
[[ -n "$STATUS" ]]   && ROWS=$(echo "$ROWS" | awk -F, -v s="$STATUS" '$4==s')
[[ -n "$PHASE" ]]    && ROWS=$(echo "$ROWS" | awk -F, -v ph="$PHASE" '$7==ph')
[[ -n "$DOMAIN" ]]   && ROWS=$(echo "$ROWS" | awk -F, -v d="$DOMAIN" '$6==d')

if [[ "$COUNT" == "true" ]]; then
  echo "$ROWS" | grep -c '^GAP-' || echo 0
  exit 0
fi

# Pretty print
echo "$ROWS" | awk -F, '{printf "%-12s | %-4s | %-12s | %-15s | %-15s | %3s%% | %s\n", $1, $5, $4, $7, $6, $8, $3}'
