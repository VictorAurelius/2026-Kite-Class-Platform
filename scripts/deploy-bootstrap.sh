#!/usr/bin/env bash
#
# deploy-bootstrap.sh — Phase 1 BETA production FIRST-APPLY bootstrap path
#
# GAP-506 Bucket F (Wave 85): split chicken-and-egg bootstrap concerns out of
# deploy-prod.sh. This script handles the ONE-TIME initial provisioning of
# /opt/kite-prod on a fresh EC2 (clone repo, seed compose file). Subsequent
# routine deploys use deploy-prod.sh instead (which assumes /opt/kite-prod
# already exists and only runs git pull + compose pull + compose up).
#
# Chicken-and-egg context (per release-deploy-standard.md §9 + GAP-449):
#   1. terraform-apply.yml workflow needs OIDC apply role to assume.
#   2. That role itself is created by terraform apply.
#   3. First apply must therefore use admin credentials (one-time bootstrap).
#   4. Same pattern applies on the EC2 side: deploy-prod.sh requires
#      /opt/kite-prod cloned + initial compose seeded; that seed runs ONCE
#      via this script (or operator scp). After that, deploy-prod.sh handles
#      all subsequent deploys via OIDC ephemeral creds path.
#
# Env guards (per Bucket F AC F-AC1):
#   - Set KITE_FIRST_APPLY=true to acknowledge bootstrap intent.
#   - Refuses to run when AWS Parameter Store /kite/bootstrap-done=true.
#     (Prevents accidental re-bootstrap which would clobber any local state.)
#
# Usage on EC2 (one-time):
#   sudo KITE_FIRST_APPLY=true KITE_REPO_URL=https://github.com/.../...git \
#     bash scripts/deploy-bootstrap.sh
#
# After successful run:
#   1. Script writes /kite/bootstrap-done=true to Parameter Store.
#   2. Operator rotates admin key per release-deploy-standard.md §9.
#   3. Subsequent deploys MUST use scripts/deploy-prod.sh (OIDC path).
#
# Related:
#   - documents/05-guides/deploy/bootstrap-runbook.md
#   - documents/05-guides/deploy/terraform-apply-bootstrap-runbook.md
#   - .claude/rules/release-deploy-standard.md §9
#   - GAP-506

set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/kite-prod}"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.production.yml"
LOG="${KITE_BOOTSTRAP_LOG:-/var/log/kite-bootstrap.log}"
SSM_BOOTSTRAP_KEY="${SSM_BOOTSTRAP_KEY:-/kite/bootstrap-done}"
AWS_REGION="${AWS_REGION:-ap-southeast-1}"

# Logging: write to stderr always; append to $LOG when writable (no-op otherwise).
# Avoid `sudo tee` here so script remains testable in non-root contexts.
log() {
  local line
  line="[$(date -u +%FT%TZ)] $*"
  echo "$line" >&2
  if [[ -w "$(dirname "$LOG")" ]] || [[ -w "$LOG" ]] 2>/dev/null; then
    echo "$line" >> "$LOG" 2>/dev/null || true
  fi
}

# --- Env guard 1: KITE_FIRST_APPLY must be explicitly set (Bucket F AC F-AC1) ---
if [[ "${KITE_FIRST_APPLY:-false}" != "true" ]]; then
  log "ERROR: deploy-bootstrap.sh refusing to run."
  log "ERROR: This script is for ONE-TIME first-apply bootstrap only."
  log "ERROR: Set KITE_FIRST_APPLY=true to acknowledge intent."
  log "ERROR: For routine deploys, use scripts/deploy-prod.sh instead."
  exit 2
fi

# --- Env guard 2: refuse if SSM Parameter Store indicates bootstrap already done ---
# Lookup is best-effort; if AWS CLI / IAM not configured (very fresh EC2),
# guard relies on KITE_BOOTSTRAP_DONE override env var only.
BOOTSTRAP_DONE="${KITE_BOOTSTRAP_DONE:-}"
if [[ -z "$BOOTSTRAP_DONE" ]]; then
  BOOTSTRAP_DONE=$(aws ssm get-parameter \
    --name "$SSM_BOOTSTRAP_KEY" \
    --region "$AWS_REGION" \
    --query 'Parameter.Value' \
    --output text 2>/dev/null || echo "")
fi

if [[ "$BOOTSTRAP_DONE" == "true" ]]; then
  log "ERROR: Bootstrap already completed (per $SSM_BOOTSTRAP_KEY or KITE_BOOTSTRAP_DONE env)."
  log "ERROR: Re-running deploy-bootstrap.sh would clobber existing state."
  log "ERROR: For routine deploys, use scripts/deploy-prod.sh."
  log "ERROR: To force re-bootstrap (DESTRUCTIVE): delete $SSM_BOOTSTRAP_KEY first."
  exit 3
fi

log "==================== deploy-bootstrap.sh START ===================="
log "DEPLOY_DIR=${DEPLOY_DIR}"
log "KITE_FIRST_APPLY=true (acknowledged)"
log "Bootstrap guard: $SSM_BOOTSTRAP_KEY NOT set — proceeding."

# --- Step 1: Ensure deploy directory + permissions ---
sudo mkdir -p "$DEPLOY_DIR"
sudo chown ec2-user:ec2-user "$DEPLOY_DIR"

# --- Step 2: Initial clone (only bootstrap path — deploy-prod.sh assumes this done) ---
if [[ -d "$DEPLOY_DIR/.git" ]]; then
  log "WARN: $DEPLOY_DIR/.git already exists — skipping clone."
  log "WARN: If you intend to re-bootstrap, remove $DEPLOY_DIR first."
else
  if [[ -z "${KITE_REPO_URL:-}" ]]; then
    log "ERROR: KITE_REPO_URL env var required for first-time clone."
    log "ERROR: Set KITE_REPO_URL=https://github.com/<org>/<repo>.git and retry."
    exit 4
  fi
  log "Cloning repo from $KITE_REPO_URL (shallow)..."
  sudo -u ec2-user git clone --depth 1 "$KITE_REPO_URL" "$DEPLOY_DIR" 2>&1 | sudo tee -a "$LOG"
fi

# Mark dir trusted for both root + ec2-user contexts (post-clone owned by ec2-user;
# subsequent SSM-as-root operations need explicit safe.directory).
sudo git config --global --add safe.directory "$DEPLOY_DIR" 2>/dev/null || true
git config --global --add safe.directory "$DEPLOY_DIR" 2>/dev/null || true

# --- Step 3: Verify compose file exists ---
if [[ ! -f "$COMPOSE_FILE" ]]; then
  log "ERROR: $COMPOSE_FILE not found post-clone."
  log "ERROR: Either the repo branch lacks docker-compose.production.yml,"
  log "ERROR: or KITE_REPO_URL points to wrong repo."
  exit 5
fi
log "Compose file verified: $COMPOSE_FILE"

# --- Step 4: Mark bootstrap complete in SSM Parameter Store ---
log "Writing $SSM_BOOTSTRAP_KEY=true to Parameter Store (region $AWS_REGION)..."
if aws ssm put-parameter \
     --name "$SSM_BOOTSTRAP_KEY" \
     --value "true" \
     --type String \
     --overwrite \
     --region "$AWS_REGION" >/dev/null 2>&1; then
  log "Bootstrap marker SSM Parameter Store: SET"
else
  log "WARN: Could not write SSM parameter (IAM perms?). Bootstrap still considered complete."
  log "WARN: Operator MUST manually set $SSM_BOOTSTRAP_KEY=true before next deploy."
fi

log "==================== deploy-bootstrap.sh OK ===================="
log ""
log "Next steps:"
log "  1. Rotate admin AWS credentials per release-deploy-standard.md §9."
log "  2. Verify OIDC apply role / deploy role exists in terraform state."
log "  3. Routine deploys MUST use: bash scripts/deploy-prod.sh"
log "  4. See: documents/05-guides/deploy/bootstrap-runbook.md"
