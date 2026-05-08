#!/usr/bin/env bash
#
# deploy-via-local-ssm.sh — local-AWS-CLI deploy (bypass CI IAM gap)
#
# Run from local machine với aws CLI configured cho 906286017800.
#
# Per release-fix-retry-budget.md §3 STOP at retry #5 + §4 pivot:
# CI workflow `deploy-production.yml` blocked by missing ssm:SendCommand
# in kitehub-github-deploy role. Pivot: use local user creds (admin perms
# from terraform apply session) instead of CI OIDC role.
#
# Follow-up: GAP-445 add ssm:SendCommand to terraform iam.tf github_deploy_inline.
#
# Usage:
#   bash scripts/deploy-via-local-ssm.sh [version]
#
# Default version: v0.9.0-beta-staging.8

set -euo pipefail

VERSION="${1:-v0.9.0-beta-staging.8}"
REGION="ap-southeast-1"
INSTANCE_ID="i-0b65c3947d36cae61"
ALB_DNS="kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com"
LOG_FILE="/tmp/kite-deploy-$(date +%Y%m%d-%H%M%S).log"

echo "==================================================================="
echo "Phase 7 Production Deploy — local SSM path (CI IAM gap workaround)"
echo "==================================================================="
echo "Version:     $VERSION"
echo "Instance:    $INSTANCE_ID"
echo "ALB:         $ALB_DNS"
echo "Log:         $LOG_FILE"
echo "==================================================================="

# Verify AWS creds
if ! command -v aws >/dev/null 2>&1; then
  echo "ERROR: aws CLI not installed" >&2
  exit 1
fi
ACCT=$(aws sts get-caller-identity --query Account --output text 2>/dev/null || echo "")
if [[ "$ACCT" != "906286017800" ]]; then
  echo "ERROR: wrong AWS account (expected 906286017800, got '$ACCT')" >&2
  exit 1
fi
echo "AWS identity OK: $ACCT"
echo

# Send deploy command — single line, calls deploy-prod.sh on EC2
echo "Sending deploy command (KITE_VERSION=$VERSION)..."
CMD_ID=$(aws ssm send-command \
  --region "$REGION" \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --comment "Deploy $VERSION" \
  --timeout-seconds 600 \
  --parameters "commands=[\"sudo KITE_VERSION=$VERSION bash /opt/kite-prod/scripts/deploy-prod.sh 2>&1 | tail -200\"]" \
  --query "Command.CommandId" --output text)

echo "CommandId: $CMD_ID"

# Poll status (8 min max — Java services slow to start)
echo
echo "Polling status (max 8 min)..."
for i in $(seq 1 48); do
  sleep 10
  STATUS=$(aws ssm get-command-invocation \
    --region "$REGION" \
    --command-id "$CMD_ID" \
    --instance-id "$INSTANCE_ID" \
    --query "Status" --output text 2>/dev/null || echo "InProgress")
  echo "  poll $i/48: $STATUS"
  case "$STATUS" in
    Success|Failed|Cancelled|TimedOut) break ;;
  esac
done

# Capture full result
echo
echo "==================================================================="
echo "Result"
echo "==================================================================="
aws ssm get-command-invocation \
  --region "$REGION" \
  --command-id "$CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --output json > "$LOG_FILE"

# Pretty stdout/stderr
echo "STDOUT (last 100 lines):"
echo "---"
aws ssm get-command-invocation \
  --region "$REGION" \
  --command-id "$CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --query "StandardOutputContent" --output text | tail -100
echo "---"

echo
echo "STDERR:"
echo "---"
aws ssm get-command-invocation \
  --region "$REGION" \
  --command-id "$CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --query "StandardErrorContent" --output text
echo "---"

# Final verdict
FINAL_STATUS=$(aws ssm get-command-invocation \
  --region "$REGION" \
  --command-id "$CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --query "Status" --output text)

if [[ "$FINAL_STATUS" != "Success" ]]; then
  echo
  echo "==================================================================="
  echo "❌ DEPLOY FAILED — status: $FINAL_STATUS"
  echo "Full log: $LOG_FILE"
  echo "==================================================================="
  exit 1
fi

# Wait + ALB health check
echo
echo "==================================================================="
echo "Waiting 90s for ALB target group health..."
echo "==================================================================="
sleep 90

echo
echo "ALB health check:"
for i in 1 2 3; do
  HTTP=$(curl -fsS -o /dev/null -w "%{http_code}" "http://${ALB_DNS}/actuator/health" 2>&1 || echo "000")
  echo "  attempt $i: HTTP $HTTP"
  if [[ "$HTTP" == "200" ]]; then
    echo
    echo "==================================================================="
    echo "🎉 DEPLOY SUCCESS — backend live at:"
    echo "  http://${ALB_DNS}/actuator/health (200 OK)"
    echo
    echo "Container status (via SSM):"
    aws ssm send-command \
      --region "$REGION" \
      --instance-ids "$INSTANCE_ID" \
      --document-name AWS-RunShellScript \
      --comment "Verify containers" \
      --parameters 'commands=["sudo docker ps --format \"table {{.Names}}\\t{{.Status}}\""]' \
      --query "Command.CommandId" --output text
    echo "==================================================================="
    exit 0
  fi
  sleep 30
done

echo
echo "==================================================================="
echo "⚠️  ALB not yet healthy (3 attempts). Container may still be starting."
echo "Check: docker compose logs on EC2 OR re-curl ALB in 2 min."
echo "Log: $LOG_FILE"
echo "==================================================================="
exit 1
