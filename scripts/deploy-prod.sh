#!/usr/bin/env bash
#
# deploy-prod.sh — Phase 1 BETA production deploy bootstrap
#
# Runs ON kh-backend EC2 (i-0b65c3947d36cae61) via SSM session OR
# `aws ssm send-command`. NOT for agent execution per agent-aws-access.md
# §4.3 — production EC2 mutation = user-only.
#
# What it does (idempotent):
#   1. cd /opt/kite-prod (creates dir if missing)
#   2. git pull docker-compose.production.yml + scripts/ (or wget from S3)
#   3. ECR login via /etc/ecr-login.sh (set up by terraform user_data)
#   4. fetch-secrets.sh → /etc/kite/.env
#   5. docker compose pull (latest tag)
#   6. docker compose up -d --remove-orphans
#   7. Wait + healthcheck
#
# Usage on EC2:
#   sudo KITE_VERSION=v0.9.0-beta-staging.8 bash scripts/deploy-prod.sh
#
# Or via SSM run-command (operator from local):
#   aws ssm send-command \
#     --instance-ids i-0b65c3947d36cae61 \
#     --document-name AWS-RunShellScript \
#     --parameters 'commands=["sudo KITE_VERSION=v0.9.0-beta-staging.8 bash /opt/kite-prod/scripts/deploy-prod.sh"]' \
#     --timeout-seconds 600 \
#     --region ap-southeast-1

set -euo pipefail

KITE_VERSION="${KITE_VERSION:-v0.9.0-beta-staging.8}"
DEPLOY_DIR="/opt/kite-prod"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.production.yml"
LOG="/var/log/kite-deploy.log"

log() { echo "[$(date -u +%FT%TZ)] $*" | tee -a "$LOG"; }

log "==================== deploy-prod.sh START ===================="
log "KITE_VERSION=${KITE_VERSION}"
log "DEPLOY_DIR=${DEPLOY_DIR}"

# Step 1: Ensure deploy directory + permissions
sudo mkdir -p "$DEPLOY_DIR"
sudo chown ec2-user:ec2-user "$DEPLOY_DIR"

cd "$DEPLOY_DIR"

# Step 2: Pull / sync repo artifacts
# Operator initial bootstrap: clone repo to /opt/kite-prod
# Subsequent deploys: git pull
if [[ ! -d "$DEPLOY_DIR/.git" ]]; then
  log "Initial bootstrap: cloning repo (shallow)..."
  if [[ -n "${KITE_REPO_URL:-}" ]]; then
    git clone --depth 1 "$KITE_REPO_URL" "$DEPLOY_DIR" || {
      log "ERROR: git clone failed — set KITE_REPO_URL env or upload manually"
      exit 1
    }
  else
    log "WARN: \$DEPLOY_DIR/.git missing AND \$KITE_REPO_URL unset"
    log "WARN: Skipping git pull. Operator must scp/aws-s3-cp compose file + scripts manually."
    log "WARN: Required files: $COMPOSE_FILE + scripts/fetch-secrets.sh"
  fi
else
  log "Updating repo..."
  cd "$DEPLOY_DIR"
  git fetch --depth 1 origin main && git reset --hard origin/main || log "WARN: git pull failed, using existing artifacts"
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  log "ERROR: $COMPOSE_FILE not found. Bootstrap manually:"
  log "ERROR:   sudo cp /path/to/docker-compose.production.yml $COMPOSE_FILE"
  exit 1
fi

# Step 3: ECR login (uses instance profile, no static keys)
log "ECR login..."
if [[ -x /etc/ecr-login.sh ]]; then
  sudo /etc/ecr-login.sh | tee -a "$LOG"
else
  log "WARN: /etc/ecr-login.sh missing — running inline"
  aws ecr get-login-password --region ap-southeast-1 \
    | sudo docker login --username AWS --password-stdin \
      906286017800.dkr.ecr.ap-southeast-1.amazonaws.com
fi

# Step 4: Fetch secrets from AWS Secrets Manager
log "Fetching secrets..."
if [[ -x "$DEPLOY_DIR/scripts/fetch-secrets.sh" ]]; then
  KITE_VERSION="$KITE_VERSION" sudo --preserve-env=KITE_VERSION,RESEND_API_KEY \
    bash "$DEPLOY_DIR/scripts/fetch-secrets.sh"
else
  log "ERROR: fetch-secrets.sh not found at $DEPLOY_DIR/scripts/"
  exit 1
fi

# Step 5: Pull latest images
log "docker compose pull (KITE_VERSION=$KITE_VERSION)..."
KITE_VERSION="$KITE_VERSION" sudo --preserve-env=KITE_VERSION \
  docker compose -f "$COMPOSE_FILE" pull

# Step 6: Up
log "docker compose up -d --remove-orphans..."
KITE_VERSION="$KITE_VERSION" sudo --preserve-env=KITE_VERSION \
  docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

# Step 7: Wait + healthcheck
log "Waiting 60s for stack to settle..."
sleep 60

log "Container status:"
sudo docker compose -f "$COMPOSE_FILE" ps | tee -a "$LOG"

log "Gateway healthcheck:"
if curl -fsS -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health 2>&1 | tee -a "$LOG"; then
  log "==================== deploy-prod.sh OK ===================="
else
  log "WARN: Gateway healthcheck not 200 yet — may still be starting"
  log "Check: docker compose -f $COMPOSE_FILE logs --tail 50 kitehub-gateway"
fi

# Cleanup old images
sudo docker system prune -f --filter "until=24h" || true
