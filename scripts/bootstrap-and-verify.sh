#!/usr/bin/env bash
#
# bootstrap-and-verify.sh — one-shot Phase 7 EC2 bootstrap + status check
#
# Run from local machine với aws CLI configured cho account 906286017800.
# NO SessionManagerPlugin needed (uses send-command, not start-session).
#
# Usage:
#   bash scripts/bootstrap-and-verify.sh
#
# What it does:
#   1. ssm send-command vào kh-backend EC2 — install git+jq + clone repo to /opt/kite-prod
#   2. Poll command status mỗi 10s, max 5 phút
#   3. Print stdout/stderr khi xong
#   4. Save full log vào /tmp/kite-bootstrap-$(date).log
#
# Per .claude/rules/agent-aws-access.md §4.1: ssm:SendCommand = Tier 3 mutation.
# User-execute only. This script ships from agent for user copy-paste convenience.

set -euo pipefail

REGION="ap-southeast-1"
INSTANCE_ID="i-0b65c3947d36cae61"
REPO_URL="https://github.com/VictorAurelius/2026-Kite-Class-Platform.git"
LOG_FILE="/tmp/kite-bootstrap-$(date +%Y%m%d-%H%M%S).log"

echo "==================================================================="
echo "Phase 7 EC2 Bootstrap"
echo "==================================================================="
echo "Account:     906286017800"
echo "Region:      $REGION"
echo "Instance:    $INSTANCE_ID (kh-backend)"
echo "Repo:        $REPO_URL"
echo "Log:         $LOG_FILE"
echo "==================================================================="

# Verify AWS CLI + creds
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

# Send the bootstrap command
echo "Sending SSM bootstrap command..."
CMD_ID=$(aws ssm send-command \
  --region "$REGION" \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --comment "Phase 7 bootstrap" \
  --timeout-seconds 300 \
  --parameters "commands=[\
\"sudo dnf install -y git jq\",\
\"sudo mkdir -p /opt/kite-prod\",\
\"sudo chown ec2-user:ec2-user /opt/kite-prod\",\
\"cd /opt/kite-prod && (git rev-parse --git-dir > /dev/null 2>&1 && git fetch origin main && git reset --hard origin/main || git clone --depth 1 ${REPO_URL} .)\",\
\"ls -la /opt/kite-prod/scripts/\",\
\"echo Bootstrap-complete-at-\\$(date -u +%FT%TZ)\"\
]" \
  --query "Command.CommandId" --output text)

echo "CommandId: $CMD_ID"
echo "$CMD_ID" > /tmp/kite-bootstrap-cmd-id

# Poll status
echo
echo "Polling status (max 5 min)..."
for i in $(seq 1 30); do
  sleep 10
  STATUS=$(aws ssm get-command-invocation \
    --region "$REGION" \
    --command-id "$CMD_ID" \
    --instance-id "$INSTANCE_ID" \
    --query "Status" --output text 2>/dev/null || echo "InProgress")
  echo "  poll $i/30: $STATUS"
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
  --output json | tee "$LOG_FILE"

# Pretty-print stdout/stderr
echo
echo "==================================================================="
echo "STDOUT"
echo "==================================================================="
aws ssm get-command-invocation \
  --region "$REGION" \
  --command-id "$CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --query "StandardOutputContent" --output text

echo
echo "==================================================================="
echo "STDERR"
echo "==================================================================="
aws ssm get-command-invocation \
  --region "$REGION" \
  --command-id "$CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --query "StandardErrorContent" --output text

# Final verdict
FINAL_STATUS=$(aws ssm get-command-invocation \
  --region "$REGION" \
  --command-id "$CMD_ID" \
  --instance-id "$INSTANCE_ID" \
  --query "Status" --output text)

echo
echo "==================================================================="
if [[ "$FINAL_STATUS" == "Success" ]]; then
  echo "✅ BOOTSTRAP SUCCESS — /opt/kite-prod ready"
  echo "Next: trigger CI deploy:"
  echo "  gh workflow run deploy-production.yml -f version=v0.9.0-beta-staging.8 -f confirm=DEPLOY"
else
  echo "❌ BOOTSTRAP FAILED — status: $FINAL_STATUS"
  echo "Full log saved: $LOG_FILE"
  exit 1
fi
echo "==================================================================="
