#!/usr/bin/env bash
# sync-gap-csv-from-ac.sh
#
# Detects drift between gap file Acceptance Criteria checkbox state and
# gap-status.csv `completion_pct` column. WARN if delta >10 percentage points.
#
# Closes CSV/AC auto-sync mechanism (GAP-822) per Wave meta-8 Bucket D.
# Per `meta-gap-priority.md` §3 META P1 force-multiplier — single chuẩn AC
# completion auto-derived eliminates manual pct upkeep + retroactive audit cost.
#
# Modes:
#   --report      Generate full drift report (default)
#   --warn        WARN-only CI mode; never exit non-zero
#   --hard-stop   Exit 1 if any gap >10pp drift (target post-30d grace)
#   --self-test   Run synthetic fixtures
#   --threshold N Override 10pp drift threshold
#
# Per `incident-to-rule-pipeline.md` §3.1 self-test mandate.

set -euo pipefail

MODE="report"
THRESHOLD=10
case "${1:-}" in
  --report) MODE="report" ;;
  --warn) MODE="warn" ;;
  --hard-stop) MODE="hard-stop" ;;
  --self-test) MODE="self-test" ;;
  --threshold)
    THRESHOLD="${2:-10}"; MODE="report" ;;
  -h|--help)
    sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
    exit 0 ;;
esac

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GAPS_DIR="${REPO_ROOT}/documents/04-quality/gaps"
CSV="${GAPS_DIR}/gap-status.csv"
TODAY=$(date +%Y-%m-%d)

count_ac() {
  # Args: gap file path
  # Output: "checked total" via stdout
  local f="$1"
  local checked total
  # Scope: lines under `## Acceptance Criteria` heading up to next H2 OR EOF
  checked=$(awk '/^## Acceptance Criteria/{flag=1; next} /^## /{flag=0} flag' "$f" 2>/dev/null \
    | grep -cE '^- \[[xX]\]' || true)
  total=$(awk '/^## Acceptance Criteria/{flag=1; next} /^## /{flag=0} flag' "$f" 2>/dev/null \
    | grep -cE '^- \[[ xX]\]' || true)
  echo "$checked $total"
}

run_check() {
  local gaps_dir="$1"
  local csv="$2"
  local drift_count=0
  local total_checked=0

  if [[ ! -f "$csv" ]]; then
    echo "WARN: gap-status.csv not found at $csv" >&2
    return 0
  fi

  # Build CSV id → completion_pct map
  declare -A csv_pct
  declare -A csv_filename
  while IFS=',' read -r id filename rest; do
    [[ "$id" =~ ^GAP- ]] || continue
    csv_filename["$id"]="$filename"
  done < "$csv"

  # Parse CSV with Python for proper quoting (notes column has commas)
  while IFS=$'\t' read -r id pct; do
    [[ -z "$id" || -z "$pct" ]] && continue
    csv_pct["$id"]="$pct"
  done < <(python3 -c "
import csv, sys
with open('$csv') as f:
    lines = f.readlines()
hdr = next(i for i, l in enumerate(lines) if l.startswith('id,filename,'))
reader = csv.DictReader(lines[hdr:])
for row in reader:
    if row['completion_pct'].isdigit():
        print(f\"{row['id']}\t{row['completion_pct']}\")
")

  echo "# Gap CSV ↔ AC drift report ($(date -u +%Y-%m-%dT%H:%M:%SZ))"
  echo ""
  echo "**Threshold:** ${THRESHOLD}pp"
  echo ""
  echo "| Gap | File | AC | CSV pct | AC-derived pct | Δ |"
  echo "|---|---|---|---|---|---|"

  for id in "${!csv_filename[@]}"; do
    filename="${csv_filename[$id]}"
    f="${gaps_dir}/${filename}"
    [[ -f "$f" ]] || continue
    read -r checked total < <(count_ac "$f")
    total_checked=$((total_checked+1))
    [[ "$total" -gt 0 ]] || continue
    ac_pct=$(( checked * 100 / total ))
    csv_p="${csv_pct[$id]:-0}"
    delta=$(( ac_pct - csv_p ))
    abs_delta=${delta#-}
    if [[ $abs_delta -gt $THRESHOLD ]]; then
      echo "| $id | $filename | $checked/$total | $csv_p% | $ac_pct% | ${delta:+$delta}pp |"
      drift_count=$((drift_count+1))
    fi
  done

  echo ""
  echo "**Drift count:** $drift_count of $total_checked gaps audited (threshold ${THRESHOLD}pp)"

  if [[ $drift_count -gt 0 && "$MODE" == "hard-stop" ]]; then
    return 1
  fi
  return 0
}

self_test() {
  local tmpdir
  tmpdir=$(mktemp -d)
  trap "rm -rf $tmpdir" RETURN

  mkdir -p "$tmpdir/gaps"
  # Compliant gap: 3/5 AC = 60%, CSV says 60% → no drift
  cat > "$tmpdir/gaps/GAP-901-compliant.md" <<EOF
# GAP-901

## Acceptance Criteria

- [x] AC 1
- [x] AC 2
- [x] AC 3
- [ ] AC 4
- [ ] AC 5

## Log
EOF

  # Drift gap: 1/4 AC = 25%, CSV says 80% → 55pp drift WARN
  cat > "$tmpdir/gaps/GAP-902-drift.md" <<EOF
# GAP-902

## Acceptance Criteria

- [x] AC 1
- [ ] AC 2
- [ ] AC 3
- [ ] AC 4

## Log
EOF

  cat > "$tmpdir/gaps/gap-status.csv" <<EOF
id,filename,title_short,status,priority,domain,phase,completion_pct,found_date,last_verified,notes
GAP-901,GAP-901-compliant.md,Compliant,PARTIAL,P1,Meta,phase-1-beta,60,2026-06-01,2026-06-01,
GAP-902,GAP-902-drift.md,Drift,PARTIAL,P1,Meta,phase-1-beta,80,2026-06-01,2026-06-01,
EOF

  echo "=== Self-test: 1 compliant + 1 drift (55pp) expected ==="
  local output
  output=$(run_check "$tmpdir/gaps" "$tmpdir/gaps/gap-status.csv" 2>&1) || true
  echo "$output"

  if echo "$output" | grep -q "GAP-902" && \
     ! echo "$output" | grep -q "| GAP-901 |"; then
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

run_check "$GAPS_DIR" "$CSV"
exit $?
