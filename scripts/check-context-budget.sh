#!/usr/bin/env bash
# check-context-budget.sh — enforce .claude/rules/context-budget-mandate.md
#
# Two gates against base-context auto-load creep:
#   1. TOTAL ceiling — sum of always-load rule bytes (rules WITHOUT `paths:`
#      frontmatter). WARN/FAIL bands prevent runaway growth over many sessions.
#   2. PER-RULE gate — any always-load rule ≥ MIN_BYTES that is NOT Priority
#      CRITICAL and has NO `## Auto-load justification` section → must be
#      path-scoped/justified/hook-covered per §3.2. (Default: WARN; CI_FAIL_PER_RULE=1 → FAIL)
#
# Byte-based (deterministic; ~4 bytes ≈ 1 token rough proxy). Exit 1 on FAIL.
set -u
RULES_DIR=".claude/rules"
MIN_BYTES=${MIN_BYTES:-4000}              # ~1k tokens — §3.2 size gate
WARN_TOTAL=${WARN_TOTAL:-250000}          # ~62k tokens
FAIL_TOTAL=${FAIL_TOTAL:-300000}          # ~75k tokens hard ceiling
CI_FAIL_PER_RULE=${CI_FAIL_PER_RULE:-1}   # per-rule violation blocks (current state compliant)

total=0; n=0; per_rule_viol=0
declare -a OFFENDERS
for f in "$RULES_DIR"/*.md; do
  base=$(basename "$f")
  [ "$base" = "README.md" ] && continue
  # always-load = no `paths:` in frontmatter (first 3 lines)
  if head -3 "$f" | grep -q '^paths:'; then continue; fi
  bytes=$(wc -c < "$f")
  total=$((total + bytes)); n=$((n + 1))
  # per-rule §3.2 check
  if [ "$bytes" -ge "$MIN_BYTES" ]; then
    prio=$(grep -m1 '\*\*Priority:\*\*' "$f" | grep -oE 'CRITICAL|MANDATORY|ADVISORY')
    if [ "$prio" != "CRITICAL" ] && ! grep -qiE 'Auto-load justification' "$f"; then
      OFFENDERS+=("$base ($bytes bytes, $prio, no justification)")
      per_rule_viol=$((per_rule_viol + 1))
    fi
  fi
done

echo "Context Budget Check (per context-budget-mandate.md §3.2)"
echo "──────────────────────────────────────────────────────────"
echo "  Always-load rules:  $n"
echo "  Total bytes:        $total  (≈ $((total/4)) tokens rough proxy)"
echo "  Ceiling:            WARN ≥ $WARN_TOTAL / FAIL ≥ $FAIL_TOTAL bytes"
echo "  Per-rule §3.2 viol: $per_rule_viol"
echo ""

rc=0
if [ "$per_rule_viol" -gt 0 ]; then
  echo "Per-rule §3.2 violations (always-load ≥${MIN_BYTES}B, not CRITICAL, no '## Auto-load justification'):"
  for o in "${OFFENDERS[@]}"; do echo "  ✗ $o"; done
  echo "  → Fix: add 'paths:' frontmatter (path-scope) OR '## Auto-load justification' OR hook-cover."
  [ "$CI_FAIL_PER_RULE" = "1" ] && rc=1 || echo "  (WARN mode — not blocking)"
  echo ""
fi

if [ "$total" -ge "$FAIL_TOTAL" ]; then
  echo "🔴 FAIL: always-load total $total ≥ $FAIL_TOTAL bytes hard ceiling."
  echo "   Path-scope a rule (add 'paths:' frontmatter) before adding more always-load content."
  rc=1
elif [ "$total" -ge "$WARN_TOTAL" ]; then
  echo "🟡 WARN: always-load total $total ≥ $WARN_TOTAL bytes — approaching ceiling $FAIL_TOTAL."
  echo "   Consider path-scoping the next rule rather than adding always-load."
else
  echo "🟢 PASS: always-load total $total < $WARN_TOTAL warn threshold."
fi
exit $rc
