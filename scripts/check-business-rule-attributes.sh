#!/usr/bin/env bash
#
# check-business-rule-attributes.sh — Business Logic Correctness 5-attribute detector
#
# Enforces `.claude/rules/business-logic-review.md` §2: every per-domain
# `documents/01-business/**/rules.md` business rule must document 5 attributes:
#   Source · Rationale · Reviewer · Compliance check · Review cadence (+ Next review)
#
# Closes GAP-156 AC-E: real warn→block detector (the §6.2 "audit-gate.py partial
# detector" was described but never actually implemented — this is the real one).
#
# Modes (per file change type — makes block-mode SAFE without breaking grandfathered files):
#   - ADDED rules.md (brand-new business domain) → BLOCK if missing any attribute
#     (born-compliant mandate). Downgraded to WARN by BUSINESS_RULE_OVERRIDE: trailer.
#   - MODIFIED existing rules.md → WARN only (grandfathered until bucket-B backfill;
#     flip to BLOCK via BUSINESS_RULE_BLOCK_MODIFIED=1 once backfill complete).
#
# Usage:
#   bash scripts/check-business-rule-attributes.sh                 # CI: diff origin/main...HEAD
#   bash scripts/check-business-rule-attributes.sh --diff <base>   # diff <base>...HEAD
#   bash scripts/check-business-rule-attributes.sh --files a.md b.md   # explicit files (treated as ADDED)
#   bash scripts/check-business-rule-attributes.sh --self-test      # run fixture self-test
#
# Env:
#   BUSINESS_RULE_BLOCK_MODIFIED=1   # also BLOCK on modified files (post-backfill flip)
#   BUSINESS_RULE_OVERRIDE_TRAILER   # injected override reason (testing); CI reads git log
#
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT" || exit 2

ATTRS=(Source Rationale Reviewer Compliance "Review cadence")
RULE_REF=".claude/rules/business-logic-review.md §2"
OVERRIDE_KEY="BUSINESS_RULE_OVERRIDE:"

# ── attribute presence check on a single file ────────────────────
# Returns list of MISSING attribute names (space-joined). Matches the
# bolded-label format from business-logic-review.md §4.2 GOOD example:
#   - **Source:** ...   |   - **Review cadence:** ... / **Next review:** ...
missing_attrs() {
    local file="$1" missing=() a
    [ -f "$file" ] || { echo "FILE_NOT_FOUND"; return; }
    for a in "${ATTRS[@]}"; do
        if [ "$a" = "Review cadence" ]; then
            # cadence satisfied by either "Review cadence" or "Next review"
            grep -qiE '\*\*(Review cadence|Next review)' "$file" || missing+=("Review-cadence")
        elif [ "$a" = "Compliance" ]; then
            grep -qiE '\*\*Compliance' "$file" || missing+=("Compliance")
        else
            grep -qiE "\\*\\*${a}" "$file" || missing+=("$a")
        fi
    done
    echo "${missing[*]:-}"
}

# ── override trailer present in PR commits? ──────────────────────
has_override() {
    [ -n "${BUSINESS_RULE_OVERRIDE_TRAILER:-}" ] && return 0
    local base="${1:-origin/main}"
    git log "${base}..HEAD" --format='%B' 2>/dev/null | grep -q "$OVERRIDE_KEY" && return 0
    return 1
}

# ── resolve changed rules.md files + their status (A=added, M=modified) ──
collect_diff() {
    local base="$1"
    git diff --name-status "${base}...HEAD" 2>/dev/null \
        | awk '$2 ~ /documents\/01-business\/.*rules\.md$/ {print $1"\t"$2}'
}

run_check() {
    local mode="$1"; shift
    local base="${1:-origin/main}"
    local fail=0 warn=0 checked=0
    local override=0
    has_override "$base" && override=1

    declare -a entries=()
    if [ "$mode" = "files" ]; then
        shift  # drop base placeholder
        for f in "$@"; do entries+=("A	$f"); done
    else
        while IFS= read -r line; do [ -n "$line" ] && entries+=("$line"); done < <(collect_diff "$base")
    fi

    if [ ${#entries[@]} -eq 0 ]; then
        echo "✅ check-business-rule-attributes: no documents/01-business/**/rules.md changes — nothing to check"
        return 0
    fi

    echo "── Business rule 5-attribute check (per ${RULE_REF}) ──"
    for entry in "${entries[@]}"; do
        local status file miss
        status="${entry%%	*}"
        file="${entry#*	}"
        [ "$status" = "D" ] && continue   # deletions skip
        checked=$((checked+1))
        miss="$(missing_attrs "$file")"
        if [ -z "$miss" ]; then
            echo "  ✅ $file — all 5 attributes present"
            continue
        fi
        local sev="WARN"
        # ADDED file (new domain) → BLOCK unless override. MODIFIED → WARN (unless block-modified flip).
        if [ "$status" = "A" ] && [ "$override" -eq 0 ]; then
            sev="BLOCK"
        elif [ "${BUSINESS_RULE_BLOCK_MODIFIED:-0}" = "1" ] && [ "$override" -eq 0 ]; then
            sev="BLOCK"
        fi
        echo "  ❌ [$sev] $file (${status}) — missing: $miss"
        if [ "$sev" = "BLOCK" ]; then fail=$((fail+1)); else warn=$((warn+1)); fi
    done

    echo "── checked=$checked  block=$fail  warn=$warn  override=$override ──"
    if [ "$fail" -gt 0 ]; then
        echo "🔴 BLOCK: $fail new rules.md missing required attributes."
        echo "   Fix: add the 5 attributes per ${RULE_REF}, OR add commit trailer '${OVERRIDE_KEY} <reason + GAP link>'."
        return 1
    fi
    [ "$warn" -gt 0 ] && echo "🟡 WARN: $warn modified rules.md missing attributes (grandfathered — backfill per GAP-156 bucket B)."
    echo "🟢 PASS (no blocking violations)"
    return 0
}

# ── self-test on fixtures ────────────────────────────────────────
self_test() {
    local fx="scripts/fixtures/business-rule-attributes"
    local pass=0 total=0 rc

    echo "=== SELF-TEST: check-business-rule-attributes.sh ==="

    # Fixture 1: compliant (5/5) treated as ADDED → expect PASS (exit 0)
    total=$((total+1))
    run_check files _ "$fx/fixture-compliant.md" >/tmp/brt1.log 2>&1; rc=$?
    if [ $rc -eq 0 ]; then echo "✅ T1 compliant-added → PASS"; pass=$((pass+1)); else echo "❌ T1 expected PASS got rc=$rc"; cat /tmp/brt1.log; fi

    # Fixture 2: missing attrs treated as ADDED, no override → expect BLOCK (exit 1)
    total=$((total+1))
    run_check files _ "$fx/fixture-missing.md" >/tmp/brt2.log 2>&1; rc=$?
    if [ $rc -eq 1 ]; then echo "✅ T2 missing-added → BLOCK"; pass=$((pass+1)); else echo "❌ T2 expected BLOCK got rc=$rc"; cat /tmp/brt2.log; fi

    # Fixture 3: missing attrs ADDED + override trailer → expect downgrade to PASS (exit 0)
    total=$((total+1))
    BUSINESS_RULE_OVERRIDE_TRAILER="test override per fixture" \
        run_check files _ "$fx/fixture-missing.md" >/tmp/brt3.log 2>&1; rc=$?
    if [ $rc -eq 0 ]; then echo "✅ T3 missing-added+override → PASS (warn)"; pass=$((pass+1)); else echo "❌ T3 expected PASS got rc=$rc"; cat /tmp/brt3.log; fi

    echo "=== SELF-TEST: $pass/$total passed ==="
    [ "$pass" -eq "$total" ]
}

# ── main ─────────────────────────────────────────────────────────
case "${1:-}" in
    --self-test) self_test ;;
    --files)     shift; run_check files _ "$@" ;;
    --diff)      run_check diff "${2:-origin/main}" ;;
    "" )         run_check diff "origin/main" ;;
    * )          echo "Unknown arg: $1"; echo "Usage: $0 [--diff <base> | --files <f...> | --self-test]"; exit 2 ;;
esac
