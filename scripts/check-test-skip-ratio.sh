#!/usr/bin/env bash
# check-test-skip-ratio.sh — frontend test skip-ratio WARN detector (GAP-346)
#
# GAP-346 class-of-debt: skipped tests pass CI without executing → false
# confidence. A "565 passed" badge hides 206 skipped tests (26.7% skip ratio
# in kiteclass-frontend, far above the <5% industry-healthy threshold). This
# script surfaces the skip ratio per frontend project so the number is VISIBLE
# in CI, instead of buried mid-stream in vitest output.
#
# Scope (GAP-346 Bucket C — CI warning mechanism, bounded + safe):
#   - This script COUNTS + WARNS only. It NEVER fails CI (always exit 0 unless
#     a usage/IO error). Bulk un-skip is DEFERRED (see GAP-346 Log + follow-up
#     gap stub) per `incident-to-rule-pipeline.md` premature-rule guard
#     (WARN-first; HARD STOP only after the un-skip backlog is cleared).
#
# Counting method (deterministic, documented here so the ratio is reproducible):
#   - SKIPPED = count of skip call-sites matching the regex
#         \.skip\(            (covers it.skip / test.skip / describe.skip /
#                              it.only-less, vi-test .skip variants)
#     NOTE: a whole `describe.skip(...)` block disables every test inside it,
#     but statically (without executing vitest) we cannot expand the block to
#     its member count. So one `describe.skip` counts as 1 skip-site here. This
#     UNDER-counts true skipped-test volume (vitest reports 206 skipped tests
#     vs ~77 .skip( call-sites in kiteclass-frontend). The ratio below is
#     therefore a CALL-SITE ratio (skip-sites / test-sites), a stable proxy
#     that needs no running stack. Treat thresholds accordingly.
#   - TOTAL_SITES = count of leaf test call-sites matching
#         \b(it|test)\(       (excludes `describe(` container blocks, which are
#                              groupings not assertions)
#     Skip call-sites ARE a subset of these when written `it.skip(` / `test.skip(`
#     (the `\b(it|test)\(` regex matches `it.skip(`? No — `it.skip(` has `.skip`
#     between `it` and `(`, so `it(` is NOT matched. We therefore add skip-sites
#     to the denominator so the ratio is skip / (active + skip).)
#   - RATIO = SKIPPED / (TOTAL_SITES + SKIPPED) * 100   (call-site skip ratio)
#
# Thresholds (call-site ratio):
#   - ratio  > 15%  → 🔴 HIGH-WARN   (kiteclass-frontend currently here)
#   - ratio  >  5%  → 🟡 WARN
#   - ratio <=  5%  → 🟢 OK
#
# Override: PR body / commit body trailer
#   TEST_SKIP_RATIO_OVERRIDE: <reason>
# (informational only; this script does not block, but the trailer documents
#  an intentional acceptance of a high ratio for a given PR.)
#
# Exit codes:
#   0 — always (WARN mode); ratio printed regardless of threshold
#   2 — usage / IO error (e.g. a scanned source root is missing)
#
# Self-test:
#   bash scripts/check-test-skip-ratio.sh --self-test
#   Builds synthetic fixtures with known skip/total counts and asserts the
#   computed ratio + threshold band match expectations.
#
# Usage:
#   bash scripts/check-test-skip-ratio.sh            # human-readable per project
#   bash scripts/check-test-skip-ratio.sh --json     # machine-readable JSON
#   bash scripts/check-test-skip-ratio.sh --self-test

set -uo pipefail

ROOT="${SKIP_RATIO_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"

# Frontend project source roots to scan (project_label:relative_path)
PROJECTS=(
  "kiteclass-frontend:kiteclass/kiteclass-frontend/src"
  "kitehub-frontend:kitehub/kitehub-frontend/src"
)

WARN_THRESHOLD=5     # % — above this = WARN
HIGH_THRESHOLD=15    # % — above this = HIGH-WARN

# count_pattern <regex> <dir> — count regex matches across .test/.spec source.
# Uses grep -roE so each occurrence on a line counts (multiple per line rare
# but handled). Returns 0 on missing dir (caller validates existence).
count_pattern() {
  local pattern="$1" dir="$2"
  [[ -d "$dir" ]] || { echo 0; return; }
  grep -roE "$pattern" "$dir" 2>/dev/null | wc -l | tr -d ' '
}

# compute_band <ratio_int> — echo OK|WARN|HIGH for an integer percent.
compute_band() {
  local r="$1"
  if [[ "$r" -gt "$HIGH_THRESHOLD" ]]; then
    echo "HIGH"
  elif [[ "$r" -gt "$WARN_THRESHOLD" ]]; then
    echo "WARN"
  else
    echo "OK"
  fi
}

# analyze_project <label> <abs_dir> — prints one report line; sets globals
#   LAST_SKIPPED LAST_ACTIVE LAST_RATIO LAST_BAND for the JSON aggregator.
analyze_project() {
  local label="$1" dir="$2"
  local skipped active total ratio band

  skipped=$(count_pattern '\.skip\(' "$dir")
  # active leaf test sites: it( / test( but NOT it.skip( / test.skip(
  active=$(count_pattern '\b(it|test)\(' "$dir")
  total=$((active + skipped))

  if [[ "$total" -eq 0 ]]; then
    ratio=0
  else
    # integer percent (round half-down via *100/total)
    ratio=$(( skipped * 100 / total ))
  fi
  band=$(compute_band "$ratio")

  LAST_SKIPPED="$skipped"
  LAST_ACTIVE="$active"
  LAST_RATIO="$ratio"
  LAST_BAND="$band"

  local icon
  case "$band" in
    HIGH) icon="🔴 HIGH-WARN" ;;
    WARN) icon="🟡 WARN" ;;
    *)    icon="🟢 OK" ;;
  esac

  printf '  %-20s skip=%-4s active=%-5s ratio=%s%% (call-site)  %s\n' \
    "$label" "$skipped" "$active" "$ratio" "$icon"
}

run_report() {
  local json_mode="${1:-0}"
  echo "=== Frontend test skip-ratio (GAP-346 — call-site ratio, WARN mode) ==="
  echo "    threshold: >${WARN_THRESHOLD}% WARN · >${HIGH_THRESHOLD}% HIGH-WARN"
  echo ""

  local json_rows=()
  local any_high=0

  for entry in "${PROJECTS[@]}"; do
    local label="${entry%%:*}"
    local rel="${entry#*:}"
    local dir="$ROOT/$rel"
    if [[ ! -d "$dir" ]]; then
      printf '  %-20s (source dir missing: %s — skipped)\n' "$label" "$rel"
      continue
    fi
    analyze_project "$label" "$dir"
    [[ "$LAST_BAND" == "HIGH" ]] && any_high=1
    json_rows+=("{\"project\":\"$label\",\"skipped\":$LAST_SKIPPED,\"active\":$LAST_ACTIVE,\"ratio_pct\":$LAST_RATIO,\"band\":\"$LAST_BAND\"}")
  done

  echo ""
  if [[ "$any_high" -eq 1 ]]; then
    echo "  ⚠️  At least one project exceeds the ${HIGH_THRESHOLD}% HIGH-WARN band."
    echo "      This is a WARN-only check (CI not blocked). Bulk un-skip is"
    echo "      tracked separately — see GAP-346 + follow-up gap."
  fi

  if [[ "$json_mode" -eq 1 ]]; then
    echo ""
    local IFS=','
    echo "{\"projects\":[${json_rows[*]}],\"warn_threshold\":$WARN_THRESHOLD,\"high_threshold\":$HIGH_THRESHOLD}"
  fi

  return 0
}

# ----------------------------------------------------------------------------
# Self-test — synthetic fixtures with known skip/active counts.
# ----------------------------------------------------------------------------
self_test() {
  local tmp
  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' RETURN

  # Fixture A: 2 skipped + 18 active = 10% ratio → WARN band
  mkdir -p "$tmp/projA/src"
  {
    for i in $(seq 1 18); do echo "  it('active test $i', () => {});"; done
    echo "  it.skip('skipped A', () => {});"
    echo "  test.skip('skipped B', () => {});"
  } > "$tmp/projA/src/a.test.tsx"

  # Fixture B: 0 skipped + 10 active = 0% ratio → OK band
  mkdir -p "$tmp/projB/src"
  {
    for i in $(seq 1 10); do echo "  test('active $i', () => {});"; done
  } > "$tmp/projB/src/b.test.tsx"

  # Fixture C: 6 skipped + 4 active = 60% ratio → HIGH band
  mkdir -p "$tmp/projC/src"
  {
    for i in $(seq 1 4); do echo "  it('active $i', () => {});"; done
    for i in $(seq 1 5); do echo "  it.skip('s $i', () => {});"; done
    echo "  describe.skip('grp', () => {});"
  } > "$tmp/projC/src/c.test.tsx"

  local fail=0

  # --- Fixture A ---
  analyze_project "projA" "$tmp/projA/src"
  if [[ "$LAST_SKIPPED" == "2" && "$LAST_ACTIVE" == "18" && "$LAST_RATIO" == "10" && "$LAST_BAND" == "WARN" ]]; then
    echo "✅ Fixture A: 2 skip / 18 active = 10% WARN"
  else
    echo "❌ Fixture A: got skip=$LAST_SKIPPED active=$LAST_ACTIVE ratio=$LAST_RATIO band=$LAST_BAND (expected 2/18/10/WARN)"
    fail=1
  fi

  # --- Fixture B ---
  analyze_project "projB" "$tmp/projB/src"
  if [[ "$LAST_SKIPPED" == "0" && "$LAST_ACTIVE" == "10" && "$LAST_RATIO" == "0" && "$LAST_BAND" == "OK" ]]; then
    echo "✅ Fixture B: 0 skip / 10 active = 0% OK"
  else
    echo "❌ Fixture B: got skip=$LAST_SKIPPED active=$LAST_ACTIVE ratio=$LAST_RATIO band=$LAST_BAND (expected 0/10/0/OK)"
    fail=1
  fi

  # --- Fixture C: 6 skip (.skip( matches 5 it.skip + 1 describe.skip), 4 active, 6/(4+6)=60% HIGH
  analyze_project "projC" "$tmp/projC/src"
  if [[ "$LAST_SKIPPED" == "6" && "$LAST_ACTIVE" == "4" && "$LAST_RATIO" == "60" && "$LAST_BAND" == "HIGH" ]]; then
    echo "✅ Fixture C: 6 skip / 4 active = 60% HIGH-WARN"
  else
    echo "❌ Fixture C: got skip=$LAST_SKIPPED active=$LAST_ACTIVE ratio=$LAST_RATIO band=$LAST_BAND (expected 6/4/60/HIGH)"
    fail=1
  fi

  # --- Missing dir tolerance ---
  analyze_project "projMissing" "$tmp/does-not-exist"
  if [[ "$LAST_SKIPPED" == "0" && "$LAST_ACTIVE" == "0" && "$LAST_RATIO" == "0" ]]; then
    echo "✅ Missing dir: counts 0, ratio 0 (no crash)"
  else
    echo "❌ Missing dir: got skip=$LAST_SKIPPED active=$LAST_ACTIVE ratio=$LAST_RATIO"
    fail=1
  fi

  echo ""
  if [[ "$fail" -eq 0 ]]; then
    echo "🟢 self-test PASS"
    return 0
  else
    echo "🔴 self-test FAIL"
    return 1
  fi
}

# ----------------------------------------------------------------------------
case "${1:-}" in
  --self-test) self_test ;;
  --json)      run_report 1 ;;
  "")          run_report 0 ;;
  *)
    echo "Usage: $0 [--json|--self-test]" >&2
    exit 2
    ;;
esac
