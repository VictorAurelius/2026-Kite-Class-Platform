#!/bin/bash
set -euo pipefail

# check-ci.sh - Check or wait for GitHub Actions CI
# NOTE: chmod +x this file before use
#
# Usage:
#   ./scripts/check-ci.sh                     # Wait for CI on current branch (default)
#   ./scripts/check-ci.sh --status            # Quick status check, no waiting
#   ./scripts/check-ci.sh my-branch           # Wait for CI on specific branch
#   ./scripts/check-ci.sh my-branch --status  # Quick status for specific branch
#   ./scripts/check-ci.sh my-branch 20        # Wait with 20-minute timeout
#
# Requirements: gh CLI (https://cli.github.com/)

# Parse arguments
STATUS_ONLY=false
BRANCH=""
TIMEOUT_MINUTES=15

for arg in "$@"; do
    case "$arg" in
        --status) STATUS_ONLY=true ;;
        [0-9]*) TIMEOUT_MINUTES="$arg" ;;
        *) BRANCH="$arg" ;;
    esac
done

BRANCH="${BRANCH:-$(git branch --show-current)}"
POLL_INTERVAL=15

# Fetch CI runs
fetch_runs() {
    gh run list --branch "$BRANCH" --limit 10 --json name,status,conclusion --jq '.[] | "\(.name)\t\(.status)\t\(.conclusion // "")"'
}

# Display CI status
display_status() {
    local RUNS="$1"
    local IN_PROGRESS SUCCESS FAILURE TOTAL

    IN_PROGRESS=$(echo "$RUNS" | grep -c "in_progress" || true)
    SUCCESS=$(echo "$RUNS" | grep -c "success" || true)
    FAILURE=$(echo "$RUNS" | grep -c "failure" || true)
    TOTAL=$(echo "$RUNS" | wc -l)

    echo "==========================================================="
    echo "  CI Status: $BRANCH"
    echo "==========================================================="
    echo ""
    echo "Summary:"
    echo "  [PASS]        $SUCCESS"
    echo "  [FAIL]        $FAILURE"
    echo "  [IN PROGRESS] $IN_PROGRESS"
    echo "  [TOTAL]       $TOTAL"
    echo ""
    echo "-----------------------------------------------------------"
    echo "Details:"
    while IFS=$'\t' read -r name status conclusion; do
        if [ "$status" = "in_progress" ]; then
            echo "  [..] $name: in_progress"
        elif [ "$conclusion" = "success" ]; then
            echo "  [OK] $name: success"
        elif [ "$conclusion" = "failure" ]; then
            echo "  [!!] $name: failure"
        else
            echo "  [--] $name: $status"
        fi
    done <<< "$RUNS"
    echo "-----------------------------------------------------------"

    # Return counts via global vars
    _IN_PROGRESS=$IN_PROGRESS
    _FAILURE=$FAILURE
}

# --- Quick status mode ---
if $STATUS_ONLY; then
    echo "CI status for branch: $BRANCH (quick check)"
    echo ""
    RUNS=$(fetch_runs)
    display_status "$RUNS"
    echo ""
    if [ "$_IN_PROGRESS" -gt 0 ]; then
        echo "[..] CI still running ($_IN_PROGRESS in progress)"
        echo "     Run without --status to wait for completion"
        exit 2
    elif [ "$_FAILURE" -gt 0 ]; then
        echo "[!!] CI has failures"
        exit 1
    else
        echo "[OK] All CI checks passed!"
        exit 0
    fi
fi

# --- Wait mode ---
echo "Checking CI status for branch: $BRANCH"
echo "Timeout: ${TIMEOUT_MINUTES} minutes"
echo ""

TIMEOUT_SECONDS=$((TIMEOUT_MINUTES * 60))
ELAPSED=0

while [ $ELAPSED -lt $TIMEOUT_SECONDS ]; do
    RUNS=$(fetch_runs)

    # Use separator instead of clear (clear breaks non-interactive shells like Claude Code)
    echo ""
    echo "--- poll $(date +%H:%M:%S) ---"
    display_status "$RUNS"
    echo ""
    printf "Elapsed: %d/%d seconds\n" "$ELAPSED" "$TIMEOUT_SECONDS"

    # Check if all done
    if [ "$_IN_PROGRESS" -eq 0 ]; then
        echo ""
        if [ "$_FAILURE" -eq 0 ]; then
            echo "[OK] All CI checks passed!"
            exit 0
        else
            echo "[!!] Some CI checks failed!"
            echo ""
            echo "Failed runs:"
            echo "$RUNS" | grep "failure" | cut -f1 | sed 's/^/  - /'
            echo ""
            echo "View logs: gh run list --branch $BRANCH"
            exit 1
        fi
    fi

    sleep $POLL_INTERVAL
    ELAPSED=$((ELAPSED + POLL_INTERVAL))
done

echo ""
echo "Timeout reached after ${TIMEOUT_MINUTES} minutes"
echo "CI still in progress. Check manually:"
echo "  ./scripts/check-ci.sh --status"
exit 2
