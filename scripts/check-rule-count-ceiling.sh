#!/usr/bin/env bash
# check-rule-count-ceiling — count `.claude/rules/*.md` files and emit
# WARN/HARD-STOP at policy thresholds.
#
# Closes Wave 76 Bucket D scope. Spec lives in `.claude/rules/README.md`
# "Rule count ceiling policy" section.
#
# Thresholds:
#   0-50      Free growth (PASS — below industry maintainability ceiling)
#   50-75     INFO (quarterly review trigger at 75)
#   75-100    WARN (consolidation review required)
#   >100      HARD STOP (exit 1 — must consolidate or deprecate before adding)
#
# Exit codes:
#   0 = count within HARD STOP (≤100), regardless of WARN/INFO
#   1 = count > HARD STOP threshold
#
# Used by:
#   - `.github/workflows/script-quality.yml` job `rule-count-ceiling`
#   - Manual run: `bash scripts/check-rule-count-ceiling.sh`

set -euo pipefail

RULES_DIR=".claude/rules"

if [ ! -d "$RULES_DIR" ]; then
    echo "ERROR: $RULES_DIR not found (run from repo root)" >&2
    exit 2
fi

# Count *.md excluding README.md (folder index, not a rule)
# Use -maxdepth 1 so nested fixtures / archived rules don't inflate count.
COUNT=$(find "$RULES_DIR" -maxdepth 1 -name '*.md' -not -name 'README.md' -type f | wc -l)

FREE_GROWTH_MAX=50
QUARTERLY_REVIEW_MAX=75
HARD_STOP=100

echo "Current rule count: $COUNT (excluding README.md)"
echo "Thresholds: free-growth ≤$FREE_GROWTH_MAX, quarterly-review at >$FREE_GROWTH_MAX, consolidation-review at >$QUARTERLY_REVIEW_MAX, HARD STOP at >$HARD_STOP"

if [ "$COUNT" -gt "$HARD_STOP" ]; then
    echo "FAIL: rule count $COUNT > $HARD_STOP — HARD STOP. Must consolidate or deprecate before adding new rules."
    echo "See: .claude/rules/README.md 'Rule count ceiling policy'"
    exit 1
elif [ "$COUNT" -gt "$QUARTERLY_REVIEW_MAX" ]; then
    echo "WARN: rule count $COUNT — consolidation review required (>$QUARTERLY_REVIEW_MAX threshold)"
    echo "Action: audit overlap + identify deprecation candidates before next rule add"
    exit 0
elif [ "$COUNT" -gt "$FREE_GROWTH_MAX" ]; then
    echo "INFO: rule count $COUNT — quarterly review trigger at $QUARTERLY_REVIEW_MAX"
    exit 0
fi

echo "PASS: rule count $COUNT within free-growth (≤$FREE_GROWTH_MAX)"
exit 0
