#!/bin/bash
set -euo pipefail

# repo-status.sh - Quick remote repo health check
#
# Usage:
#   ./scripts/repo-status.sh              # Full status report
#   ./scripts/repo-status.sh --json       # JSON output (for skill consumption)
#   ./scripts/repo-status.sh --level      # Just print level: GREEN/YELLOW/ORANGE/RED/BLACK
#
# Checks 4 factors:
#   1. CI status on main (green/failing, how long)
#   2. Open PRs + stale remote branches
#   3. Latest audit gaps without fix PRs
#   4. GitHub Security — Dependabot + code-scanning + secret-scanning alerts
#
# Exit codes:
#   0 = GREEN or YELLOW
#   1 = ORANGE
#   2 = RED
#   3 = BLACK

# --- Parse arguments ---
OUTPUT_MODE="full"
for arg in "$@"; do
    case "$arg" in
        --json)  OUTPUT_MODE="json" ;;
        --level) OUTPUT_MODE="level" ;;
        --help|-h)
            echo "Usage: ./scripts/repo-status.sh [--json|--level|--help]"
            echo ""
            echo "  --json   JSON output for script consumption"
            echo "  --level  Print only the status level"
            echo "  --help   Show this help"
            exit 0
            ;;
    esac
done

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
ORANGE='\033[0;33m'  # terminal doesn't have orange, use yellow
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# --- Factor 1: CI Status ---
check_ci() {
    local ci_status="green"
    local ci_failures=0
    local ci_last_green=""
    local ci_days_red=0
    local ci_details=""

    # Get last 10 runs on main
    local runs
    runs=$(gh run list --branch main --limit 10 --json name,status,conclusion,createdAt \
        --jq '.[] | "\(.name)\t\(.conclusion // "in_progress")\t\(.createdAt)"' 2>/dev/null) || {
        echo "ci_status=unknown"
        echo "ci_error=cannot_reach_github"
        return
    }

    if [ -z "$runs" ]; then
        echo "ci_status=unknown"
        echo "ci_error=no_runs_found"
        return
    fi

    # Count failures in latest runs (deduplicate by workflow name - take latest only)
    local latest_failures=0
    local latest_total=0
    local seen_workflows=""

    while IFS=$'\t' read -r name conclusion created; do
        # GAP-205 Stage D: skip "Dependabot Updates" failures from CI health count.
        # These are pnpm transitive limitation failures (known upstream issue,
        # not actionable). See memory feedback_dependabot_pnpm_transitive.md.
        if [ "$name" = "Dependabot Updates" ] && [ "$conclusion" = "failure" ]; then
            continue
        fi

        # Skip if we've already seen this workflow
        if echo "$seen_workflows" | grep -qF "$name"; then
            continue
        fi
        seen_workflows="$seen_workflows|$name"
        latest_total=$((latest_total + 1))

        if [ "$conclusion" = "failure" ]; then
            latest_failures=$((latest_failures + 1))
            ci_details="${ci_details}FAIL: ${name}\n"
        elif [ "$conclusion" = "in_progress" ]; then
            ci_details="${ci_details}RUNNING: ${name}\n"
        else
            ci_details="${ci_details}PASS: ${name}\n"
        fi
    done <<< "$runs"

    # Find how long CI has been failing
    if [ "$latest_failures" -gt 0 ]; then
        ci_status="failing"
        ci_failures=$latest_failures

        # Find last successful run date
        local last_success_date
        last_success_date=$(gh run list --branch main --limit 30 --json conclusion,createdAt \
            --jq '[.[] | select(.conclusion=="success")] | .[0].createdAt // empty' 2>/dev/null) || true

        if [ -n "$last_success_date" ]; then
            local now_epoch last_epoch
            now_epoch=$(date +%s)
            # Cross-platform date parsing
            if date -d "$last_success_date" +%s &>/dev/null; then
                last_epoch=$(date -d "$last_success_date" +%s)
            else
                # macOS fallback
                last_epoch=$(date -jf "%Y-%m-%dT%H:%M:%SZ" "$last_success_date" +%s 2>/dev/null || echo "$now_epoch")
            fi
            ci_days_red=$(( (now_epoch - last_epoch) / 86400 ))
            ci_last_green="$last_success_date"
        else
            ci_days_red=999
            ci_last_green="never"
        fi
    fi

    # Count stale failed runs in history (all failed runs on main)
    # GAP-205 Stage D: exclude "Dependabot Updates" failures (pnpm transitive
    # limitation, not actionable). See ci-cleanup.yml auto-prunes these.
    local failed_run_count=0
    failed_run_count=$(gh run list --branch main --limit 30 --json conclusion,name \
        --jq '[.[] | select(.conclusion=="failure" and .name!="Dependabot Updates")] | length' 2>/dev/null) || true

    echo "ci_status=$ci_status"
    echo "ci_failures=$ci_failures"
    echo "ci_days_red=$ci_days_red"
    echo "ci_last_green=$ci_last_green"
    echo "ci_failed_history=$failed_run_count"
    echo "ci_details=$(echo -e "$ci_details" | head -10)"
}

# --- Factor 2: PRs + Branches ---
check_pr_branches() {
    local open_prs=0
    local stale_branches=0
    local pr_details=""
    local branch_details=""

    # Open PRs
    local pr_list
    pr_list=$(gh pr list --state open --json number,title,headRefName,createdAt \
        --jq '.[] | "#\(.number) \(.headRefName) — \(.title)"' 2>/dev/null) || true

    if [ -n "$pr_list" ]; then
        open_prs=$(echo "$pr_list" | wc -l | tr -d ' ')
        pr_details="$pr_list"
    fi

    # Stale remote branches (not merged into main)
    git fetch --prune &>/dev/null || true
    local branches
    branches=$(git branch -r --no-merged origin/main 2>/dev/null | grep -v "HEAD" | sed 's/^ *//' ) || true

    if [ -n "$branches" ]; then
        stale_branches=$(echo "$branches" | wc -l | tr -d ' ')
        branch_details="$branches"
    fi

    echo "open_prs=$open_prs"
    echo "stale_branches=$stale_branches"
    echo "pr_details=$pr_details"
    echo "branch_details=$branch_details"
}

# --- Factor 3: Audit Gaps ---
check_audit_gaps() {
    local has_unfixed_gaps="false"
    local gap_p0=0
    local gap_p1=0
    local gap_p2=0
    local latest_audit=""
    local latest_audit_date=""
    local gap_details=""

    # Find latest quality audit report (sort by filename = date, not mtime)
    local audit_file
    audit_file=$(ls documents/04-quality/audits/quality/quality-audit-*.md 2>/dev/null | sort -r | head -1) || true

    if [ -z "$audit_file" ]; then
        echo "has_unfixed_gaps=unknown"
        echo "latest_audit=none"
        return
    fi

    latest_audit="$audit_file"
    latest_audit_date=$(echo "$audit_file" | grep -oP '\d{4}-\d{2}-\d{2}' | tail -1) || true

    # Count P0/P1/P2 items in Remaining Gaps / Action Items / Improvement Roadmap
    # Look for priority markers after "Remaining Gaps" or "Action Items" section
    local gaps_section
    gaps_section=$(sed -n '/## Remaining Gaps\|## Action Items\|## Improvement Roadmap/,/^## [^#]/p' "$audit_file" 2>/dev/null) || true

    if [ -n "$gaps_section" ]; then
        # Exclude resolved items (strikethrough ~~ or ✅ Fixed/Done)
        local active_gaps
        active_gaps=$(echo "$gaps_section" | grep -v '~~' | grep -v '✅.*Fixed\|✅.*Done\|✅.*Resolved')

        gap_p0=$(echo "$active_gaps" | grep -cE '🔴|P0' || true)
        gap_p1=$(echo "$active_gaps" | grep -cE '🟠|P1' || true)
        gap_p2=$(echo "$active_gaps" | grep -cE '🟡|P2' || true)

        if [ "$gap_p0" -gt 0 ] || [ "$gap_p1" -gt 0 ]; then
            has_unfixed_gaps="true"
        fi

        # Extract gap summaries (active only, first 10 lines)
        gap_details=$(echo "$active_gaps" | grep -E '🔴|🟠|🟡|P0|P1|P2' | head -10)
    fi

    # Also check UI audit issues (count open items by severity)
    local ui_audit
    ui_audit=$(ls documents/04-quality/audits/ui/ui-audit-issues-*.md 2>/dev/null | sort -r | head -1) || true
    local ui_audit_date=""
    local ui_open_total=0
    if [ -n "$ui_audit" ]; then
        ui_audit_date=$(echo "$ui_audit" | grep -oP '\d{4}-\d{2}-\d{2}' | tail -1) || true
        # Count total open items (marked with ⬜)
        ui_open_total=$(grep -c '⬜' "$ui_audit" 2>/dev/null || true)
        # Add UI open items to P2 count (open UI issues are medium priority)
        gap_p2=$((gap_p2 + ui_open_total))
    fi

    echo "has_unfixed_gaps=$has_unfixed_gaps"
    echo "gap_p0=$gap_p0"
    echo "gap_p1=$gap_p1"
    echo "gap_p2=$gap_p2"
    echo "latest_audit=$latest_audit"
    echo "latest_audit_date=$latest_audit_date"
    echo "ui_audit=$ui_audit"
    echo "ui_audit_date=$ui_audit_date"
    echo "ui_open_total=$ui_open_total"
    echo "gap_details=$gap_details"
}

# --- Factor 4: GitHub Security ---
# Probes Dependabot, code-scanning, secret-scanning via gh api.
# Graceful-degrades when any API is disabled or unavailable.
check_security() {
    local repo
    repo=$(gh repo view --json nameWithOwner --jq .nameWithOwner 2>/dev/null) || {
        echo "sec_status=unknown"
        echo "sec_error=gh_unavailable"
        return
    }

    # Helper: run gh api, capture stdout + exit code separately.
    # Sets globals: _GH_BODY (stdout), _GH_EXIT (exit code).
    _gh_api() {
        _GH_BODY=$(gh api "$1" 2>/dev/null) || _GH_EXIT=$?
        _GH_EXIT=${_GH_EXIT:-0}
    }

    # Helper: determine if API is disabled/unavailable.
    # Returns 0 (disabled) if exit != 0 OR response is not a JSON array.
    _is_disabled() {
        local exit_code="$1" body="$2"
        [ "$exit_code" -ne 0 ] && return 0
        echo "$body" | jq -e 'type=="array"' >/dev/null 2>&1 || return 0
        return 1
    }

    # --- Dependabot ---
    local dependabot_status="enabled"
    local sec_critical=0 sec_high=0 sec_medium=0 sec_low=0
    _GH_EXIT=0
    _gh_api "repos/$repo/dependabot/alerts?state=open&per_page=100"
    if _is_disabled "$_GH_EXIT" "$_GH_BODY"; then
        dependabot_status="disabled"
    else
        sec_critical=$(echo "$_GH_BODY" | jq '[.[] | select(.security_advisory.severity=="critical")] | length' 2>/dev/null || echo 0)
        sec_high=$(echo "$_GH_BODY" | jq '[.[] | select(.security_advisory.severity=="high")] | length' 2>/dev/null || echo 0)
        sec_medium=$(echo "$_GH_BODY" | jq '[.[] | select(.security_advisory.severity=="medium")] | length' 2>/dev/null || echo 0)
        sec_low=$(echo "$_GH_BODY" | jq '[.[] | select(.security_advisory.severity=="low")] | length' 2>/dev/null || echo 0)
    fi

    # --- Code scanning ---
    local code_scan_status="enabled"
    local code_scan_errors=0 code_scan_warnings=0 code_scan_notes=0
    _GH_EXIT=0
    _gh_api "repos/$repo/code-scanning/alerts?state=open&per_page=100"
    if _is_disabled "$_GH_EXIT" "$_GH_BODY"; then
        code_scan_status="disabled"
    else
        code_scan_errors=$(echo "$_GH_BODY" | jq '[.[] | select(.rule.severity=="error")] | length' 2>/dev/null || echo 0)
        code_scan_warnings=$(echo "$_GH_BODY" | jq '[.[] | select(.rule.severity=="warning")] | length' 2>/dev/null || echo 0)
        code_scan_notes=$(echo "$_GH_BODY" | jq '[.[] | select(.rule.severity=="note")] | length' 2>/dev/null || echo 0)
    fi

    # --- Secret scanning ---
    local secret_scan_status="enabled"
    local secret_scan_count=0
    _GH_EXIT=0
    _gh_api "repos/$repo/secret-scanning/alerts?state=open"
    if _is_disabled "$_GH_EXIT" "$_GH_BODY"; then
        secret_scan_status="disabled"
    else
        secret_scan_count=$(echo "$_GH_BODY" | jq 'length' 2>/dev/null || echo 0)
    fi

    echo "sec_status=ok"
    echo "dependabot_status=$dependabot_status"
    echo "sec_critical=$sec_critical"
    echo "sec_high=$sec_high"
    echo "sec_medium=$sec_medium"
    echo "sec_low=$sec_low"
    echo "code_scan_status=$code_scan_status"
    echo "code_scan_errors=$code_scan_errors"
    echo "code_scan_warnings=$code_scan_warnings"
    echo "code_scan_notes=$code_scan_notes"
    echo "secret_scan_status=$secret_scan_status"
    echo "secret_scan_count=$secret_scan_count"
}

# --- Determine Level ---
determine_level() {
    local ci_status="$1"
    local ci_days_red="$2"
    local open_prs="$3"
    local stale_branches="$4"
    local has_unfixed_gaps="$5"
    local gap_p0="$6"
    local ci_failed_history="${7:-0}"
    # Security inputs
    local dependabot_status="${8:-enabled}"
    local sec_critical="${9:-0}"
    local sec_high="${10:-0}"
    local code_scan_errors="${11:-0}"
    local code_scan_warnings="${12:-0}"
    local secret_scan_count="${13:-0}"

    # Aggregate HIGH-severity counts across Dependabot + code-scanning
    local hi_total=$((sec_high + code_scan_errors))
    local crit_total=$((sec_critical))

    # BLACK: CI red >7 days OR any CRITICAL CVE OR secret exposed
    if [ "$ci_days_red" -gt 7 ] || [ "$crit_total" -gt 0 ] || [ "$secret_scan_count" -gt 0 ]; then
        echo "BLACK"
        return
    fi

    # RED: CI failing on main OR P0 gaps unfixed OR any HIGH CVE / code-scanning error
    if [ "$ci_status" = "failing" ] || [ "$gap_p0" -gt 0 ] || [ "$hi_total" -gt 0 ]; then
        echo "RED"
        return
    fi

    # ORANGE: P1 gaps unfixed OR >2 stale branches/open PRs OR Dependabot disabled OR many warnings
    local total_stale=$((open_prs + stale_branches))
    if [ "$has_unfixed_gaps" = "true" ] || [ "$total_stale" -gt 2 ] \
       || [ "$dependabot_status" = "disabled" ] || [ "$code_scan_warnings" -ge 3 ]; then
        echo "ORANGE"
        return
    fi

    # YELLOW: minor gaps (P2/P3) OR 1-2 stale branches OR dirty CI history OR any code-scanning warnings
    if [ "$total_stale" -gt 0 ] || [ "$ci_failed_history" -gt 2 ] || [ "$code_scan_warnings" -gt 0 ]; then
        echo "YELLOW"
        return
    fi

    # GREEN: all clean
    echo "GREEN"
}

# --- Collect all data ---
CI_DATA=$(check_ci)
PR_DATA=$(check_pr_branches)
AUDIT_DATA=$(check_audit_gaps)
SEC_DATA=$(check_security)

# Parse values
ci_status=$(echo "$CI_DATA" | grep "^ci_status=" | cut -d= -f2)
ci_failures=$(echo "$CI_DATA" | grep "^ci_failures=" | cut -d= -f2)
ci_days_red=$(echo "$CI_DATA" | grep "^ci_days_red=" | cut -d= -f2)
ci_last_green=$(echo "$CI_DATA" | grep "^ci_last_green=" | cut -d= -f2-)
ci_failed_history=$(echo "$CI_DATA" | grep "^ci_failed_history=" | cut -d= -f2)
ci_details=$(echo "$CI_DATA" | grep "^ci_details=" | cut -d= -f2-)

open_prs=$(echo "$PR_DATA" | grep "^open_prs=" | cut -d= -f2)
stale_branches=$(echo "$PR_DATA" | grep "^stale_branches=" | cut -d= -f2)
pr_details=$(echo "$PR_DATA" | grep "^pr_details=" | cut -d= -f2-)
branch_details=$(echo "$PR_DATA" | grep "^branch_details=" | cut -d= -f2-)

has_unfixed_gaps=$(echo "$AUDIT_DATA" | grep "^has_unfixed_gaps=" | cut -d= -f2)
gap_p0=$(echo "$AUDIT_DATA" | grep "^gap_p0=" | cut -d= -f2)
gap_p1=$(echo "$AUDIT_DATA" | grep "^gap_p1=" | cut -d= -f2)
gap_p2=$(echo "$AUDIT_DATA" | grep "^gap_p2=" | cut -d= -f2)
latest_audit=$(echo "$AUDIT_DATA" | grep "^latest_audit=" | cut -d= -f2-)
latest_audit_date=$(echo "$AUDIT_DATA" | grep "^latest_audit_date=" | cut -d= -f2-)
ui_audit=$(echo "$AUDIT_DATA" | grep "^ui_audit=" | cut -d= -f2-)
ui_open_total=$(echo "$AUDIT_DATA" | grep "^ui_open_total=" | cut -d= -f2)
gap_details=$(echo "$AUDIT_DATA" | grep "^gap_details=" | cut -d= -f2-)

sec_status=$(echo "$SEC_DATA" | grep "^sec_status=" | cut -d= -f2)
dependabot_status=$(echo "$SEC_DATA" | grep "^dependabot_status=" | cut -d= -f2)
sec_critical=$(echo "$SEC_DATA" | grep "^sec_critical=" | cut -d= -f2)
sec_high=$(echo "$SEC_DATA" | grep "^sec_high=" | cut -d= -f2)
sec_medium=$(echo "$SEC_DATA" | grep "^sec_medium=" | cut -d= -f2)
sec_low=$(echo "$SEC_DATA" | grep "^sec_low=" | cut -d= -f2)
code_scan_status=$(echo "$SEC_DATA" | grep "^code_scan_status=" | cut -d= -f2)
code_scan_errors=$(echo "$SEC_DATA" | grep "^code_scan_errors=" | cut -d= -f2)
code_scan_warnings=$(echo "$SEC_DATA" | grep "^code_scan_warnings=" | cut -d= -f2)
code_scan_notes=$(echo "$SEC_DATA" | grep "^code_scan_notes=" | cut -d= -f2)
secret_scan_status=$(echo "$SEC_DATA" | grep "^secret_scan_status=" | cut -d= -f2)
secret_scan_count=$(echo "$SEC_DATA" | grep "^secret_scan_count=" | cut -d= -f2)

# Defaults when gh unavailable
dependabot_status="${dependabot_status:-unknown}"
sec_critical="${sec_critical:-0}"
sec_high="${sec_high:-0}"
sec_medium="${sec_medium:-0}"
sec_low="${sec_low:-0}"
code_scan_status="${code_scan_status:-unknown}"
code_scan_errors="${code_scan_errors:-0}"
code_scan_warnings="${code_scan_warnings:-0}"
code_scan_notes="${code_scan_notes:-0}"
secret_scan_status="${secret_scan_status:-unknown}"
secret_scan_count="${secret_scan_count:-0}"

# Determine level
LEVEL=$(determine_level "$ci_status" "$ci_days_red" "$open_prs" "$stale_branches" "$has_unfixed_gaps" "$gap_p0" "$ci_failed_history" "$dependabot_status" "$sec_critical" "$sec_high" "$code_scan_errors" "$code_scan_warnings" "$secret_scan_count")

# --- Output ---
case "$OUTPUT_MODE" in
    level)
        echo "$LEVEL"
        ;;
    json)
        cat <<EOJSON
{
  "level": "$LEVEL",
  "ci": {
    "status": "$ci_status",
    "failures": $ci_failures,
    "days_red": $ci_days_red,
    "last_green": "$ci_last_green",
    "failed_history": $ci_failed_history
  },
  "branches": {
    "open_prs": $open_prs,
    "stale_branches": $stale_branches
  },
  "audit": {
    "has_unfixed_gaps": $has_unfixed_gaps,
    "p0": $gap_p0,
    "p1": $gap_p1,
    "p2": $gap_p2,
    "latest_audit": "$latest_audit",
    "latest_audit_date": "$latest_audit_date"
  },
  "security": {
    "dependabot_status": "$dependabot_status",
    "critical": $sec_critical,
    "high": $sec_high,
    "medium": $sec_medium,
    "low": $sec_low,
    "code_scan_status": "$code_scan_status",
    "code_scan_errors": $code_scan_errors,
    "code_scan_warnings": $code_scan_warnings,
    "code_scan_notes": $code_scan_notes,
    "secret_scan_status": "$secret_scan_status",
    "secret_scan_alerts": $secret_scan_count
  }
}
EOJSON
        ;;
    full)
        echo ""
        echo "═══════════════════════════════════════════════════════════"

        case "$LEVEL" in
            GREEN) echo -e "  Repo Status: ${GREEN}${BOLD}■ GREEN — Healthy${NC}" ;;
            YELLOW) echo -e "  Repo Status: ${YELLOW}${BOLD}■ YELLOW — Minor Issues${NC}" ;;
            ORANGE) echo -e "  Repo Status: ${ORANGE}${BOLD}■ ORANGE — Needs Attention${NC}" ;;
            RED) echo -e "  Repo Status: ${RED}${BOLD}■ RED — Degraded${NC}" ;;
            BLACK) echo -e "  Repo Status: ${RED}${BOLD}■ BLACK — Broken${NC}" ;;
        esac

        echo "═══════════════════════════════════════════════════════════"
        echo ""

        # Factor 1: CI
        echo "───────────────────────────────────────────────────────────"
        if [ "$ci_status" = "green" ]; then
            echo -e "  ${GREEN}✅ CI:${NC} All workflows passing on main"
        elif [ "$ci_status" = "failing" ]; then
            echo -e "  ${RED}❌ CI:${NC} $ci_failures workflow(s) failing on main"
            echo "     Days since last green: $ci_days_red"
            if [ -n "$ci_details" ]; then
                echo "     Details:"
                echo "$ci_details" | while IFS= read -r line; do
                    [ -n "$line" ] && echo "       $line"
                done
            fi
        else
            echo -e "  ${YELLOW}⚠️  CI:${NC} Status unknown"
        fi
        # CI history cleanliness
        if [ "$ci_failed_history" -gt 2 ]; then
            echo -e "  ${YELLOW}⚠️  CI History:${NC} $ci_failed_history failed runs (cleanup needed, run: scripts/cleanup-ci-runs.sh)"
        elif [ "$ci_failed_history" -gt 0 ]; then
            echo -e "  ${CYAN}ℹ️  CI History:${NC} $ci_failed_history failed run(s) in history"
        fi
        echo ""

        # Factor 2: PRs + Branches
        echo "───────────────────────────────────────────────────────────"
        if [ "$open_prs" -eq 0 ] && [ "$stale_branches" -eq 0 ]; then
            echo -e "  ${GREEN}✅ PRs & Branches:${NC} Clean (0 open PRs, 0 stale branches)"
        else
            if [ "$open_prs" -gt 0 ]; then
                echo -e "  ${YELLOW}⚠️  Open PRs:${NC} $open_prs"
                if [ -n "$pr_details" ]; then
                    echo "$pr_details" | while IFS= read -r line; do
                        [ -n "$line" ] && echo "       $line"
                    done
                fi
            else
                echo -e "  ${GREEN}✅ Open PRs:${NC} 0"
            fi
            if [ "$stale_branches" -gt 0 ]; then
                echo -e "  ${YELLOW}⚠️  Stale Branches:${NC} $stale_branches (unmerged into main)"
                if [ -n "$branch_details" ]; then
                    echo "$branch_details" | while IFS= read -r line; do
                        [ -n "$line" ] && echo "       $line"
                    done
                fi
            else
                echo -e "  ${GREEN}✅ Stale Branches:${NC} 0"
            fi
        fi
        echo ""

        # Factor 3: Audit Gaps
        echo "───────────────────────────────────────────────────────────"
        if [ "$has_unfixed_gaps" = "unknown" ]; then
            echo -e "  ${YELLOW}⚠️  Audit Gaps:${NC} No audit report found"
        elif [ "$has_unfixed_gaps" = "true" ]; then
            echo -e "  ${RED}❌ Audit Gaps:${NC} Unfixed gaps found"
            echo "     P0 (critical): $gap_p0"
            echo "     P1 (high):     $gap_p1"
            echo "     P2 (medium):   $gap_p2"
            echo "     Latest audit:  $latest_audit_date ($latest_audit)"
            if [ -n "$gap_details" ]; then
                echo "     Items:"
                echo "$gap_details" | while IFS= read -r line; do
                    [ -n "$line" ] && echo "       $line"
                done
            fi
        else
            echo -e "  ${GREEN}✅ Audit Gaps:${NC} No P0/P1 gaps (latest: $latest_audit_date)"
            if [ "$gap_p2" -gt 0 ]; then
                echo "     P2 (minor): $gap_p2 items"
            fi
        fi

        # Factor 4: GitHub Security
        echo ""
        echo "───────────────────────────────────────────────────────────"
        sec_total_high=$((sec_high + code_scan_errors))
        if [ "$sec_status" = "unknown" ]; then
            echo -e "  ${YELLOW}⚠️  Security:${NC} gh unavailable — cannot query alerts"
        elif [ "$sec_critical" -gt 0 ] || [ "$secret_scan_count" -gt 0 ]; then
            echo -e "  ${RED}🔴 Security:${NC} CRITICAL — $sec_critical critical CVE, $secret_scan_count secret alert(s)"
        elif [ "$sec_total_high" -gt 0 ]; then
            echo -e "  ${RED}❌ Security:${NC} $sec_total_high HIGH-severity alert(s) (Dependabot: $sec_high, CodeQL: $code_scan_errors)"
            [ "$dependabot_status" = "disabled" ] && echo -e "     ${YELLOW}⚠️  Dependabot also DISABLED — auto-tracking missing${NC}"
            echo "     Run: gh api repos/\$(gh repo view --json nameWithOwner --jq .nameWithOwner)/code-scanning/alerts?state=open"
        elif [ "$dependabot_status" = "disabled" ]; then
            echo -e "  ${YELLOW}⚠️  Security:${NC} Dependabot DISABLED — enable at repo Settings → Code security and analysis"
            [ "$code_scan_warnings" -gt 0 ] && echo "     CodeQL warnings: $code_scan_warnings"
        elif [ "$code_scan_warnings" -gt 0 ] || [ "$sec_medium" -gt 0 ]; then
            echo -e "  ${YELLOW}⚠️  Security:${NC} $code_scan_warnings CodeQL warning(s), $sec_medium medium Dependabot alert(s)"
        else
            echo -e "  ${GREEN}✅ Security:${NC} 0 CVE, 0 secrets, Dependabot $dependabot_status"
        fi

        # Factor 5: UI Audit
        if [ -n "$ui_audit" ]; then
            echo "───────────────────────────────────────────────────────────"
            if [ "$ui_open_total" -gt 0 ]; then
                echo -e "  ${CYAN}ℹ️  UI Audit:${NC} $ui_open_total open issue(s) ($ui_audit)"
            else
                echo -e "  ${GREEN}✅ UI Audit:${NC} All issues resolved"
            fi
        fi

        echo ""
        echo "═══════════════════════════════════════════════════════════"
        echo "  Run /repo-status in Claude Code for full analysis"
        echo "═══════════════════════════════════════════════════════════"
        echo ""
        ;;
esac

# Exit code based on level
case "$LEVEL" in
    GREEN|YELLOW) exit 0 ;;
    ORANGE)       exit 1 ;;
    RED)          exit 2 ;;
    BLACK)        exit 3 ;;
esac
