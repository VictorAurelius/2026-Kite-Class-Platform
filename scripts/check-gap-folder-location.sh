#!/usr/bin/env bash
# check-gap-folder-location.sh — validate gap file location mirrors CSV status+phase
#
# Per `.claude/rules/gap-folder-organization.md` §2: file location is a
# projection of CSV `status` + `phase` columns onto the filesystem.
#
# Modes:
#   --strict       Exit 1 on any mismatch (target mode after PR2 mass migration)
#   --warn         Print mismatches, exit 0 (initial mode through PR1 → PR2)
#   --report-only  Print full table + counts, exit 0 (manual analysis)
#
# Default: --warn (transition mode).
#
# Exit codes:
#   0 — all OK OR warn/report mode
#   1 — strict mode + mismatches found OR script error
#   2 — CSV malformed / missing

set -euo pipefail

CSV="documents/04-quality/gaps/gap-status.csv"
GAPS_DIR="documents/04-quality/gaps"
MODE="${1:---warn}"

case "$MODE" in
  --strict|--warn|--report-only) ;;
  *)
    echo "Usage: $0 [--strict|--warn|--report-only]"
    exit 1
    ;;
esac

if [[ ! -f "$CSV" ]]; then
  echo "FAIL: $CSV not found"
  exit 2
fi

# Compute expected subdir per §2 taxonomy
expected_subdir() {
  local status="$1" phase="$2"
  case "$status" in
    DONE)        echo "closed" ;;
    PENDING)     echo "pending" ;;
    PARTIAL|IN_PROGRESS) echo "partial" ;;
    WONTFIX)     echo "wontfix" ;;
    OPEN|PLANNED)
      case "$phase" in
        phase-1-beta)   echo "phase-1-beta" ;;
        phase-1.5-paid) echo "phase-1.5-paid" ;;
        phase-2)        echo "phase-2" ;;
        phase-3)        echo "phase-3" ;;
        n/a)            echo "unclassified" ;;
        *)              echo "UNKNOWN-PHASE" ;;
      esac
      ;;
    *) echo "UNKNOWN-STATUS" ;;
  esac
}

# Counters
TOTAL_ROWS=0
OK_COUNT=0
MISMATCH_COUNT=0
MISSING_FILE_COUNT=0
declare -A MISMATCH_BY_SUBDIR

# Collect mismatches
MISMATCHES=()
MISSING_FILES=()

while IFS=, read -r ID FILENAME TITLE STATUS PRIORITY DOMAIN PHASE COMPLETION FOUND LAST_VERIFIED NOTES; do
  [[ "$ID" =~ ^GAP- ]] || continue
  TOTAL_ROWS=$((TOTAL_ROWS + 1))

  EXPECTED_SUBDIR=$(expected_subdir "$STATUS" "$PHASE")

  # Extract actual subdir from filename column (first path segment, or "" if no /)
  if [[ "$FILENAME" == */* ]]; then
    ACTUAL_SUBDIR="${FILENAME%%/*}"
  else
    ACTUAL_SUBDIR=""
  fi

  if [[ "$ACTUAL_SUBDIR" != "$EXPECTED_SUBDIR" ]]; then
    MISMATCHES+=("$ID|status=$STATUS phase=$PHASE|csv-says=${ACTUAL_SUBDIR:-<root>}|expected=$EXPECTED_SUBDIR")
    MISMATCH_COUNT=$((MISMATCH_COUNT + 1))
    MISMATCH_BY_SUBDIR["$EXPECTED_SUBDIR"]=$((${MISMATCH_BY_SUBDIR["$EXPECTED_SUBDIR"]:-0} + 1))
  else
    OK_COUNT=$((OK_COUNT + 1))
  fi

  # File existence check (CSV filename column → actual filesystem)
  FULLPATH="$GAPS_DIR/$FILENAME"
  if [[ ! -f "$FULLPATH" ]]; then
    MISSING_FILES+=("$ID|csv-points-to=$FILENAME")
    MISSING_FILE_COUNT=$((MISSING_FILE_COUNT + 1))
  fi
done < <(grep -v '^#' "$CSV" | grep -v '^$' || true)

# Report
echo "=== Gap folder location report ($(date +%Y-%m-%d)) ==="
echo "CSV rows total:           $TOTAL_ROWS"
echo "Files at expected subdir: $OK_COUNT"
echo "Files MISPLACED:          $MISMATCH_COUNT"
echo "CSV→file missing:         $MISSING_FILE_COUNT"
echo ""

if [[ "$MISMATCH_COUNT" -gt 0 ]]; then
  echo "--- Mismatch breakdown by expected subdir ---"
  for subdir in closed pending partial wontfix phase-1-beta phase-1.5-paid phase-2 phase-3 unclassified UNKNOWN-PHASE UNKNOWN-STATUS; do
    count="${MISMATCH_BY_SUBDIR[$subdir]:-0}"
    [[ "$count" -gt 0 ]] && printf "  · should be in %-18s: %d\n" "$subdir/" "$count"
  done
  echo ""
fi

if [[ "$MODE" == "--report-only" ]] && [[ "$MISMATCH_COUNT" -gt 0 ]]; then
  echo "--- First 20 mismatches (sample) ---"
  printf "%s\n" "${MISMATCHES[@]:0:20}"
  echo ""
fi

if [[ "$MISSING_FILE_COUNT" -gt 0 ]]; then
  echo "--- CSV rows pointing to missing files (first 10) ---"
  printf "%s\n" "${MISSING_FILES[@]:0:10}"
  echo ""
fi

# Exit decision
if [[ "$MISMATCH_COUNT" -eq 0 ]] && [[ "$MISSING_FILE_COUNT" -eq 0 ]]; then
  echo "✅ PASS — all gap files at correct location per gap-folder-organization.md §2"
  exit 0
fi

case "$MODE" in
  --strict)
    echo "❌ FAIL (strict mode) — $MISMATCH_COUNT misplaced + $MISSING_FILE_COUNT missing"
    echo "   Fix: git mv files to correct subdir + update CSV filename column"
    echo "   Reference: .claude/rules/gap-folder-organization.md §3 lifecycle events"
    exit 1
    ;;
  --warn)
    echo "⚠️  WARN — $MISMATCH_COUNT misplaced + $MISSING_FILE_COUNT missing"
    echo "   Initial WARN mode active. Strict mode flips after Wave 95 PR2 mass migration."
    echo "   Reference: .claude/rules/gap-folder-organization.md §5.1"
    exit 0
    ;;
  --report-only)
    echo "📊 REPORT-ONLY — $MISMATCH_COUNT misplaced + $MISSING_FILE_COUNT missing"
    exit 0
    ;;
esac
