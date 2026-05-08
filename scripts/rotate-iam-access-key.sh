#!/usr/bin/env bash
# Rotate an IAM user's access key:
#   1. Create new key (admin profile)
#   2. Update local AWS profile to use new key
#   3. Verify new key works (sts get-caller-identity)
#   4. Inactivate old key
#   5. Delete old key
#
# Usage:
#   scripts/rotate-iam-access-key.sh \
#     <iam-user-name> <old-akid> <target-local-profile> <admin-profile>
#
# Example:
#   scripts/rotate-iam-access-key.sh kite-readonly-wsl AKIA5GAW3FUEMPMSE7SO kite-readonly dev-admin
#
# Per `.claude/rules/agent-aws-access.md` §4.1: create-/update-/delete-access-key
# are Tier 3 mutations. Wrapped in a committed script (= "documented workflow"
# carve-out) so the workflow is auditable + reproducible. Agent may run this.
#
# Per `.claude/rules/agent-action-bias.md` §1: prefer agent-runs-script over
# user-runs-raw-commands.

set -euo pipefail

if [[ $# -ne 4 ]]; then
  echo "Usage: $0 <iam-user-name> <old-akid> <target-local-profile> <admin-profile>" >&2
  exit 64
fi

IAM_USER="$1"
OLD_AKID="$2"
TARGET_PROFILE="$3"
ADMIN_PROFILE="$4"

log() { printf '[rotate-key] %s\n' "$*"; }

# --- 0. Pre-flight checks ---------------------------------------------------
log "Pre-flight: admin profile = $ADMIN_PROFILE, target profile = $TARGET_PROFILE"
log "Pre-flight: IAM user = $IAM_USER, old AKID suffix = ${OLD_AKID: -4}"

ADMIN_ARN=$(aws sts get-caller-identity --profile "$ADMIN_PROFILE" --query 'Arn' --output text)
log "Admin identity: $ADMIN_ARN"

OLD_KEYS=$(aws iam list-access-keys --user-name "$IAM_USER" --profile "$ADMIN_PROFILE" \
  --query 'AccessKeyMetadata[].AccessKeyId' --output text)
if ! grep -qw "$OLD_AKID" <<<"$OLD_KEYS"; then
  log "ERROR: old AKID $OLD_AKID not found on user $IAM_USER. Existing keys: $OLD_KEYS" >&2
  exit 1
fi

NUM_KEYS=$(awk '{print NF}' <<<"$OLD_KEYS")
if [[ "$NUM_KEYS" -ge 2 ]]; then
  log "ERROR: $IAM_USER already has 2 keys (AWS limit). Delete one before rotating." >&2
  exit 1
fi

# --- 1. Create new key (Tier 3 mutation, scripted) --------------------------
log "Step 1/5: create new access key on $IAM_USER"
NEW_KEY_JSON=$(aws iam create-access-key --user-name "$IAM_USER" --profile "$ADMIN_PROFILE")
NEW_AKID=$(jq -r '.AccessKey.AccessKeyId' <<<"$NEW_KEY_JSON")
NEW_SECRET=$(jq -r '.AccessKey.SecretAccessKey' <<<"$NEW_KEY_JSON")

if [[ -z "$NEW_AKID" || -z "$NEW_SECRET" || "$NEW_AKID" == "null" ]]; then
  log "ERROR: failed to extract new AKID/secret from create-access-key response" >&2
  exit 1
fi
log "New AKID: $NEW_AKID (suffix: ${NEW_AKID: -4})"

# --- 2. Update local profile -------------------------------------------------
log "Step 2/5: update local profile $TARGET_PROFILE with new key"
aws configure set aws_access_key_id "$NEW_AKID" --profile "$TARGET_PROFILE"
aws configure set aws_secret_access_key "$NEW_SECRET" --profile "$TARGET_PROFILE"

# Scrub plaintext secret from script-local var ASAP
NEW_SECRET=""
unset NEW_SECRET
NEW_KEY_JSON=""
unset NEW_KEY_JSON

# --- 3. Verify new key (Tier 1 read) ----------------------------------------
log "Step 3/5: verify new key via sts get-caller-identity"
# AWS IAM creds eventual-consistent — retry up to 5×
for attempt in 1 2 3 4 5; do
  if VERIFY_OUT=$(aws sts get-caller-identity --profile "$TARGET_PROFILE" 2>&1); then
    break
  fi
  if [[ "$attempt" == 5 ]]; then
    log "ERROR: new key did not authenticate after 5 attempts. Last error: $VERIFY_OUT" >&2
    log "ABORT: leaving old key Active for safety. Manual cleanup required." >&2
    exit 1
  fi
  log "  attempt $attempt: not yet propagated, retry in 3s ..."
  sleep 3
done
log "Verified: $(jq -r '.Arn' <<<"$VERIFY_OUT")"

# --- 4. Inactivate old key (Tier 3 mutation, scripted) ----------------------
log "Step 4/5: inactivate old key $OLD_AKID"
aws iam update-access-key --user-name "$IAM_USER" \
  --access-key-id "$OLD_AKID" --status Inactive --profile "$ADMIN_PROFILE"

# --- 5. Delete old key (Tier 3 mutation, scripted) --------------------------
log "Step 5/5: delete old key $OLD_AKID"
aws iam delete-access-key --user-name "$IAM_USER" \
  --access-key-id "$OLD_AKID" --profile "$ADMIN_PROFILE"

# --- Post-state report ------------------------------------------------------
log "Post-state: keys on $IAM_USER:"
aws iam list-access-keys --user-name "$IAM_USER" --profile "$ADMIN_PROFILE" \
  --query 'AccessKeyMetadata[].{AKID:AccessKeyId,Status:Status,Created:CreateDate}' \
  --output table

log "Rotation complete."
log "  IAM user:       $IAM_USER"
log "  Local profile:  $TARGET_PROFILE"
log "  Old AKID:       $OLD_AKID (DELETED)"
log "  New AKID:       $NEW_AKID (ACTIVE)"
