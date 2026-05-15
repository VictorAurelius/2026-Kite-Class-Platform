#!/usr/bin/env bash
# stop-stack.sh — Stop Phase 1 BETA AWS stack (2× EC2 + RDS) when idle
#
# Per .claude/rules/agent-aws-access.md §4: agent KHÔNG run autonomously.
# User executes this script manually when stack idle (>30 min, end of demo, EOD).
#
# Usage:
#   bash scripts/aws/stop-stack.sh                  # Real run, 60s grace warning
#   bash scripts/aws/stop-stack.sh --dry-run        # Print actions, no AWS calls
#   bash scripts/aws/stop-stack.sh --force          # Skip 60s grace warning
#
# Exit codes:
#   0 — All resources stopped
#   1 — Timeout (>5 min total wall-clock)
#   2 — Partial (1+ instance failed to stop)
#   3 — AWS CLI / credentials missing
#
# Stack resources (matches start-stack.sh):
#   EC2: instances tagged Name=kitehub-kh-backend + kitehub-kc-app
#        (dynamic lookup — survives EC2 replacement per GAP-492)
#   RDS: kitehub-postgres

set -euo pipefail

# ─────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────
RDS_DB_IDENTIFIER="kitehub-postgres"
AWS_REGION="${AWS_REGION:-ap-southeast-1}"
AWS_PROFILE_STOP="${AWS_PROFILE_STOP:-${AWS_PROFILE:-dev-admin}}"

# Dynamic EC2 instance ID lookup by tag (GAP-492 fix — survives AMI bump replacement).
# Filters: Name tag matches stack + state running OR stopped (skip terminated).
# Output: space-separated InstanceId list ordered by Name tag.
lookup_ec2_instance_ids() {
  AWS_PROFILE="${AWS_PROFILE_STOP}" aws ec2 describe-instances \
    --region "${AWS_REGION}" \
    --filters \
      "Name=tag:Name,Values=kitehub-kh-backend,kitehub-kc-app,kitehub-kc-app-fe" \
      "Name=instance-state-name,Values=running,stopped" \
    --query 'Reservations[].Instances[].InstanceId' \
    --output text 2>/dev/null
}

mapfile -t EC2_INSTANCE_IDS < <(lookup_ec2_instance_ids | tr '\t' '\n' | grep -v '^$')
if [ "${#EC2_INSTANCE_IDS[@]}" -eq 0 ]; then
  echo "::error :: No kitehub-kh-backend or kitehub-kc-app instances found via tag lookup"
  echo "Check: AWS_PROFILE=${AWS_PROFILE_STOP} aws ec2 describe-instances --filters 'Name=tag:Name,Values=kitehub-*'"
  exit 3
fi
STATE_FILE="${STATE_FILE:-.aws-stack-state.json}"
GRACE_SECONDS=60
TIMEOUT_SECONDS=300  # 5 min total budget
POLL_INTERVAL=10

DRY_RUN=false
FORCE=false

# ─────────────────────────────────────────────────────────────────
# Argument parsing
# ─────────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=true; shift ;;
    --force) FORCE=true; shift ;;
    --help|-h)
      sed -n '1,25p' "$0"
      exit 0
      ;;
    *)
      echo "ERROR: unknown arg '$1'. See --help." >&2
      exit 3
      ;;
  esac
done

# ─────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────
log() {
  printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"
}

require_aws_cli() {
  if ! command -v aws >/dev/null 2>&1; then
    log "ERROR: aws CLI not found in PATH"
    exit 3
  fi
  if [[ "$DRY_RUN" == "false" ]]; then
    if ! aws sts get-caller-identity --region "$AWS_REGION" >/dev/null 2>&1; then
      log "ERROR: AWS credentials not configured"
      exit 3
    fi
  fi
}

grace_warning() {
  if [[ "$FORCE" == "true" || "$DRY_RUN" == "true" ]]; then
    log "Skipping ${GRACE_SECONDS}s grace (--force or --dry-run)"
    return 0
  fi
  log "Stopping stack in ${GRACE_SECONDS}s. Press Ctrl-C to abort."
  log "  EC2: ${EC2_INSTANCE_IDS[*]}"
  log "  RDS: $RDS_DB_IDENTIFIER"
  sleep "$GRACE_SECONDS"
}

update_state_entry() {
  local stop_time="$1"
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: would close latest open session in $STATE_FILE"
    return 0
  fi
  if [[ ! -f "$STATE_FILE" ]]; then
    log "WARN: $STATE_FILE not found — creating fresh ledger with stop-only entry"
    echo '{"sessions":[]}' > "$STATE_FILE"
  fi
  python3 - "$STATE_FILE" "$stop_time" <<'PY'
import json, sys, datetime
state_file, stop_time = sys.argv[1:3]
with open(state_file) as f:
    data = json.load(f)
sessions = data.get('sessions', [])
# Find latest session without stop_time
target = None
for s in reversed(sessions):
    if s.get('stop_time') is None:
        target = s
        break
if target is None:
    # No matching start — record orphan stop
    sessions.append({
        'start_time': None,
        'stop_time': stop_time,
        'duration_minutes': None,
        'reason': 'orphan-stop (no matching start)',
        'status': 'stopped-orphan',
    })
else:
    target['stop_time'] = stop_time
    if target.get('start_time'):
        start = datetime.datetime.fromisoformat(target['start_time'].replace('Z','+00:00'))
        stop = datetime.datetime.fromisoformat(stop_time.replace('Z','+00:00'))
        target['duration_minutes'] = round((stop - start).total_seconds() / 60, 1)
    target['status'] = 'stopped'
data['sessions'] = sessions
with open(state_file, 'w') as f:
    json.dump(data, f, indent=2)
PY
  log "Session entry closed in $STATE_FILE"
}

run_aws() {
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: aws $*"
    return 0
  fi
  aws "$@"
}

# ─────────────────────────────────────────────────────────────────
# EC2 stop + wait
# ─────────────────────────────────────────────────────────────────
stop_ec2() {
  log "Stopping EC2 instances: ${EC2_INSTANCE_IDS[*]}"
  run_aws ec2 stop-instances \
    --instance-ids "${EC2_INSTANCE_IDS[@]}" \
    --region "$AWS_REGION" \
    --output text >/dev/null
}

wait_ec2_stopped() {
  log "Waiting for EC2 instances to reach 'stopped' state..."
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: skipping wait"
    return 0
  fi
  aws ec2 wait instance-stopped \
    --instance-ids "${EC2_INSTANCE_IDS[@]}" \
    --region "$AWS_REGION"
  log "All EC2 instances STOPPED"
}

# ─────────────────────────────────────────────────────────────────
# RDS stop + wait
# ─────────────────────────────────────────────────────────────────
stop_rds() {
  log "Stopping RDS instance: $RDS_DB_IDENTIFIER"
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: aws rds stop-db-instance --db-instance-identifier $RDS_DB_IDENTIFIER"
    return 0
  fi
  if ! aws rds stop-db-instance \
        --db-instance-identifier "$RDS_DB_IDENTIFIER" \
        --region "$AWS_REGION" \
        --output text >/dev/null 2>&1; then
    local current
    current=$(aws rds describe-db-instances \
      --db-instance-identifier "$RDS_DB_IDENTIFIER" \
      --region "$AWS_REGION" \
      --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "unknown")
    log "  RDS in state '$current' (already stopping/stopped or transient state)"
  fi
}

wait_rds_stopped() {
  log "Waiting for RDS to reach 'stopped' state (this can take 2-3 minutes)..."
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: skipping RDS wait"
    return 0
  fi
  local elapsed=0
  while (( elapsed < TIMEOUT_SECONDS )); do
    local status
    status=$(aws rds describe-db-instances \
      --db-instance-identifier "$RDS_DB_IDENTIFIER" \
      --region "$AWS_REGION" \
      --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "unknown")
    if [[ "$status" == "stopped" ]]; then
      log "RDS STOPPED"
      return 0
    fi
    log "  RDS status: $status"
    sleep "$POLL_INTERVAL"
    elapsed=$((elapsed + POLL_INTERVAL))
  done
  log "ERROR: RDS did not reach 'stopped' within $TIMEOUT_SECONDS s"
  return 1
}

# ─────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────
main() {
  local script_start_epoch
  script_start_epoch=$(date +%s)
  log "=== Stop stack ==="
  log "  Region: $AWS_REGION"
  log "  EC2:    ${EC2_INSTANCE_IDS[*]}"
  log "  RDS:    $RDS_DB_IDENTIFIER"
  log "  Mode:   $([[ "$DRY_RUN" == "true" ]] && echo "DRY-RUN" || echo "REAL")"

  require_aws_cli
  grace_warning

  stop_ec2
  stop_rds

  local partial=false
  wait_ec2_stopped || partial=true
  wait_rds_stopped || partial=true

  local stop_time
  stop_time=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  update_state_entry "$stop_time"

  if [[ "$partial" == "true" ]]; then
    log "WARN: 1+ resource did not reach stopped state cleanly"
    exit 2
  fi

  log "=== Stack STOPPED ($(( $(date +%s) - script_start_epoch )) s elapsed) ==="
  log ""
  log "Cost impact: EC2 + RDS compute charges paused; storage (EBS + RDS) continues (~\$3-5/mo)"
  log "Session ledger: $STATE_FILE"
}

main "$@"
