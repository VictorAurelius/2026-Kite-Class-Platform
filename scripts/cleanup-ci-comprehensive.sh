#!/bin/bash
# Comprehensive CI Cleanup Script
# Follows policy defined in .github/CI-CLEANUP-POLICY.md
# Usage: ./scripts/cleanup-ci-comprehensive.sh [--mode <branch|weekly|main>] [--dry-run]

set -euo pipefail

# Parse arguments
MODE="${1:-}"
DRY_RUN=false

if [ "$MODE" = "--dry-run" ] || [ "${2:-}" = "--dry-run" ]; then
  DRY_RUN=true
  echo "🔍 DRY RUN MODE - No deletions will be performed"
fi

cleanup_branch() {
  local branch="$1"
  echo ""
  echo "🧹 Cleaning branch: $branch"

  # Get all runs for this branch
  runs=$(gh run list --branch "$branch" --limit 100 --json databaseId,conclusion,status 2>/dev/null || echo "[]")

  if [ "$runs" = "[]" ]; then
    echo "  ℹ️  No runs found"
    return
  fi

  # Count runs
  total=$(echo "$runs" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")
  echo "  📊 Total runs: $total"

  if [ "$total" -eq 0 ]; then
    return
  fi

  # Find last successful run
  success_run=$(echo "$runs" | python3 -c "
import sys, json
data = json.load(sys.stdin)
success = [r for r in data if r['conclusion'] == 'success']
print(success[0]['databaseId'] if success else '')
")

  if [ -n "$success_run" ]; then
    echo "  ✅ Keeping successful run: $success_run"
  fi

  # Get runs to delete (failed and cancelled)
  to_delete=$(echo "$runs" | python3 -c "
import sys, json
data = json.load(sys.stdin)
keep = '$success_run'
for r in data:
    if r['conclusion'] in ['failure', 'cancelled']:
        if not keep or str(r['databaseId']) != keep:
            print(r['databaseId'])
")

  if [ -z "$to_delete" ]; then
    echo "  ✨ Already clean!"
    return
  fi

  delete_count=$(echo "$to_delete" | wc -l)
  echo "  🗑️  Deleting $delete_count runs"

  if [ "$DRY_RUN" = true ]; then
    echo "$to_delete" | while read -r run_id; do
      echo "      [DRY RUN] Would delete: $run_id"
    done
    return
  fi

  # Actually delete
  echo "$to_delete" | while read -r run_id; do
    echo "y" | gh run delete "$run_id" >/dev/null 2>&1 && echo "    ✓ Deleted $run_id" || echo "    ✗ Failed $run_id"
  done

  echo "  ✅ Cleanup complete for $branch"
}

cleanup_main_duplicates() {
  echo ""
  echo "🧹 Cleaning main branch (failed runs only)"

  # Get main branch runs
  runs=$(gh run list --branch main --limit 50 --json databaseId,conclusion,createdAt)

  # Keep latest 3 runs, delete failed runs older than that
  to_delete=$(echo "$runs" | python3 -c "
import sys, json
data = json.load(sys.stdin)

# Sort by date (newest first)
data.sort(key=lambda x: x['createdAt'], reverse=True)

# Keep latest 3, delete only failed runs beyond that
for i, r in enumerate(data):
    if i >= 3 and r['conclusion'] == 'failure':
        print(r['databaseId'])
")

  if [ -z "$to_delete" ]; then
    echo "  ✨ Main branch is clean!"
    return
  fi

  delete_count=$(echo "$to_delete" | wc -l)
  echo "  🗑️  Deleting $delete_count failed runs"

  if [ "$DRY_RUN" = true ]; then
    echo "$to_delete" | while read -r run_id; do
      echo "      [DRY RUN] Would delete: $run_id"
    done
    return
  fi

  echo "$to_delete" | while read -r run_id; do
    echo "y" | gh run delete "$run_id" >/dev/null 2>&1 && echo "    ✓ Deleted $run_id" || echo "    ✗ Failed $run_id"
  done

  echo "  ✅ Main branch cleanup complete"
}

cleanup_weekly() {
  echo ""
  echo "🧹 Weekly cleanup (failed/cancelled runs older than 7 days)"

  cutoff=$(date -u -d '7 days ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-7d +%Y-%m-%dT%H:%M:%SZ)

  runs=$(gh run list --limit 200 --json databaseId,conclusion,createdAt,headBranch)

  to_delete=$(echo "$runs" | python3 -c "
import sys, json
from datetime import datetime

data = json.load(sys.stdin)
cutoff = '$cutoff'

for r in data:
    if r['conclusion'] in ['failure', 'cancelled']:
        if r['createdAt'] < cutoff and r['headBranch'] != 'main':
            print(r['databaseId'])
")

  if [ -z "$to_delete" ]; then
    echo "  ✨ No old runs to cleanup!"
    return
  fi

  delete_count=$(echo "$to_delete" | wc -l)
  echo "  🗑️  Deleting $delete_count old runs"

  if [ "$DRY_RUN" = true ]; then
    echo "$to_delete" | while read -r run_id; do
      echo "      [DRY RUN] Would delete: $run_id"
    done
    return
  fi

  echo "$to_delete" | while read -r run_id; do
    echo "y" | gh run delete "$run_id" >/dev/null 2>&1 && echo "    ✓ Deleted $run_id" || true
  done

  echo "  ✅ Weekly cleanup complete"
}

case "$MODE" in
  --branch)
    if [ -z "${2:-}" ]; then
      echo "Error: --branch requires branch name"
      exit 1
    fi
    cleanup_branch "$2"
    ;;
  --main)
    cleanup_main_duplicates
    ;;
  --weekly)
    cleanup_weekly
    cleanup_main_duplicates
    ;;
  --all)
    echo "=== COMPREHENSIVE CLEANUP ==="

    # 1. Clean all failed/cancelled PRs
    echo ""
    echo "Step 1/2: Cleaning up all PR branches"

    pr_branches=$(gh run list --limit 200 --json headBranch | python3 -c "
import sys, json
branches = set()
for r in json.load(sys.stdin):
    if r['headBranch'] != 'main':
        branches.add(r['headBranch'])
for b in sorted(branches):
    print(b)
")

    echo "Found $(echo "$pr_branches" | wc -l) unique branches"

    echo "$pr_branches" | while read -r branch; do
      cleanup_branch "$branch"
    done

    # 2. Clean main
    echo ""
    echo "Step 2/2: Cleaning main branch"
    cleanup_main_duplicates

    echo ""
    echo "✅ COMPREHENSIVE CLEANUP COMPLETE"
    ;;
  *)
    echo "Usage: $0 [--branch <name> | --main | --weekly | --all] [--dry-run]"
    echo ""
    echo "Modes:"
    echo "  --branch <name>  Clean specific branch (keep last success, delete failed/cancelled)"
    echo "  --main           Clean main branch (delete duplicate failed runs)"
    echo "  --weekly         Clean old failed runs (>7 days) + main"
    echo "  --all            Comprehensive cleanup of all branches"
    echo ""
    echo "Options:"
    echo "  --dry-run        Show what would be deleted without deleting"
    exit 1
    ;;
esac
