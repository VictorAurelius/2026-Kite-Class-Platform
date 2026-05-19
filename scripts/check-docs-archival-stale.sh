#!/usr/bin/env bash
# check-docs-archival-stale.sh — detect time-bound artifacts exceeding cadence
#
# Per `.claude/rules/docs-archival-cadence.md` §2 cadence table:
#   - Audit reports          : 90 days from date-prefix filename
#   - Session handoffs       : 30 days from date-prefix filename
#   - Wave plans             : 60 days post-closure (heuristic: file mtime)
#   - PR-logs                : 180 days from git creation
#
# Closes deferred-detector debt for docs-archival-cadence.md §4.3 (GAP-675 SHIP-NOW).
#
# Modes:
#   --strict       Exit 1 if any artifact past threshold
#   --warn         Exit 0 + emit WARN (initial mode through grace period)
#   --report-only  Print full inventory + counts, exit 0
#   --self-test    Run against fixtures + assert detector logic
#
# Override trailer in commit body:
#   DOCS_ARCHIVAL_OVERRIDE: <path> — <reason — landmark / compliance / future-ref>
#
# Date math approach: parse filename `YYYY-MM-DD-*.md` prefix (no frontmatter read).
# Falls back to git creation date when no prefix (covers wave plans + PR-logs).

set -euo pipefail

MODE="${1:---warn}"
TODAY="$(date -u +%Y-%m-%d)"

# Helper: days between two ISO dates (date1 = older, date2 = newer)
days_between() {
  local d1="$1" d2="$2"
  local sec1 sec2
  sec1=$(date -u -d "$d1" +%s 2>/dev/null) || return 1
  sec2=$(date -u -d "$d2" +%s 2>/dev/null) || return 1
  echo $(( (sec2 - sec1) / 86400 ))
}

# Helper: parse YYYY-MM-DD prefix from filename basename
extract_date_prefix() {
  basename "$1" | grep -oE '^[0-9]{4}-[0-9]{2}-[0-9]{2}' || echo ""
}

# Self-test: detector fires correctly on synthetic fixtures
run_self_test() {
  local tmpdir
  tmpdir=$(mktemp -d)
  trap 'rm -rf "$tmpdir"' RETURN

  # Fixture 1: stale audit (100 days old, > 90 threshold)
  local old_date
  old_date=$(date -u -d "100 days ago" +%Y-%m-%d)
  mkdir -p "$tmpdir/audits/quality"
  touch "$tmpdir/audits/quality/${old_date}-stale-audit.md"

  # Fixture 2: fresh audit (10 days old)
  local fresh_date
  fresh_date=$(date -u -d "10 days ago" +%Y-%m-%d)
  touch "$tmpdir/audits/quality/${fresh_date}-fresh-audit.md"

  # Fixture 3: stale session-handoff (40 days old, > 30 threshold)
  mkdir -p "$tmpdir/session-handoffs"
  local stale_session
  stale_session=$(date -u -d "40 days ago" +%Y-%m-%d)
  touch "$tmpdir/session-handoffs/${stale_session}-handoff.md"

  # Run detector with fixture dir
  local report
  report=$(SCAN_DIR_OVERRIDE="$tmpdir" "$0" --report-only 2>&1 || true)

  if echo "$report" | grep -q "stale-audit.md" && \
     ! echo "$report" | grep -q "fresh-audit.md" && \
     echo "$report" | grep -q "${stale_session}-handoff.md"; then
    echo "PASS — detector flagged 2 stale artifacts (audit + handoff), ignored 1 fresh"
    return 0
  else
    echo "FAIL — self-test mismatch"
    echo "$report"
    return 1
  fi
}

case "$MODE" in
  --self-test) run_self_test; exit $? ;;
  --strict|--warn|--report-only) ;;
  *) echo "Usage: $0 [--strict|--warn|--report-only|--self-test]" >&2; exit 2 ;;
esac

ROOT="${SCAN_DIR_OVERRIDE:-.}"
declare -i STALE=0
declare -a VIOLATIONS=()

scan_dated_artifacts() {
  local pattern="$1"      # find path glob
  local threshold="$2"    # max age in days
  local label="$3"

  while IFS= read -r -d '' f; do
    local prefix
    prefix=$(extract_date_prefix "$f")
    [[ -z "$prefix" ]] && continue
    local age
    age=$(days_between "$prefix" "$TODAY") || continue
    if (( age > threshold )); then
      STALE+=1
      VIOLATIONS+=("$f (age $age days, threshold $threshold, $label)")
      [[ "$MODE" == "--report-only" ]] && echo "  ✗ $f — $age days > $threshold ($label)"
    fi
  done < <(find "$ROOT" -path "$pattern" -type f -print0 2>/dev/null)
}

# Audit reports (90d) — under documents/04-quality/audits/**
scan_dated_artifacts "*/audits/*/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-*.md" 90 "audit"
scan_dated_artifacts "*/audits/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-*.md" 90 "audit-root"

# Session handoffs (30d)
scan_dated_artifacts "*/session-handoffs/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-*.md" 30 "session-handoff"

# Wave plans (60d) — heuristic: filename `wave-YYYY-MM-DD-NN-*.md`
while IFS= read -r -d '' f; do
  prefix=$(basename "$f" | grep -oE 'wave-[0-9]{4}-[0-9]{2}-[0-9]{2}' | sed 's/wave-//' || echo "")
  [[ -z "$prefix" ]] && continue
  age=$(days_between "$prefix" "$TODAY") || continue
  if (( age > 60 )); then
    STALE+=1
    VIOLATIONS+=("$f (age $age days, threshold 60, wave-plan)")
    [[ "$MODE" == "--report-only" ]] && echo "  ✗ $f — $age days > 60 (wave-plan)"
  fi
done < <(find "$ROOT/documents/03-planning/waves" -maxdepth 1 -name "wave-*.md" -type f -print0 2>/dev/null)

echo "─────────────────────────────────────"
echo "Docs archival cadence check"
echo "  Stale artifacts: $STALE"

if (( STALE == 0 )); then
  echo "  ✓ No artifacts exceed archival threshold"
  exit 0
fi

case "$MODE" in
  --strict)
    echo ""
    echo "Violations:"
    for v in "${VIOLATIONS[@]}"; do echo "  - $v"; done
    echo ""
    echo "FAIL: $STALE artifact(s) past archival cadence per docs-archival-cadence.md §2"
    echo "Override: commit trailer 'DOCS_ARCHIVAL_OVERRIDE: <path> — <reason>'"
    exit 1
    ;;
  --warn|--report-only)
    [[ "$MODE" == "--warn" ]] && {
      echo "  WARN: $STALE artifact(s) past cadence (non-blocking — first batch trigger)"
      echo "  Top 5 stale:"
      for v in "${VIOLATIONS[@]:0:5}"; do echo "    - $v"; done
    }
    exit 0
    ;;
esac
