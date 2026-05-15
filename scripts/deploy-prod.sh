#!/usr/bin/env bash
#
# deploy-prod.sh — Phase 1 BETA production routine deploy (POST-BOOTSTRAP)
#
# GAP-506 Bucket F (Wave 85): refactored to remove bootstrap concerns
# (initial clone, repo seed). This script now ASSUMES /opt/kite-prod is
# already cloned + initial compose file present (via deploy-bootstrap.sh
# run one time). Subsequent routine deploys use OIDC ephemeral credentials
# only — no static admin keys.
#
# Runs ON kh-backend EC2 (i-0b65c3947d36cae61) via SSM session OR
# `aws ssm send-command`. NOT for agent execution per agent-aws-access.md
# §4.3 — production EC2 mutation = user-only.
#
# Env guards (per Bucket F AC F-AC1):
#   - Refuses to run when KITE_FIRST_APPLY=true (force user to use
#     deploy-bootstrap.sh for first-apply path).
#
# What it does (idempotent):
#   1. cd /opt/kite-prod (errors if missing — bootstrap must have run)
#   2. git pull docker-compose.production.yml + scripts/
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
#
# Related:
#   - scripts/deploy-bootstrap.sh (first-apply path)
#   - documents/05-guides/deploy/post-bootstrap-deploy-runbook.md
#   - .claude/rules/release-deploy-standard.md §9
#   - GAP-506

set -euo pipefail

# --- Env guard: KITE_FIRST_APPLY must NOT be set for routine deploys (Bucket F AC F-AC1) ---
if [[ "${KITE_FIRST_APPLY:-false}" == "true" ]]; then
  echo "ERROR: deploy-prod.sh refusing to run with KITE_FIRST_APPLY=true." >&2
  echo "ERROR: For first-time bootstrap, use scripts/deploy-bootstrap.sh instead." >&2
  echo "ERROR: deploy-prod.sh is for routine deploys (post-bootstrap) only." >&2
  exit 2
fi

# Strip leading "v" prefix — Docker metadata-action `type=semver,pattern={{version}}`
# emits ECR tags WITHOUT v (e.g., `0.9.0-beta-staging.8`, not `v0.9.0-beta-staging.8`).
# User passes `KITE_VERSION=v0.9.0-beta-staging.8` for git tag UX consistency;
# we normalize to the no-v form for docker compose pull.
KITE_VERSION="${KITE_VERSION:-v0.9.0-beta-staging.8}"
KITE_VERSION="${KITE_VERSION#v}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/kite-prod}"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.production.yml"
LOG="${KITE_DEPLOY_LOG:-/var/log/kite-deploy.log}"

# Logging: write to stdout always; append to $LOG when writable (no-op otherwise).
# Avoids `sudo tee` so script stays testable in non-root contexts.
log() {
  local line
  line="[$(date -u +%FT%TZ)] $*"
  echo "$line"
  if [[ -w "$(dirname "$LOG")" ]] || [[ -w "$LOG" ]] 2>/dev/null; then
    echo "$line" >> "$LOG" 2>/dev/null || true
  fi
}

log "==================== deploy-prod.sh START ===================="
log "KITE_VERSION=${KITE_VERSION}"
log "DEPLOY_DIR=${DEPLOY_DIR}"

# --- Step 1: Verify bootstrap has happened (deploy-prod assumes /opt/kite-prod ready) ---
if [[ ! -d "$DEPLOY_DIR/.git" ]]; then
  log "ERROR: $DEPLOY_DIR/.git missing — bootstrap has not run."
  log "ERROR: Run scripts/deploy-bootstrap.sh ONCE first."
  log "ERROR: See documents/05-guides/deploy/bootstrap-runbook.md"
  exit 3
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  log "ERROR: $COMPOSE_FILE not found. Bootstrap state corrupted."
  log "ERROR: Inspect $DEPLOY_DIR manually; may need re-bootstrap."
  exit 4
fi

# Bootstrap clone via SSM ran as root, so .git ownership may be root.
# deploy-prod.sh runs as ec2-user via sudo; git refuses cross-uid ops without
# explicit safe.directory. Marking trusted for both root + ec2-user contexts.
sudo git config --global --add safe.directory "$DEPLOY_DIR" 2>/dev/null || true
git config --global --add safe.directory "$DEPLOY_DIR" 2>/dev/null || true

# --- Step 2: Pull / sync repo artifacts (post-bootstrap routine update) ---
cd "$DEPLOY_DIR"
log "Updating repo..."
# shellcheck disable=SC2015  # intentional: WARN-only fallback if fetch fails
sudo git -C "$DEPLOY_DIR" fetch --depth 1 origin main \
  && sudo git -C "$DEPLOY_DIR" reset --hard origin/main \
  || log "WARN: git pull failed, using existing artifacts"

# --- Step 3: ECR login (uses instance profile, no static keys) ---
log "ECR login..."
if [[ -x /etc/ecr-login.sh ]]; then
  sudo /etc/ecr-login.sh | tee -a "$LOG"
else
  log "WARN: /etc/ecr-login.sh missing — running inline"
  aws ecr get-login-password --region ap-southeast-1 \
    | sudo docker login --username AWS --password-stdin \
      906286017800.dkr.ecr.ap-southeast-1.amazonaws.com
fi

# --- Step 4: Fetch secrets from AWS Secrets Manager ---
log "Fetching secrets..."
if [[ -x "$DEPLOY_DIR/scripts/fetch-secrets.sh" ]]; then
  KITE_VERSION="$KITE_VERSION" sudo --preserve-env=KITE_VERSION,RESEND_API_KEY \
    bash "$DEPLOY_DIR/scripts/fetch-secrets.sh"
else
  log "ERROR: fetch-secrets.sh not found at $DEPLOY_DIR/scripts/"
  exit 5
fi

# --- Step 5: Pull latest images ---
log "docker compose pull (KITE_VERSION=$KITE_VERSION)..."
KITE_VERSION="$KITE_VERSION" sudo --preserve-env=KITE_VERSION \
  docker compose -f "$COMPOSE_FILE" pull

# --- Step 6: Up ---
log "docker compose up -d --remove-orphans..."
KITE_VERSION="$KITE_VERSION" sudo --preserve-env=KITE_VERSION \
  docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

# --- Step 6.5: Sync RabbitMQ user (GAP-502 RC1 self-heal) ---
#
# fetch-secrets.sh may generate ephemeral rabbit creds when
# kitehub/production/rabbitmq-default-creds secret is empty (see fetch-secrets.sh
# fallback block). Even when secret has stable values, rabbit container itself
# doesn't know about the kite_admin_* user — it only knows the default `user` +
# `guest`. Without this sync, Spring Boot services hit ACCESS_REFUSED on startup.
#
# Self-heal: after compose up + rabbit healthy, ensure rabbit has the user
# matching .env creds. add_user if missing; change_password if exists.
log "Sync rabbit user (GAP-502 RC1)..."
RMQ_USER=$(grep -E '^RABBITMQ_USER=' /etc/kite/.env | cut -d= -f2- | tr -d '"' || true)
RMQ_PASS=$(grep -E '^RABBITMQ_PASS=' /etc/kite/.env | cut -d= -f2- | tr -d '"' || true)
if [[ -n "${RMQ_USER:-}" && -n "${RMQ_PASS:-}" ]]; then
  # Wait up to 60s for rabbit to be reachable
  for i in 1 2 3 4 5 6; do
    if sudo docker exec kite-rabbitmq rabbitmqctl status >/dev/null 2>&1; then
      break
    fi
    log "Waiting for rabbit broker (attempt $i/6)..."
    sleep 10
  done
  if sudo docker exec kite-rabbitmq rabbitmqctl list_users 2>/dev/null | awk 'NR>1{print $1}' | grep -qx "$RMQ_USER"; then
    log "rabbit user '$RMQ_USER' exists — running change_password (idempotent)"
    sudo docker exec kite-rabbitmq rabbitmqctl change_password "$RMQ_USER" "$RMQ_PASS" 2>&1 | tee -a "$LOG"
  else
    log "rabbit user '$RMQ_USER' missing — add_user + set_permissions + admin tag"
    sudo docker exec kite-rabbitmq rabbitmqctl add_user "$RMQ_USER" "$RMQ_PASS" 2>&1 | tee -a "$LOG"
    sudo docker exec kite-rabbitmq rabbitmqctl set_permissions -p / "$RMQ_USER" ".*" ".*" ".*" 2>&1 | tee -a "$LOG"
    sudo docker exec kite-rabbitmq rabbitmqctl set_user_tags "$RMQ_USER" administrator 2>&1 | tee -a "$LOG"
  fi
  # Restart KH services so they pick up working auth (idempotent — services already running just reconnect)
  log "Restart kitehub-* to refresh rabbit connections..."
  KITE_VERSION="$KITE_VERSION" sudo --preserve-env=KITE_VERSION \
    docker compose -f "$COMPOSE_FILE" restart kitehub-subscription kitehub-gateway kitehub-admin kitehub-branding kitehub-email | tee -a "$LOG"
else
  log "WARN: RABBITMQ_USER/PASS not in /etc/kite/.env — skipping rabbit user sync"
fi

# --- Step 7: Wait + healthcheck ---
# Bucket F: extended wait honors kitehub-email cold start ~30s + Spring Boot
# warm-up window (start_period: 150s per docker-compose.production.yml).
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
