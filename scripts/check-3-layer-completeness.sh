#!/usr/bin/env bash
# check-3-layer-completeness.sh — verify business domain 3-layer doc presence
#
# Per CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure":
# Each domain under documents/01-business/{project}/{domain}/ MUST have ALL 3 files:
#   - rules.md       (Layer 1: Business Rules)
#   - use-cases.md   (Layer 2: Use Cases)
#   - api-contract.md (Layer 3: API Contract)
#
# Closes deferred-detector debt for `audit-to-gap-pipeline.md` §2.5 state-check
# at filing time + Wave 98 GAP-664 recurrence #2 (Wave 92 GAP-640 was #1).
#
# Modes:
#   --strict       Exit 1 on any missing layer (target post 30-day grace)
#   --warn         Exit 0 + emit WARN (initial mode through grace period)
#   --report-only  Print full domain × layer matrix + counts, exit 0
#
# Self-test:
#   --self-test    Run against synthetic fixtures, expected PASS/FAIL pre-canned
#
# Exit codes:
#   0  pass (or --warn / --report-only)
#   1  strict mode + ≥1 domain missing layer
#   2  invocation error (bad mode flag)
#
# Override trailer in commit body to allow specific domain temporarily:
#   THREE_LAYER_DEFER: <domain-path> — <reason + follow-up gap link>

set -euo pipefail

MODE="${1:---warn}"
BUSINESS_DIR="documents/01-business"
REQUIRED_LAYERS=("rules.md" "use-cases.md" "api-contract.md")

run_self_test() {
  local tmpdir
  tmpdir=$(mktemp -d)
  trap 'rm -rf "$tmpdir"' RETURN

  # Fixture 1: good (all 3 layers)
  mkdir -p "$tmpdir/kitehub/good"
  touch "$tmpdir/kitehub/good/rules.md" "$tmpdir/kitehub/good/use-cases.md" "$tmpdir/kitehub/good/api-contract.md"

  # Fixture 2: bad (missing use-cases.md)
  mkdir -p "$tmpdir/kitehub/bad-partial"
  touch "$tmpdir/kitehub/bad-partial/rules.md" "$tmpdir/kitehub/bad-partial/api-contract.md"

  # Fixture 3: bad (only api-contract.md — matches Wave 98 preferences pattern)
  mkdir -p "$tmpdir/kitehub/bad-contract-only"
  touch "$tmpdir/kitehub/bad-contract-only/api-contract.md"

  # Run with custom dir
  local report
  report=$(BUSINESS_DIR_OVERRIDE="$tmpdir" "$0" --report-only 2>&1 || true)

  # Verify: 3 domains scanned, 1 complete, 2 missing layer, 2 specific violations
  if echo "$report" | grep -qE "Domains scanned: *3" && \
     echo "$report" | grep -qE "Complete: *1" && \
     echo "$report" | grep -qE "Missing layer: *2" && \
     echo "$report" | grep -q "bad-partial" && \
     echo "$report" | grep -q "bad-contract-only"; then
    echo "PASS — self-test detected 1 good + 2 bad fixtures (counts match expected 3/1/2)"
    return 0
  else
    echo "FAIL — self-test result mismatch"
    echo "$report"
    return 1
  fi
}

case "$MODE" in
  --self-test) run_self_test; exit $? ;;
  --strict|--warn|--report-only) ;;
  *) echo "Usage: $0 [--strict|--warn|--report-only|--self-test]" >&2; exit 2 ;;
esac

# Allow self-test to override scan dir
SCAN_DIR="${BUSINESS_DIR_OVERRIDE:-$BUSINESS_DIR}"

if [[ ! -d "$SCAN_DIR" ]]; then
  echo "FAIL: $SCAN_DIR not found" >&2
  exit 1
fi

declare -i TOTAL=0
declare -i COMPLETE=0
declare -i MISSING=0
declare -a VIOLATIONS=()

while IFS= read -r -d '' domain_dir; do
  TOTAL+=1
  local_missing=()
  for layer in "${REQUIRED_LAYERS[@]}"; do
    if [[ ! -f "$domain_dir/$layer" ]]; then
      local_missing+=("$layer")
    fi
  done

  if [[ ${#local_missing[@]} -eq 0 ]]; then
    COMPLETE+=1
    [[ "$MODE" == "--report-only" ]] && echo "  ✓ $domain_dir — all 3 layers"
  else
    MISSING+=1
    rel="${domain_dir#$SCAN_DIR/}"
    marks=""
    for layer in "${REQUIRED_LAYERS[@]}"; do
      [[ -f "$domain_dir/$layer" ]] && marks+="✓ " || marks+="✗ "
    done
    VIOLATIONS+=("$rel ($marks — missing: ${local_missing[*]})")
    [[ "$MODE" == "--report-only" ]] && echo "  ✗ $rel — missing: ${local_missing[*]}"
  fi
done < <(find "$SCAN_DIR" -mindepth 2 -maxdepth 2 -type d -print0 2>/dev/null)

echo "─────────────────────────────────────"
echo "3-layer completeness check"
echo "  Domains scanned: $TOTAL"
echo "  Complete:        $COMPLETE"
echo "  Missing layer:   $MISSING"

if [[ $MISSING -eq 0 ]]; then
  echo "  ✓ All domains have full 3-layer coverage"
  exit 0
fi

echo ""
echo "Violations:"
for v in "${VIOLATIONS[@]}"; do
  echo "  - $v"
done

case "$MODE" in
  --strict)
    echo ""
    echo "FAIL: $MISSING domain(s) missing required layer(s)"
    echo "Per CLAUDE.md §3-Layer Structure: every domain MUST have rules.md + use-cases.md + api-contract.md"
    echo "Override: add commit trailer 'THREE_LAYER_DEFER: <domain> — <reason + gap link>'"
    exit 1
    ;;
  --warn|--report-only)
    echo ""
    echo "WARN: $MISSING domain(s) missing layers (non-blocking — will HARD STOP post-grace per GAP-664)"
    exit 0
    ;;
esac
