#!/usr/bin/env bash
#
# reboot-and-deploy-minimal.sh — recovery from hung EC2 + minimal deploy
#
# Per release-fix-retry-budget.md §3 STOP after 5+ retries on full deploy:
# pivot to recovery + minimal smoke test. Single decisive action.
#
# Steps:
#   1. Force reboot kh-backend EC2 (clean OOM/hung state)
#   2. Wait for SSM agent recovery (~3 min)
#   3. Stop any leftover containers + prune docker
#   4. Force-update repo to latest main
#   5. Deploy MINIMAL compose (redis + kitehub-subscription + kitehub-gateway)
#   6. Wait + verify ALB health check
#
# Why minimal:
#   - 5 KH services on t3.medium 4GB = OOM probability high
#   - Smoke test infra chain end-to-end with smallest scope first
#   - Add services back incrementally after gateway responds 200
#
# Usage: bash scripts/reboot-and-deploy-minimal.sh

set -euo pipefail

VERSION="${1:-0.9.0-beta-staging.8}"
VERSION="${VERSION#v}"  # strip v prefix
REGION="ap-southeast-1"
INSTANCE_ID="i-0b65c3947d36cae61"
ALB_DNS="kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com"
LOG_FILE="/tmp/kite-minimal-deploy-$(date +%Y%m%d-%H%M%S).log"

echo "==================================================================="
echo "Phase 7 RECOVERY: reboot + minimal compose deploy"
echo "==================================================================="
echo "Version:     $VERSION"
echo "Instance:    $INSTANCE_ID"
echo "Compose:     /opt/kite-prod/docker-compose.minimal.yml"
echo "Log:         $LOG_FILE"
echo "==================================================================="

# Verify creds
ACCT=$(aws sts get-caller-identity --query Account --output text 2>/dev/null || echo "")
if [[ "$ACCT" != "906286017800" ]]; then
  echo "ERROR: wrong AWS account (got '$ACCT')" >&2; exit 1
fi
echo "AWS identity OK: $ACCT"

# Step 1: Force reboot
echo
echo "──────────────────────────────────────────────────────────────────"
echo "[1/5] Force reboot EC2 (clean OOM/hung state)"
echo "──────────────────────────────────────────────────────────────────"
aws ec2 reboot-instances --instance-ids "$INSTANCE_ID" --region "$REGION"
echo "Reboot triggered. Waiting 90s for boot + SSM agent..."
sleep 90

# Step 2: Wait for SSM agent ready
echo
echo "──────────────────────────────────────────────────────────────────"
echo "[2/5] Waiting for SSM agent ready..."
echo "──────────────────────────────────────────────────────────────────"
for i in $(seq 1 24); do
  STATUS=$(aws ssm describe-instance-information \
    --filters "Key=InstanceIds,Values=$INSTANCE_ID" \
    --region "$REGION" \
    --query 'InstanceInformationList[0].PingStatus' \
    --output text 2>/dev/null || echo "Unknown")
  echo "  poll $i/24: SSM ping = $STATUS"
  if [[ "$STATUS" == "Online" ]]; then
    echo "SSM agent online ✅"
    break
  fi
  sleep 10
done

if [[ "$STATUS" != "Online" ]]; then
  echo "ERROR: SSM agent not online after 4 min. Check console." >&2; exit 1
fi

# Step 3-5: Clean + update + deploy minimal — single SSM command
echo
echo "──────────────────────────────────────────────────────────────────"
echo "[3-5/5] Clean + update + deploy minimal..."
echo "──────────────────────────────────────────────────────────────────"
CMD_ID=$(aws ssm send-command \
  --region "$REGION" \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --comment "Phase 7 minimal deploy" \
  --timeout-seconds 600 \
  --parameters "commands=[\
\"sudo docker stop \$(sudo docker ps -aq) 2>/dev/null || true\",\
\"sudo docker rm \$(sudo docker ps -aq) 2>/dev/null || true\",\
\"sudo docker system prune -af --volumes 2>&1 | tail -5\",\
\"sudo git config --global --add safe.directory /opt/kite-prod\",\
\"sudo git -C /opt/kite-prod fetch --depth 1 origin main\",\
\"sudo git -C /opt/kite-prod reset --hard origin/main\",\
\"sudo /etc/ecr-login.sh 2>&1 | tail -3\",\
\"sudo bash /opt/kite-prod/scripts/fetch-secrets.sh 2>&1 | tail -10\",\
\"cd /opt/kite-prod && sudo KITE_VERSION=$VERSION docker compose -f docker-compose.minimal.yml pull 2>&1 | tail -10\",\
\"cd /opt/kite-prod && sudo KITE_VERSION=$VERSION docker compose -f docker-compose.minimal.yml up -d --remove-orphans 2>&1 | tail -20\",\
\"sleep 90\",\
\"sudo docker compose -f /opt/kite-prod/docker-compose.minimal.yml ps\",\
\"echo === Gateway logs ===\",\
\"sudo docker logs kitehub-gateway --tail 30 2>&1 | tail -40\",\
\"echo === Local healthcheck ===\",\
\"curl -fsS -o /dev/null -w 'HTTP %{http_code}\\n' http://localhost:8080/actuator/health || echo 'gateway not yet healthy'\"\
]" \
  --query "Command.CommandId" --output text)

echo "CmdId: $CMD_ID"

# Poll up to 12 min (Java cold start + Flyway migration)
for i in $(seq 1 72); do
  sleep 10
  STATUS=$(aws ssm get-command-invocation \
    --command-id "$CMD_ID" \
    --instance-id "$INSTANCE_ID" \
    --region "$REGION" \
    --query 'Status' --output text 2>/dev/null || echo "InProgress")
  if [[ $((i % 6)) -eq 0 ]]; then
    echo "  poll $i/72: $STATUS"
  fi
  case "$STATUS" in
    Success|Failed|Cancelled|TimedOut) break ;;
  esac
done

echo
echo "==================================================================="
echo "Result"
echo "==================================================================="
aws ssm get-command-invocation \
  --command-id "$CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --region "$REGION" \
  --output json > "$LOG_FILE"

echo "STDOUT (last 80 lines):"
echo "---"
aws ssm get-command-invocation \
  --command-id "$CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --region "$REGION" \
  --query 'StandardOutputContent' --output text 2>&1 | tail -80
echo "---"

FINAL=$(aws ssm get-command-invocation \
  --command-id "$CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --region "$REGION" \
  --query 'Status' --output text)

echo
echo "==================================================================="
echo "SSM status: $FINAL"

if [[ "$FINAL" == "Success" ]]; then
  echo
  echo "Wait 60s + ALB health check..."
  sleep 60
  for i in 1 2 3; do
    HTTP=$(curl -fsS -o /dev/null -w "%{http_code}" "http://${ALB_DNS}/actuator/health" 2>&1 || echo "000")
    echo "  ALB attempt $i: HTTP $HTTP"
    if [[ "$HTTP" == "200" ]]; then
      echo
      echo "🎉 PHASE 7 MINIMAL DEPLOY SUCCESS"
      echo "Backend live: http://${ALB_DNS}/actuator/health"
      exit 0
    fi
    sleep 30
  done
  echo "⚠️  ALB not yet 200 — gateway may still be in cold start. Wait 2 more min + curl manually."
else
  echo "❌ FAILED — see log: $LOG_FILE"
  exit 1
fi
echo "==================================================================="
