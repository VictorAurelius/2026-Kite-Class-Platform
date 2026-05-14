#!/usr/bin/env bash
# check-rule-staleness — verify `.claude/rules/*.md` have `Last-Reviewed`
# within freshness thresholds.
#
# Closes Wave 76 Bucket D scope. Spec lives in
# `.claude/rules/rule-change-process.md` §3.5 (Last-Reviewed staleness policy).
#
# Thresholds (override via env vars):
#   RULE_STALE_WARN_DAYS  (default 60)  — WARN at this age
#   RULE_STALE_FAIL_DAYS  (default 180) — FAIL at this age
#
# CI mode: WARN-only initially (script exits 0 unless Last-Reviewed missing
# or invalid). HARD STOP at FAIL threshold activates after 30-day grace
# period from Wave 76 merge — track via documented re-enable date.
#
# Exit codes:
#   0 = no missing/invalid Last-Reviewed (regardless of staleness counts)
#   1 = ≥1 rule missing Last-Reviewed field OR has invalid date
#
# Used by:
#   - `.github/workflows/script-quality.yml` job `rule-staleness` (WARN mode)
#   - Manual run: `bash scripts/check-rule-staleness.sh`
#
# Self-test:
#   No fixture mode (v1 keeps it simple); script logic is small + transparent.
#   Run against real repo, verify output matches `.claude/rules/*.md` state.
#
# See also:
#   - `.claude/rules/rule-change-process.md` §3.5 staleness policy
#   - `scripts/check-rule-frontmatter.sh` (sister script — format validation)
#   - `scripts/check-rule-count-ceiling.sh` (sister script — count ceiling)

set -euo pipefail

WARN_DAYS=${RULE_STALE_WARN_DAYS:-60}
FAIL_DAYS=${RULE_STALE_FAIL_DAYS:-180}

# CI WARN-mode flag — when set to "1" (default), script returns 0 even if
# rules cross FAIL threshold (only missing/invalid Last-Reviewed cause exit 1).
# Set to "0" after grace period to enable HARD STOP on age-based FAIL.
CI_WARN_MODE=${RULE_STALE_WARN_MODE:-1}

TODAY_EPOCH=$(date -u +%s)

PASS=0
WARN=0
FAIL_STALE=0
FAIL_MISSING=0

RULES_DIR=".claude/rules"

if [ ! -d "$RULES_DIR" ]; then
    echo "ERROR: $RULES_DIR not found (run from repo root)" >&2
    exit 2
fi

for f in "$RULES_DIR"/*.md; do
    [ -f "$f" ] || continue
    base=$(basename "$f")
    [ "$base" = "README.md" ] && continue

    # Extract Last-Reviewed (markdown-header style: **Last-Reviewed:** YYYY-MM-DD)
    last_reviewed=$(grep -m1 '^\*\*Last-Reviewed:\*\*' "$f" 2>/dev/null \
        | sed 's/^\*\*Last-Reviewed:\*\*[[:space:]]*//' \
        | awk '{print $1}' || true)

    if [ -z "$last_reviewed" ]; then
        echo "FAIL: $base — no Last-Reviewed field"
        FAIL_MISSING=$((FAIL_MISSING + 1))
        continue
    fi

    # Validate date format YYYY-MM-DD + parse to epoch
    if ! review_epoch=$(date -u -d "$last_reviewed" +%s 2>/dev/null); then
        echo "FAIL: $base — invalid date '$last_reviewed' (expected YYYY-MM-DD)"
        FAIL_MISSING=$((FAIL_MISSING + 1))
        continue
    fi

    age_days=$(( (TODAY_EPOCH - review_epoch) / 86400 ))

    if [ "$age_days" -ge "$FAIL_DAYS" ]; then
        echo "FAIL-STALE: $base — Last-Reviewed $last_reviewed ($age_days days old, threshold $FAIL_DAYS)"
        FAIL_STALE=$((FAIL_STALE + 1))
    elif [ "$age_days" -ge "$WARN_DAYS" ]; then
        echo "WARN: $base — Last-Reviewed $last_reviewed ($age_days days old)"
        WARN=$((WARN + 1))
    else
        PASS=$((PASS + 1))
    fi
done

echo ""
echo "Results: $PASS fresh / $WARN stale-WARN / $FAIL_STALE stale-FAIL / $FAIL_MISSING missing-or-invalid"
echo "Thresholds: WARN=$WARN_DAYS days, FAIL=$FAIL_DAYS days, CI_WARN_MODE=$CI_WARN_MODE"

# Exit policy:
#   - Missing/invalid Last-Reviewed = ALWAYS blocking (exit 1) — basic format
#     contract enforced by `check-rule-frontmatter.sh` too; this is a fallback.
#   - Age-based FAIL = blocking only when CI_WARN_MODE=0 (post-grace-period).
if [ "$FAIL_MISSING" -gt 0 ]; then
    echo "EXIT 1: $FAIL_MISSING rule(s) missing or invalid Last-Reviewed"
    exit 1
fi

if [ "$CI_WARN_MODE" != "1" ] && [ "$FAIL_STALE" -gt 0 ]; then
    echo "EXIT 1: $FAIL_STALE rule(s) crossed FAIL threshold ($FAIL_DAYS days); CI_WARN_MODE=0"
    exit 1
fi

if [ "$FAIL_STALE" -gt 0 ]; then
    echo "EXIT 0 (WARN mode): $FAIL_STALE rule(s) crossed FAIL threshold but CI_WARN_MODE=1 — informational only"
fi

exit 0
