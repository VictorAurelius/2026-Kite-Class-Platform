#!/usr/bin/env bash
#
# deploy-fe.sh — Phase 1 BETA FE production deploy on kc-app-fe EC2
#
# Wave 89 Bucket B (GAP-602 + GAP-603) — defensive PM2 invocation:
#   - Uses explicit `--cwd` flag (PM2 picks ecosystem config absolute path)
#   - Pairs với `pm2 save` post-start để persist process list cho systemd resurrect
#   - Assumes `pm2 startup systemd` đã được wire bởi ec2-kc-app.tf user_data
#     (GAP-603); nếu chưa, error message guide user re-run user_data.
#
# Runs ON kc-app-fe EC2 (instance ID resolve qua `bash scripts/aws/start-stack.sh`
# tag lookup OR direct via SSM SendCommand). NOT cho agent execution per
# agent-aws-access.md §4.3 — production EC2 mutation = user-only.
#
# Idempotent: re-run safe. PM2 `start` no-op nếu process đã online; PM2 `reload`
# zero-downtime restart (preferred khi rsync artifact mới).
#
# Usage on EC2:
#   sudo -u ec2-user bash scripts/deploy-fe.sh [start|reload|status]
#
# Usage via SSM (operator local):
#   aws ssm send-command \
#     --instance-ids <kc-app-fe-id> \
#     --document-name AWS-RunShellScript \
#     --parameters 'commands=["sudo -u ec2-user bash /opt/kite-fe/scripts/deploy-fe.sh reload"]' \
#     --timeout-seconds 300 \
#     --region ap-southeast-1
#
# Related:
#   - infrastructure/fe-host/pm2-ecosystem.config.js (GAP-602 cwd fix)
#   - infrastructure/terraform-aws/ec2-kc-app.tf user_data (GAP-603 systemd wire)
#   - documents/05-guides/deploy/pm2-systemd-auto-start.md (runbook)
#   - documents/05-guides/deploy/fe-self-host-runbook.md (Wave 82 parent)

set -euo pipefail

ACTION="${1:-status}"
ECOSYSTEM_CONFIG="${ECOSYSTEM_CONFIG:-/var/www/pm2-ecosystem.config.js}"
LOG="${KITE_FE_DEPLOY_LOG:-/var/log/kite-fe-deploy.log}"

log() {
  local line
  line="[$(date -u +%FT%TZ)] $*"
  echo "$line"
  if [[ -w "$(dirname "$LOG")" ]] || [[ -w "$LOG" ]] 2>/dev/null; then
    echo "$line" >> "$LOG" 2>/dev/null || true
  fi
}

# --- Pre-flight: verify ecosystem config tồn tại ---
if [[ ! -f "$ECOSYSTEM_CONFIG" ]]; then
  log "ERROR: $ECOSYSTEM_CONFIG missing."
  log "ERROR: Copy infrastructure/fe-host/pm2-ecosystem.config.js -> $ECOSYSTEM_CONFIG first."
  exit 2
fi

# --- Pre-flight: verify PM2 systemd unit exists (GAP-603) ---
if ! systemctl list-unit-files pm2-ec2-user.service >/dev/null 2>&1; then
  log "WARN: pm2-ec2-user.service NOT installed."
  log "WARN: GAP-603 fix requires user_data re-run OR manual:"
  log "WARN:   sudo -u ec2-user pm2 startup systemd -u ec2-user --hp /home/ec2-user | grep '^sudo ' | sh"
  log "WARN: Without systemd unit, PM2 will NOT auto-restart on reboot."
fi

case "$ACTION" in
  start)
    log "==================== deploy-fe.sh START ===================="
    log "Starting PM2 với explicit ecosystem config absolute path (GAP-602 defensive)..."
    # `--update-env` re-read env vars từ config (PORT, NODE_OPTIONS...).
    # PM2 internally uses ecosystem `cwd` field; explicit absolute config path
    # ensures pickup correct file regardless of current shell working dir.
    pm2 start "$ECOSYSTEM_CONFIG" --update-env 2>&1 | tee -a "$LOG"
    log "Saving process list để systemd resurrect on reboot..."
    pm2 save 2>&1 | tee -a "$LOG"
    pm2 list | tee -a "$LOG"
    log "==================== deploy-fe.sh OK ===================="
    ;;

  reload)
    log "==================== deploy-fe.sh RELOAD ===================="
    log "Zero-downtime reload (rsync artifact mới rồi reload)..."
    pm2 reload "$ECOSYSTEM_CONFIG" --update-env 2>&1 | tee -a "$LOG"
    pm2 save 2>&1 | tee -a "$LOG"
    pm2 list | tee -a "$LOG"
    log "==================== deploy-fe.sh OK ===================="
    ;;

  status)
    log "PM2 process list:"
    pm2 list | tee -a "$LOG"
    log "PM2 systemd unit:"
    systemctl status pm2-ec2-user.service --no-pager 2>&1 | head -20 | tee -a "$LOG" || true
    ;;

  *)
    echo "Usage: $0 [start|reload|status]" >&2
    exit 1
    ;;
esac
