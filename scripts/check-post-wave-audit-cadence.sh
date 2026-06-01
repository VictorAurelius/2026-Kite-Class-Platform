#!/usr/bin/env bash
# check-post-wave-audit-cadence.sh
#
# Detects waves shipped ≥7 days ago without matching audit suite entry
# in audits-index.csv. Closes audit-cadence systemic enforcement gap
# (GAP-821) per Wave meta-8 Bucket C.
#
# Per `.claude/rules/post-wave-audit-mandate.md` §2.2 — audit suite due ≤3 days
# post-merge. This script enforces the cadence at ≥7-day failure threshold.
#
# Exit codes:
#   0 — PASS or WARN-only mode (CI flag --warn)
#   1 — FAIL (HARD STOP mode + ≥1 stale wave)
#
# Modes:
#   --warn        WARN-only; never exit non-zero (initial CI mode)
#   --hard-stop   Exit 1 if any wave ≥7d without audit (target post-30d grace)
#   --self-test   Run synthetic fixtures
#
# Per `incident-to-rule-pipeline.md` §3.1 self-test mandate.

set -euo pipefail

MODE="warn"
case "${1:-}" in
  --warn) MODE="warn" ;;
  --hard-stop) MODE="hard-stop" ;;
  --self-test) MODE="self-test" ;;
  -h|--help)
    sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
    exit 0 ;;
esac

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WAVE_HISTORY="${REPO_ROOT}/.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl"
AUDITS_INDEX="${REPO_ROOT}/documents/04-quality/audits/audits-index.csv"
GRACE_DAYS=7
TODAY=$(date +%Y-%m-%d)

run_check() {
  local wave_history="$1"
  local audits_index="$2"
  local today="$3"
  local stale_count=0

  if [[ ! -f "$wave_history" ]]; then
    echo "WARN: wave-history.jsonl not found at $wave_history" >&2
    return 0
  fi
  if [[ ! -f "$audits_index" ]]; then
    echo "WARN: audits-index.csv not found at $audits_index" >&2
    return 0
  fi

  # Extract (wave, date) from wave-history.jsonl — every line is a closure record
  while IFS=$'\t' read -r wave date; do
    [[ -z "$wave" || -z "$date" ]] && continue

    # Age check
    age=$(( ( $(date -d "$today" +%s) - $(date -d "$date" +%s) ) / 86400 ))
    [[ $age -lt $GRACE_DAYS ]] && continue

    # Find matching audit row: column 5 (wave) equals wave name
    if awk -F',' -v w="$wave" 'NR>6 && $5==w {found=1; exit} END{exit !found}' "$audits_index"; then
      :  # audit found — compliant
    else
      echo "WARN: wave='$wave' shipped $date (age ${age}d) — no audit row in audits-index.csv"
      stale_count=$((stale_count+1))
    fi
  done < <(jq -r 'select(.wave and .date) | "\(.wave)\t\(.date)"' "$wave_history" 2>/dev/null || true)

  echo "---"
  echo "Stale-cadence waves: $stale_count (grace=${GRACE_DAYS}d)"

  if [[ $stale_count -gt 0 && "$MODE" == "hard-stop" ]]; then
    return 1
  fi
  return 0
}

self_test() {
  # Synthetic fixtures
  local tmpdir
  tmpdir=$(mktemp -d)
  trap "rm -rf $tmpdir" RETURN

  # Wave history: 1 fresh (today), 1 stale-with-audit, 1 stale-without-audit
  cat > "$tmpdir/wave-history.jsonl" <<EOF
{"wave":"fresh-1","date":"$TODAY","outcome":"shipped today — fresh"}
{"wave":"compliant-1","date":"$(date -d '10 days ago' +%Y-%m-%d)","outcome":"shipped 10d ago — has audit"}
{"wave":"stale-1","date":"$(date -d '15 days ago' +%Y-%m-%d)","outcome":"shipped 15d ago — no audit (should WARN)"}
EOF

  # Audits index: header (6 comment lines + 1 schema row) + 1 audit for compliant-1
  cat > "$tmpdir/audits-index.csv" <<EOF
# Test fixture
# Test fixture
# Test fixture
# Test fixture
# Test fixture
id,filename,category,phase,wave,date,status,score,note
AUDIT-TEST-compliant-1,test/c.md,quality,phase-1-beta,compliant-1,$(date -d '11 days ago' +%Y-%m-%d),complete,,
EOF

  echo "=== Self-test: 1 fresh skip, 1 compliant skip, 1 stale WARN expected ==="
  local output
  output=$(run_check "$tmpdir/wave-history.jsonl" "$tmpdir/audits-index.csv" "$TODAY" 2>&1) || true
  echo "$output"

  if echo "$output" | grep -q "wave='stale-1'" && \
     ! echo "$output" | grep -q "wave='fresh-1'" && \
     ! echo "$output" | grep -q "wave='compliant-1'"; then
    echo "✅ Self-test PASS"
    return 0
  else
    echo "❌ Self-test FAIL"
    return 1
  fi
}

if [[ "$MODE" == "self-test" ]]; then
  self_test
  exit $?
fi

run_check "$WAVE_HISTORY" "$AUDITS_INDEX" "$TODAY"
exit $?
