#!/usr/bin/env bash
#
# seed-prod-ec2.sh — Wave 67 production data seed via existing container restart
#
# Runs ON kh-backend EC2 via SSM send-command. Idempotent (ProductionSeedRunner
# uses INSERT ON CONFLICT DO NOTHING per Wave 33).
#
# Strategy: inject KITE_SEED_MODE=production into /etc/kite/.env → restart
# kitehub-subscription → wait for "seed complete" log marker → remove env vars
# to prevent re-seed on next routine restart.
#
# Per .claude/rules/agent-aws-access.md §4 + release-deploy-standard.md §9 —
# user-triggered via workflow_dispatch confirm=SEED (NOT agent-autonomous).
#
# Sister of deploy-prod.sh. Same SSM bootstrap pattern (EC2 user_data clones
# /opt/kite-prod on boot per GAP-483).

set -euo pipefail

REGION="ap-southeast-1"
ENV_FILE="/etc/kite/.env"
COMPOSE_FILE="/opt/kite-prod/docker-compose.production.yml"
LOG="${LOG:-/var/log/kite-seed.log}"
SERVICE="kitehub-subscription"
SEED_TIMEOUT_S="${SEED_TIMEOUT_S:-180}"
SEED_ADMIN_EMAIL="${SEED_ADMIN_EMAIL:-admin@kitehub.me}"
COMPLETE_MARKER="ProductionSeedRunner: seed complete"
ERROR_MARKER='ProductionSeedRunner.*ERROR\|SEED FAILED\|kite\.seed.*BUILD FAILED'

log() { echo "[$(date -u +%FT%TZ)] $*" | sudo tee -a "$LOG"; }

log "==================== seed-prod-ec2.sh START ===================="

# 1. Prerequisite: /etc/kite/.env must exist (deploy-prod.sh / fetch-secrets.sh ran)
if [[ ! -f "$ENV_FILE" ]]; then
  log "ERROR: $ENV_FILE missing — run deploy first (fetch-secrets.sh populates it)"
  exit 1
fi

# 2. Fetch seed-admin-password from Secrets Manager (per GAP-499 provisioning)
log "Fetching seed-admin-password..."
SEED_ADMIN_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id kitehub/production/seed-admin-password \
  --region "$REGION" --query SecretString --output text 2>/dev/null) || {
    log "ERROR: failed to fetch kitehub/production/seed-admin-password — provisioned by terraform?"
    exit 1
  }
[[ -z "$SEED_ADMIN_PASSWORD" || "$SEED_ADMIN_PASSWORD" == "null" ]] && {
  log "ERROR: seed-admin-password empty"
  exit 1
}
log "seed-admin-password fetched (length=${#SEED_ADMIN_PASSWORD})"
log "seed admin email: $SEED_ADMIN_EMAIL"

# 3. Inject seed env vars (append; idempotent re-run safe via sed cleanup below)
log "Injecting KITE_SEED_* into $ENV_FILE..."
sudo sed -i '/^KITE_SEED_/d' "$ENV_FILE"  # strip any stale entries first
sudo tee -a "$ENV_FILE" > /dev/null <<EOF
KITE_SEED_MODE=production
KITE_SEED_ADMIN_EMAIL=$SEED_ADMIN_EMAIL
KITE_SEED_ADMIN_PASSWORD=$SEED_ADMIN_PASSWORD
EOF

# 4. Capture pre-restart log baseline (marker may pre-exist from prior seed)
PRE_RESTART_TS=$(date -u +%s)
log "Pre-restart timestamp: $PRE_RESTART_TS"

# 5. Recreate kitehub-subscription (force re-read /etc/kite/.env env_file)
# NOTE: `docker compose restart` does NOT reload env_file — container keeps
# stale env from original `up`. `up -d --no-deps --force-recreate` recreates
# the service with fresh env from /etc/kite/.env (where we just appended
# KITE_SEED_*). --no-deps avoids restarting redis/rabbitmq dependencies.
# --env-file: docker compose uses this file for variable substitution in
# compose YAML (${KITE_VERSION} → image tag, ${RABBITMQ_USER} → service env).
# Without it, compose warns + interpolates empty strings → invalid image ref.
# /etc/kite/.env is the canonical source per fetch-secrets.sh contract.
log "Recreating $SERVICE (force re-read /etc/kite/.env; Spring cold start ~60s)..."
sudo docker compose --env-file /etc/kite/.env -f "$COMPOSE_FILE" up -d --no-deps --force-recreate "$SERVICE"

# 6. Wait for seed completion marker (only logs POST-restart count)
log "Polling for marker '$COMPLETE_MARKER' (timeout ${SEED_TIMEOUT_S}s)..."
ELAPSED=0
INTERVAL=5
FOUND=0
while (( ELAPSED < SEED_TIMEOUT_S )); do
  sleep "$INTERVAL"
  ELAPSED=$((ELAPSED + INTERVAL))

  # Get logs since the restart point (use --since with timestamp)
  RECENT_LOGS=$(sudo docker logs --since "$PRE_RESTART_TS" "$SERVICE" 2>&1 || true)

  if echo "$RECENT_LOGS" | grep -q "$COMPLETE_MARKER"; then
    log "✅ Seed completion marker found at ${ELAPSED}s"
    FOUND=1
    break
  fi
  if echo "$RECENT_LOGS" | grep -qE "$ERROR_MARKER"; then
    log "❌ Seed error marker detected at ${ELAPSED}s"
    echo "$RECENT_LOGS" | tail -50 | sudo tee -a "$LOG"
    FOUND=-1
    break
  fi
  log "  ...${ELAPSED}s elapsed, no marker yet"
done

# 7. Cleanup: strip KITE_SEED_* from /etc/kite/.env (prevent re-seed next restart)
log "Cleaning KITE_SEED_* from $ENV_FILE..."
sudo sed -i '/^KITE_SEED_/d' "$ENV_FILE"

# 8. Final outcome
if (( FOUND == 1 )); then
  log "Surfacing seed log excerpt:"
  echo "$RECENT_LOGS" | grep "ProductionSeedRunner" | sudo tee -a "$LOG"
  log "==================== seed-prod-ec2.sh DONE ===================="
  exit 0
elif (( FOUND == -1 )); then
  log "==================== seed-prod-ec2.sh FAILED (error marker) ===================="
  exit 1
else
  log "❌ Timeout ${SEED_TIMEOUT_S}s — no completion marker found"
  log "Last 30 lines of $SERVICE logs:"
  sudo docker logs --tail 30 "$SERVICE" 2>&1 | sudo tee -a "$LOG"
  log "==================== seed-prod-ec2.sh FAILED (timeout) ===================="
  exit 1
fi
