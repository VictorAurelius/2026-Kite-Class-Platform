#!/bin/bash
set -euo pipefail

# check-ci.sh - Wait for GitHub Actions CI to complete
# Usage: ./scripts/check-ci.sh [branch-name] [timeout-minutes]

BRANCH="${1:-$(git branch --show-current)}"
TIMEOUT_MINUTES="${2:-15}"
POLL_INTERVAL=15 # seconds

echo "🔍 Checking CI status for branch: $BRANCH"
echo "⏱️  Timeout: ${TIMEOUT_MINUTES} minutes"
echo ""

TIMEOUT_SECONDS=$((TIMEOUT_MINUTES * 60))
ELAPSED=0

while [ $ELAPSED -lt $TIMEOUT_SECONDS ]; do
    # Get CI runs for this branch (tab-separated format)
    RUNS=$(gh run list --branch "$BRANCH" --limit 10 --json name,status,conclusion --jq '.[] | "\(.name)\t\(.status)\t\(.conclusion // "")"')

    # Count different statuses
    IN_PROGRESS=$(echo "$RUNS" | grep -c "in_progress" || true)
    SUCCESS=$(echo "$RUNS" | grep -c "success" || true)
    FAILURE=$(echo "$RUNS" | grep -c "failure" || true)
    TOTAL=$(echo "$RUNS" | wc -l)

    # Display status
    clear
    echo "═══════════════════════════════════════════════════════════"
    echo "  CI Status: $BRANCH"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    echo "📊 Summary:"
    echo "  ✅ Success:     $SUCCESS"
    echo "  ❌ Failure:     $FAILURE"
    echo "  ⏳ In Progress: $IN_PROGRESS"
    echo "  📦 Total:       $TOTAL"
    echo ""
    echo "───────────────────────────────────────────────────────────"
    echo "Details:"
    while IFS=$'\t' read -r name status conclusion; do
        if [ "$status" = "in_progress" ]; then
            echo "  ⏳ $name: $status"
        elif [ "$conclusion" = "success" ]; then
            echo "  ✅ $name: $conclusion"
        elif [ "$conclusion" = "failure" ]; then
            echo "  ❌ $name: $conclusion"
        else
            echo "  📦 $name: $status"
        fi
    done <<< "$RUNS"
    echo "───────────────────────────────────────────────────────────"
    echo ""
    printf "⏱️  Elapsed: %d/%d seconds\n" "$ELAPSED" "$TIMEOUT_SECONDS"

    # Check if all done
    if [ "$IN_PROGRESS" -eq 0 ]; then
        echo ""
        if [ "$FAILURE" -eq 0 ]; then
            echo "✅ All CI checks passed!"
            exit 0
        else
            echo "❌ Some CI checks failed!"
            echo ""
            echo "Failed runs:"
            echo "$RUNS" | grep "failure" | cut -f1 | sed 's/^/  - /'
            echo ""
            echo "View logs: gh run list --branch $BRANCH"
            exit 1
        fi
    fi

    # Wait before next check
    sleep $POLL_INTERVAL
    ELAPSED=$((ELAPSED + POLL_INTERVAL))
done

# Timeout
echo ""
echo "⏱️  Timeout reached after ${TIMEOUT_MINUTES} minutes"
echo "CI still in progress. Check manually:"
echo "  gh run list --branch $BRANCH"
exit 2
