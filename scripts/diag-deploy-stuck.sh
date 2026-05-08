#!/usr/bin/env bash
#
# diag-deploy-stuck.sh — diagnose hung/failed Phase 7 deploy
#
# Run when deploy-via-local-ssm.sh times out OR returns InProgress.
#
# Usage:
#   bash scripts/diag-deploy-stuck.sh                        # default deploy CmdId
#   bash scripts/diag-deploy-stuck.sh <command-id>           # specific deploy CmdId

set -euo pipefail

REGION="ap-southeast-1"
INSTANCE_ID="i-0b65c3947d36cae61"
DEPLOY_CMD_ID="${1:-44f845e4-6dc5-4fd4-832c-621eeb7ccf80}"
LOG_FILE="/tmp/kite-diag-$(date +%Y%m%d-%H%M%S).log"

echo "==================================================================="
echo "Phase 7 Deploy Diagnostic"
echo "==================================================================="
echo "Deploy CmdId:  $DEPLOY_CMD_ID"
echo "Instance:      $INSTANCE_ID"
echo "Log:           $LOG_FILE"
echo "==================================================================="

# Verify aws creds
ACCT=$(aws sts get-caller-identity --query Account --output text 2>/dev/null || echo "")
if [[ "$ACCT" != "906286017800" ]]; then
  echo "ERROR: wrong AWS account (expected 906286017800, got '$ACCT')" >&2
  exit 1
fi

# Step 1: Check current status of original deploy SSM command
echo
echo "──────────────────────────────────────────────────────────────────"
echo "[1/3] Original deploy command status"
echo "──────────────────────────────────────────────────────────────────"
DEPLOY_STATUS=$(aws ssm get-command-invocation \
  --command-id "$DEPLOY_CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --region "$REGION" \
  --query 'Status' --output text 2>&1 || echo "QueryFailed")

echo "Status: $DEPLOY_STATUS"

if [[ "$DEPLOY_STATUS" == "Success" || "$DEPLOY_STATUS" == "Failed" ]]; then
  echo
  echo "Stdout (last 50 lines):"
  echo "---"
  aws ssm get-command-invocation \
    --command-id "$DEPLOY_CMD_ID" \
    --instance-id "$INSTANCE_ID" \
    --region "$REGION" \
    --query 'StandardOutputContent' --output text 2>&1 | tail -50
  echo "---"
  echo "Stderr:"
  echo "---"
  aws ssm get-command-invocation \
    --command-id "$DEPLOY_CMD_ID" \
    --instance-id "$INSTANCE_ID" \
    --region "$REGION" \
    --query 'StandardErrorContent' --output text 2>&1
  echo "---"
fi

# Step 2: Send diagnostic command to inspect containers + logs
echo
echo "──────────────────────────────────────────────────────────────────"
echo "[2/3] Sending diagnostic command (containers + logs)"
echo "──────────────────────────────────────────────────────────────────"
DIAG_CMD=$(aws ssm send-command \
  --region "$REGION" \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --comment "Phase 7 deploy diagnostic" \
  --timeout-seconds 120 \
  --parameters 'commands=[
"echo === DOCKER PS ===",
"sudo docker ps -a --format \"table {{.Names}}\\t{{.Status}}\\t{{.Ports}}\"",
"echo",
"echo === COMPOSE PS ===",
"sudo docker compose -f /opt/kite-prod/docker-compose.production.yml ps 2>&1 || true",
"echo",
"echo === REDIS LOGS ===",
"sudo docker logs kite-redis --tail 30 2>&1 | tail -40 || true",
"echo",
"echo === RABBITMQ LOGS ===",
"sudo docker logs kite-rabbitmq --tail 20 2>&1 | tail -25 || true",
"echo",
"echo === GATEWAY LOGS ===",
"sudo docker logs kitehub-gateway --tail 50 2>&1 | tail -60 || true",
"echo",
"echo === SUBSCRIPTION LOGS ===",
"sudo docker logs kitehub-subscription --tail 30 2>&1 | tail -40 || true",
"echo",
"echo === ENV FILE PRESENCE ===",
"sudo ls -la /etc/kite/.env",
"echo",
"echo === DEPLOY LOG TAIL ===",
"sudo tail -50 /var/log/kite-deploy.log 2>&1 || true"
]' \
  --query "Command.CommandId" --output text)
echo "Diag CmdId: $DIAG_CMD"

# Step 3: Poll diag status (max 2 min)
echo
echo "──────────────────────────────────────────────────────────────────"
echo "[3/3] Waiting for diag output..."
echo "──────────────────────────────────────────────────────────────────"
for i in $(seq 1 12); do
  sleep 10
  DIAG_STATUS=$(aws ssm get-command-invocation \
    --command-id "$DIAG_CMD" \
    --instance-id "$INSTANCE_ID" \
    --region "$REGION" \
    --query 'Status' --output text 2>/dev/null || echo "InProgress")
  echo "  poll $i/12: $DIAG_STATUS"
  case "$DIAG_STATUS" in
    Success|Failed|Cancelled|TimedOut) break ;;
  esac
done

echo
echo "==================================================================="
echo "DIAG OUTPUT"
echo "==================================================================="
aws ssm get-command-invocation \
  --command-id "$DIAG_CMD" \
  --instance-id "$INSTANCE_ID" \
  --region "$REGION" \
  --query 'StandardOutputContent' --output text 2>&1 | tee "$LOG_FILE"

echo
echo "==================================================================="
echo "Log saved: $LOG_FILE"
echo "==================================================================="
