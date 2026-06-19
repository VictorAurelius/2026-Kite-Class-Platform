#!/usr/bin/env bash
# check-gap-folder-location.sh — validate gap file location mirrors CSV phase
#
# Per `.claude/rules/gap-folder-organization.md` v2.0.0 §2:
#   - File location is a projection of CSV `phase` column.
#   - Status changes (OPEN/PARTIAL/etc.) do NOT move file — except DONE which
#     archives to `phase-X/closed/` one-way.
#   - Legacy root `closed/` orphans (no CSV row) are tolerated per §2.1.
#
# Modes:
#   --strict       Exit 1 on any mismatch (target after PR2 mass migration)
#   --warn         Print mismatches, exit 0 (initial through PR1.5 → PR2)
#   --report-only  Print full table + counts, exit 0
#
# Default: --warn.

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

# Map CSV phase value → top-level phase subdir name
phase_subdir() {
  case "$1" in
    phase-1-beta)   echo "phase-1-beta" ;;
    phase-1.5-paid) echo "phase-1.5-paid" ;;
    phase-2)        echo "phase-2" ;;
    phase-3)        echo "phase-3" ;;
    phase-4-deploy) echo "phase-4-deploy" ;;
    n/a)            echo "unclassified" ;;
    *)              echo "UNKNOWN-PHASE" ;;
  esac
}

# Compute expected file path given (status, phase) per rule v2.0.0 §2.1
expected_path() {
  local status="$1" phase="$2" filename_only="$3"
  local subdir
  subdir=$(phase_subdir "$phase")
  if [[ "$status" == "DONE" ]]; then
    echo "$subdir/closed/$filename_only"
  else
    echo "$subdir/$filename_only"
  fi
}

# Counters
TOTAL_ROWS=0
OK_COUNT=0
MISMATCH_COUNT=0
MISSING_FILE_COUNT=0
declare -A MISMATCH_BY_TARGET

MISMATCHES=()
MISSING_FILES=()

while IFS=, read -r ID FILENAME TITLE STATUS PRIORITY DOMAIN PHASE COMPLETION FOUND LAST_VERIFIED NOTES; do
  [[ "$ID" =~ ^GAP- ]] || continue
  TOTAL_ROWS=$((TOTAL_ROWS + 1))

  # Extract bare filename (last path segment)
  FILENAME_ONLY="${FILENAME##*/}"

  EXPECTED=$(expected_path "$STATUS" "$PHASE" "$FILENAME_ONLY")

  if [[ "$FILENAME" != "$EXPECTED" ]]; then
    MISMATCHES+=("$ID|status=$STATUS phase=$PHASE|csv-says=$FILENAME|expected=$EXPECTED")
    MISMATCH_COUNT=$((MISMATCH_COUNT + 1))
    EXPECTED_SUBDIR="${EXPECTED%/*}"
    MISMATCH_BY_TARGET["$EXPECTED_SUBDIR"]=$((${MISMATCH_BY_TARGET["$EXPECTED_SUBDIR"]:-0} + 1))
  else
    OK_COUNT=$((OK_COUNT + 1))
  fi

  # File existence check
  FULLPATH="$GAPS_DIR/$FILENAME"
  if [[ ! -f "$FULLPATH" ]]; then
    MISSING_FILES+=("$ID|csv-points-to=$FILENAME")
    MISSING_FILE_COUNT=$((MISSING_FILE_COUNT + 1))
  fi
done < <(grep -v '^#' "$CSV" | grep -v '^$' || true)

# Count legacy orphans in root closed/ (files present, no CSV row)
LEGACY_ORPHAN_COUNT=0
if [[ -d "$GAPS_DIR/closed" ]]; then
  while IFS= read -r fpath; do
    bare="${fpath##*/}"
    if ! grep -qE "^GAP-[^,]*,closed/$bare," "$CSV" 2>/dev/null; then
      LEGACY_ORPHAN_COUNT=$((LEGACY_ORPHAN_COUNT + 1))
    fi
  done < <(find "$GAPS_DIR/closed" -maxdepth 1 -type f -name 'GAP-*.md' 2>/dev/null)
fi

# Report
echo "=== Gap folder location report ($(date +%Y-%m-%d)) ==="
echo "Rule: gap-folder-organization.md v2.0.0 (phase-only + per-phase closed/)"
echo "CSV rows total:           $TOTAL_ROWS"
echo "Files at expected path:   $OK_COUNT"
echo "Files MISPLACED:          $MISMATCH_COUNT"
echo "CSV→file missing:         $MISSING_FILE_COUNT"
echo "Legacy orphans (root closed/, no CSV row): $LEGACY_ORPHAN_COUNT (tolerated per rule §2.1)"
echo ""

if [[ "$MISMATCH_COUNT" -gt 0 ]]; then
  echo "--- Mismatch breakdown by expected location ---"
  for target in phase-1-beta phase-1-beta/closed \
                phase-1.5-paid phase-1.5-paid/closed \
                phase-2 phase-2/closed \
                phase-3 phase-3/closed \
                phase-4-deploy phase-4-deploy/closed \
                unclassified unclassified/closed \
                UNKNOWN-PHASE UNKNOWN-PHASE/closed; do
    count="${MISMATCH_BY_TARGET[$target]:-0}"
    [[ "$count" -gt 0 ]] && printf "  · should be in %-30s: %d\n" "$target/" "$count"
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
  echo "✅ PASS — all gap files at correct location per gap-folder-organization.md v2.0.0 §2"
  exit 0
fi

case "$MODE" in
  --strict)
    echo "❌ FAIL (strict mode) — $MISMATCH_COUNT misplaced + $MISSING_FILE_COUNT missing"
    echo "   Fix: git mv files to correct phase-X/[closed/] + update CSV filename column"
    echo "   Reference: .claude/rules/gap-folder-organization.md §3 lifecycle events"
    exit 1
    ;;
  --warn)
    echo "⚠️  WARN — $MISMATCH_COUNT misplaced + $MISSING_FILE_COUNT missing"
    echo "   Initial WARN mode active. Strict mode flips after Wave 95 PR2 mass migration."
    echo "   Reference: .claude/rules/gap-folder-organization.md v2.0.0 §5.1"
    exit 0
    ;;
  --report-only)
    echo "📊 REPORT-ONLY — $MISMATCH_COUNT misplaced + $MISSING_FILE_COUNT missing"
    exit 0
    ;;
esac
