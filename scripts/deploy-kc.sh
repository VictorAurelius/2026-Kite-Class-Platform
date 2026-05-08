#!/usr/bin/env bash
#
# deploy-kc.sh — Phase 1 BETA KC stack deploy on kc-app EC2
#
# Run ON kc-app EC2 (i-04f65503ace7febe4) via SSM. Same pattern as
# deploy-prod.sh (KH stack) — just different compose file.
#
# Per agent-aws-access.md §4.1: docker compose mutations = USER execute
# (or via SSM send-command from CI/local AWS CLI).
#
# Usage on EC2:
#   sudo KITE_VERSION=0.9.0-beta-staging.8 bash /opt/kite-prod/scripts/deploy-kc.sh

set -euo pipefail

KITE_VERSION="${KITE_VERSION:-v0.9.0-beta-staging.8}"
KITE_VERSION="${KITE_VERSION#v}"  # strip v prefix per ECR tag format
DEPLOY_DIR="/opt/kite-prod"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.kc.yml"
LOG="/var/log/kite-deploy-kc.log"

log() { echo "[$(date -u +%FT%TZ)] $*" | tee -a "$LOG"; }

log "==================== deploy-kc.sh START ===================="
log "KITE_VERSION=${KITE_VERSION}"

sudo mkdir -p "$DEPLOY_DIR"
cd "$DEPLOY_DIR"

# Same git pull pattern as deploy-prod.sh
sudo git config --global --add safe.directory "$DEPLOY_DIR" 2>/dev/null || true
git config --global --add safe.directory "$DEPLOY_DIR" 2>/dev/null || true
if [[ -d "$DEPLOY_DIR/.git" ]]; then
  sudo git -C "$DEPLOY_DIR" fetch --depth 1 origin main \
    && sudo git -C "$DEPLOY_DIR" reset --hard origin/main \
    || log "WARN: git pull failed"
else
  log "WARN: $DEPLOY_DIR not a git repo — bootstrap first"
fi

[[ -f "$COMPOSE_FILE" ]] || { log "ERROR: $COMPOSE_FILE missing"; exit 1; }

log "ECR login..."
if [[ -x /etc/ecr-login.sh ]]; then
  sudo /etc/ecr-login.sh | tee -a "$LOG"
else
  aws ecr get-login-password --region ap-southeast-1 \
    | sudo docker login --username AWS --password-stdin \
      906286017800.dkr.ecr.ap-southeast-1.amazonaws.com
fi

log "Fetching secrets..."
if [[ -x "$DEPLOY_DIR/scripts/fetch-secrets.sh" ]]; then
  KITE_VERSION="$KITE_VERSION" sudo --preserve-env=KITE_VERSION bash "$DEPLOY_DIR/scripts/fetch-secrets.sh"
else
  log "ERROR: fetch-secrets.sh missing"; exit 1
fi

log "docker compose pull (KITE_VERSION=$KITE_VERSION)..."
KITE_VERSION="$KITE_VERSION" sudo --preserve-env=KITE_VERSION \
  docker compose -f "$COMPOSE_FILE" pull

log "docker compose up -d --remove-orphans..."
KITE_VERSION="$KITE_VERSION" sudo --preserve-env=KITE_VERSION \
  docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

log "Wait 90s for KC stack..."
sleep 90

log "Container status:"
sudo docker compose -f "$COMPOSE_FILE" ps | tee -a "$LOG"

log "Gateway healthcheck (port 8080 → host 3000):"
if curl -fsS -o /dev/null -w "%{http_code}\n" http://localhost:3000/actuator/health 2>&1 | tee -a "$LOG"; then
  log "==================== deploy-kc.sh OK ===================="
else
  log "WARN: KC gateway healthcheck not 200 yet (may still be starting)"
fi

sudo docker system prune -f --filter "until=24h" || true
