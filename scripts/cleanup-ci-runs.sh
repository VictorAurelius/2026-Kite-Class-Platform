#!/bin/bash
# CI Runs Cleanup Helper
# Usage: ./scripts/cleanup-ci-runs.sh [options]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Defaults
DRY_RUN=false
MERGED_ONLY=false
BRANCH=""

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --branch)
      BRANCH="$2"
      shift 2
      ;;
    --merged-only)
      MERGED_ONLY=true
      shift
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--branch <name> | --merged-only] [--dry-run]"
      exit 1
      ;;
  esac
done

# Cleanup function
cleanup_branch() {
  local branch=$1
  echo -e "${YELLOW}Cleaning: $branch${NC}"

  # Create audit log
  local log_file=".log/ci-cleanup-${branch//\//-}-$(date +%Y%m%d-%H%M%S).json"
  mkdir -p .log
  gh run list --branch "$branch" --limit 100 \
    --json databaseId,conclusion,createdAt,workflowName \
    > "$log_file"

  echo -e "${GREEN}Audit log: $log_file${NC}"

  # Count total runs
  local total=$(gh run list --branch "$branch" --limit 100 | wc -l)
  # Subtract 1 for header line
  total=$((total - 1))

  echo "Total runs: $total"

  if [ "$total" -le 1 ]; then
    echo -e "${GREEN}Only 1 run, nothing to cleanup${NC}"
    return
  fi

  # Get runs to delete (skip first line - header, skip second line - most recent)
  gh run list --branch "$branch" --limit 100 | tail -n +2 | \
    awk '{print $NF}' > /tmp/delete_runs_$$.txt

  local delete_count=$(wc -l < /tmp/delete_runs_$$.txt)

  echo -e "${YELLOW}Will delete $delete_count runs (keeping most recent)${NC}"

  if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}[DRY RUN] Would delete:${NC}"
    cat /tmp/delete_runs_$$.txt
    rm -f /tmp/delete_runs_$$.txt
    return
  fi

  # Delete runs
  while read -r run_id; do
    echo "Deleting run $run_id..."
    echo "y" | gh run delete "$run_id" 2>&1 > /dev/null || echo "Failed: $run_id"
  done < /tmp/delete_runs_$$.txt

  rm -f /tmp/delete_runs_$$.txt

  echo -e "${GREEN}✅ Cleanup complete for $branch${NC}"
}

# Main logic
if [ -n "$BRANCH" ]; then
  cleanup_branch "$BRANCH"
elif [ "$MERGED_ONLY" = true ]; then
  echo -e "${YELLOW}Fetching merged PRs...${NC}"
  merged_branches=$(gh pr list --state merged --limit 50 \
    --json headRefName --jq '.[].headRefName')

  if [ -z "$merged_branches" ]; then
    echo -e "${GREEN}No merged branches${NC}"
    exit 0
  fi

  echo "Found $(echo "$merged_branches" | wc -l) merged branches"

  for branch in $merged_branches; do
    cleanup_branch "$branch"
    echo ""
  done
else
  echo "Usage: $0 [--branch <name> | --merged-only] [--dry-run]"
  exit 1
fi

echo -e "${GREEN}✅ All cleanup tasks completed${NC}"
