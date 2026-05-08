#!/usr/bin/env bash
# Clean up GitHub Deployments page entries.
#
# Strategy:
#   - Delete ALL `Preview – *` environments (Vercel-bot ephemeral, no value retained)
#   - Keep last N (default 5) `Production – *` per environment, delete older
#   - Skip `github-pages` (managed by GitHub Pages action, leave alone)
#   - Skip any other unknown environment for safety
#
# GitHub API requires deployments be set to `inactive` status before delete.
# Script handles both steps atomically per deployment.
#
# Usage:
#   scripts/cleanup-github-deployments.sh [--dry-run] [--keep N] [--repo owner/repo]
#
# Default repo: detected via `gh repo view --json nameWithOwner`.
# Default keep: 5 production entries per environment.
#
# Per `.claude/rules/agent-action-bias.md` §1 — agent runs script vs user runs raw API calls.

set -euo pipefail

DRY_RUN=0
KEEP_PROD=5
REPO=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1; shift ;;
    --keep) KEEP_PROD="$2"; shift 2 ;;
    --repo) REPO="$2"; shift 2 ;;
    -h|--help) sed -n '2,/^$/p' "$0" | sed 's|^# \?||'; exit 0 ;;
    *) echo "Unknown arg: $1" >&2; exit 64 ;;
  esac
done

if [[ -z "$REPO" ]]; then
  REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner)
fi

log() { printf '[cleanup-deploy] %s\n' "$*"; }

[[ "$DRY_RUN" == 1 ]] && log "DRY RUN — no changes will be made"
log "Repo: $REPO"
log "Keep last $KEEP_PROD production per environment"

# --- Inventory ---
log "Fetching deployment inventory..."
INVENTORY=$(gh api "repos/$REPO/deployments" --paginate \
  --jq '.[] | {id, env: .environment, created: .created_at}' \
  | jq -s 'sort_by(.created) | reverse')  # newest first

TOTAL=$(jq 'length' <<<"$INVENTORY")
log "Total deployments: $TOTAL"

# Print environment breakdown
log "Breakdown by environment:"
jq -r 'group_by(.env) | .[] | "  \(.[0].env): \(length)"' <<<"$INVENTORY" | sort

# --- Build delete list ---
# Rule: delete all Preview – *, keep last N Production – *, skip github-pages, skip unknown
DELETE_IDS=()

# All Preview-* deployments → delete
PREVIEW_IDS=$(jq -r '.[] | select(.env | startswith("Preview")) | .id' <<<"$INVENTORY")
while IFS= read -r id; do
  [[ -n "$id" ]] && DELETE_IDS+=("$id")
done <<<"$PREVIEW_IDS"

# Production – * grouped by env, keep newest N
for env_name in "Production – kitehub" "Production – kiteclass"; do
  PROD_IDS=$(jq -r --arg env "$env_name" \
    '.[] | select(.env == $env) | .id' <<<"$INVENTORY")
  count=0
  while IFS= read -r id; do
    [[ -z "$id" ]] && continue
    count=$((count + 1))
    if (( count > KEEP_PROD )); then
      DELETE_IDS+=("$id")
    fi
  done <<<"$PROD_IDS"
done

# github-pages and unknowns: skipped (no append)

DELETE_COUNT=${#DELETE_IDS[@]}
KEEP_COUNT=$((TOTAL - DELETE_COUNT))
log "Will delete: $DELETE_COUNT"
log "Will keep:   $KEEP_COUNT (recent prod + github-pages + unknown)"

if [[ "$DELETE_COUNT" == 0 ]]; then
  log "Nothing to delete. Exiting."
  exit 0
fi

if [[ "$DRY_RUN" == 1 ]]; then
  log "Dry-run preview (first 10 IDs that would be deleted):"
  for i in "${!DELETE_IDS[@]}"; do
    [[ "$i" -ge 10 ]] && break
    echo "  ${DELETE_IDS[$i]}"
  done
  log "Run without --dry-run to apply."
  exit 0
fi

# --- Execute deletions ---
log "Starting deletion of $DELETE_COUNT deployments..."
ok=0
fail=0
for id in "${DELETE_IDS[@]}"; do
  # Step 1: set status inactive (required by API before delete)
  if gh api "repos/$REPO/deployments/$id/statuses" \
       -X POST -f state=inactive >/dev/null 2>&1; then
    : # ok
  else
    log "  WARN: could not set inactive on $id (may already be inactive or no permission)"
  fi

  # Step 2: delete
  if gh api "repos/$REPO/deployments/$id" -X DELETE >/dev/null 2>&1; then
    ok=$((ok + 1))
  else
    fail=$((fail + 1))
    log "  FAIL: could not delete $id"
  fi

  # Progress every 25
  if (( (ok + fail) % 25 == 0 )); then
    log "  progress: $((ok + fail))/$DELETE_COUNT (ok=$ok fail=$fail)"
  fi
done

log "Done. Deleted: $ok, Failed: $fail"

# --- Post-state ---
log "Post-state inventory:"
gh api "repos/$REPO/deployments" --paginate --jq '.[] | .environment' \
  | sort | uniq -c | sort -rn | sed 's|^|  |'
