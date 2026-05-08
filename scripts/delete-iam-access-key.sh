#!/usr/bin/env bash
# Delete an orphan IAM access key (no local profile swap).
#
# Use case: a key was created (e.g. for one-off bootstrap or via Console)
# and is no longer associated with any local profile but still exists in
# IAM. Use `rotate-iam-access-key.sh` instead when the key IS bound to a
# local profile and needs replacement.
#
# Usage:
#   scripts/delete-iam-access-key.sh <iam-user-name> <akid> <admin-profile>
#
# Per `.claude/rules/agent-aws-access.md` §4.1 — `delete-access-key` is a
# Tier 3 mutation. Wrapping in committed script = "documented workflow"
# carve-out, agents may execute.
#
# Per `.claude/rules/agent-action-bias.md` §1 Part A — agent runs script
# vs user runs raw commands.

set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <iam-user-name> <akid> <admin-profile>" >&2
  exit 64
fi

IAM_USER="$1"
TARGET_AKID="$2"
ADMIN_PROFILE="$3"

log() { printf '[delete-key] %s\n' "$*"; }

# --- 0. Pre-flight ----------------------------------------------------------
log "Admin profile: $ADMIN_PROFILE"
log "Target: user=$IAM_USER akid=$TARGET_AKID (suffix ${TARGET_AKID: -4})"

ADMIN_ARN=$(aws sts get-caller-identity --profile "$ADMIN_PROFILE" --query 'Arn' --output text)
log "Admin identity: $ADMIN_ARN"

# Refuse to delete the key the admin profile itself is using (would self-lock).
ADMIN_AKID=$(aws configure get aws_access_key_id --profile "$ADMIN_PROFILE" || echo "")
if [[ "$ADMIN_AKID" == "$TARGET_AKID" ]]; then
  log "ERROR: target AKID is the same key the admin profile uses." >&2
  log "       Use rotate-iam-access-key.sh instead (creates new, swaps, deletes old)." >&2
  exit 1
fi

# Verify target key exists + is on the named user.
KEYS=$(aws iam list-access-keys --user-name "$IAM_USER" --profile "$ADMIN_PROFILE" \
  --query 'AccessKeyMetadata[].AccessKeyId' --output text)
if ! grep -qw "$TARGET_AKID" <<<"$KEYS"; then
  log "ERROR: $TARGET_AKID not found on user $IAM_USER. Existing keys: $KEYS" >&2
  exit 1
fi

# Show pre-state
log "Pre-state on $IAM_USER:"
aws iam list-access-keys --user-name "$IAM_USER" --profile "$ADMIN_PROFILE" \
  --query 'AccessKeyMetadata[].{AKID:AccessKeyId,Status:Status,Created:CreateDate}' \
  --output table

# --- 1. Inactivate first (defense-in-depth) ---------------------------------
log "Step 1/2: inactivate $TARGET_AKID"
aws iam update-access-key --user-name "$IAM_USER" \
  --access-key-id "$TARGET_AKID" --status Inactive --profile "$ADMIN_PROFILE"

# --- 2. Delete --------------------------------------------------------------
log "Step 2/2: delete $TARGET_AKID (irreversible)"
aws iam delete-access-key --user-name "$IAM_USER" \
  --access-key-id "$TARGET_AKID" --profile "$ADMIN_PROFILE"

# --- Post-state -------------------------------------------------------------
log "Post-state on $IAM_USER:"
aws iam list-access-keys --user-name "$IAM_USER" --profile "$ADMIN_PROFILE" \
  --query 'AccessKeyMetadata[].{AKID:AccessKeyId,Status:Status,Created:CreateDate}' \
  --output table

log "Deletion complete — $TARGET_AKID removed."
