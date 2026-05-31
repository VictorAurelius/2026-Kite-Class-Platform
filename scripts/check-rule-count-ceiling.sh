#!/usr/bin/env bash
# check-rule-count-ceiling — count `.claude/rules/*.md` files against
# CONTEXT-AWARE policy thresholds (2 bands).
#
# Rationale (2026-05-31 rewrite): the original single-band ceiling (set Wave 76
# when most rules were always-load) conflated two very different costs:
#   - ALWAYS-LOAD rules (no `paths:` frontmatter) cost base-context every session
#     → must stay few. This is the expensive tier. Byte-size also gated by
#     `scripts/check-context-budget.sh` (the primary guard).
#   - PATH-SCOPED rules (`paths:` frontmatter) cost ~0 base context — load only
#     when a matching file is in context. Count here is a pure MAINTAINABILITY
#     metric, so its ceiling is far looser.
# A flat "≤50 / 76-100 WARN" on the combined total mislabels a healthy repo
# (e.g. 13 always-load + 77 path-scoped) as WARN. This script bands them.
#
# Exit codes:
#   0 = within hard ceilings (WARN/INFO non-blocking)
#   1 = a HARD ceiling exceeded (runaway always-load OR runaway path-scoped)
#
# Used by: `.github/workflows/quality-rules-skills.yml` job (rule governance).
# Spec: `.claude/rules/README.md` "Rule count ceiling policy".

set -euo pipefail

RULES_DIR=".claude/rules"
[ -d "$RULES_DIR" ] || { echo "ERROR: $RULES_DIR not found (run from repo root)" >&2; exit 2; }

# --- count, split by always-load vs path-scoped ---
always=0; scoped=0
while IFS= read -r f; do
    if head -3 "$f" | grep -q '^paths:'; then
        scoped=$((scoped + 1))
    else
        always=$((always + 1))
    fi
done < <(find "$RULES_DIR" -maxdepth 1 -name '*.md' -not -name 'README.md' -type f)
total=$((always + scoped))

# --- thresholds ---
# Always-load (context-expensive): keep few. Byte gate is primary; count is backstop.
ALWAYS_WARN=18
ALWAYS_HARD=25
# Path-scoped (maintainability only): loose.
SCOPED_INFO=100
SCOPED_WARN=150
SCOPED_HARD=200

echo "Rule count (context-aware) — total $total = $always always-load + $scoped path-scoped"
echo "  Always-load band : WARN ≥$ALWAYS_WARN / HARD ≥$ALWAYS_HARD  (also byte-gated by check-context-budget.sh)"
echo "  Path-scoped band : INFO ≥$SCOPED_INFO / WARN ≥$SCOPED_WARN / HARD ≥$SCOPED_HARD"
echo ""

rc=0

# Always-load band
if [ "$always" -ge "$ALWAYS_HARD" ]; then
    echo "🔴 FAIL: $always always-load rules ≥ $ALWAYS_HARD — runaway base-context. Path-scope or deprecate before adding."
    rc=1
elif [ "$always" -ge "$ALWAYS_WARN" ]; then
    echo "🟡 WARN: $always always-load rules ≥ $ALWAYS_WARN — review whether each truly needs always-load (else add 'paths:')."
else
    echo "🟢 always-load $always — OK (< $ALWAYS_WARN)."
fi

# Path-scoped band
if [ "$scoped" -ge "$SCOPED_HARD" ]; then
    echo "🔴 FAIL: $scoped path-scoped rules ≥ $SCOPED_HARD — consolidate/deprecate before adding."
    rc=1
elif [ "$scoped" -ge "$SCOPED_WARN" ]; then
    echo "🟡 WARN: $scoped path-scoped rules ≥ $SCOPED_WARN — consolidation review recommended."
elif [ "$scoped" -ge "$SCOPED_INFO" ]; then
    echo "ℹ️  INFO: $scoped path-scoped rules ≥ $SCOPED_INFO — quarterly overlap audit."
else
    echo "🟢 path-scoped $scoped — OK (< $SCOPED_INFO)."
fi

exit $rc
