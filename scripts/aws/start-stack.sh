#!/usr/bin/env bash
# start-stack.sh — Resume Phase 1 BETA AWS stack (2× EC2 + RDS) on-demand
#
# Per .claude/rules/agent-aws-access.md §4: agent KHÔNG run autonomously.
# User executes this script manually when needed (demo, tenant session, smoke test).
#
# Usage:
#   bash scripts/aws/start-stack.sh                 # Real run
#   bash scripts/aws/start-stack.sh --dry-run       # Print actions, no AWS calls
#   bash scripts/aws/start-stack.sh --reason "demo Tenant X 2026-05-12"
#
# Exit codes:
#   0 — All resources healthy
#   1 — Timeout (>10 min total wall-clock)
#   2 — Partial (1+ instance failed to reach healthy state)
#   3 — AWS CLI / credentials missing
#
# Stack resources (Phase 1 BETA, account 906286017800, region ap-southeast-1):
#   EC2: instances tagged Name=kitehub-kh-backend + kitehub-kc-app
#        (dynamic lookup — survives EC2 replacement per GAP-492)
#   RDS: kitehub-postgres
#   ALB: kitehub-alb (ALWAYS-ON, Free Tier; not managed here)

set -euo pipefail

# ─────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────
EC2_INSTANCE_NAMES=("kitehub-kh-backend" "kitehub-kc-app")
RDS_DB_IDENTIFIER="kitehub-postgres"
AWS_REGION="${AWS_REGION:-ap-southeast-1}"
AWS_PROFILE_START="${AWS_PROFILE_START:-${AWS_PROFILE:-dev-admin}}"

# Dynamic EC2 instance ID lookup by tag (GAP-492 fix — survives AMI bump replacement).
lookup_ec2_instance_ids() {
  AWS_PROFILE="${AWS_PROFILE_START}" aws ec2 describe-instances \
    --region "${AWS_REGION}" \
    --filters \
      "Name=tag:Name,Values=kitehub-kh-backend,kitehub-kc-app,kitehub-kc-app-fe" \
      "Name=instance-state-name,Values=stopped,running" \
    --query 'Reservations[].Instances[].InstanceId' \
    --output text 2>/dev/null
}

mapfile -t EC2_INSTANCE_IDS < <(lookup_ec2_instance_ids | tr '\t' '\n' | grep -v '^$')
if [ "${#EC2_INSTANCE_IDS[@]}" -eq 0 ]; then
  echo "::error :: No kitehub-kh-backend or kitehub-kc-app instances found via tag lookup"
  exit 3
fi
STATE_FILE="${STATE_FILE:-.aws-stack-state.json}"
TIMEOUT_SECONDS=600  # 10 min total budget
POLL_INTERVAL=15

DRY_RUN=false
REASON=""

# ─────────────────────────────────────────────────────────────────
# Argument parsing
# ─────────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=true; shift ;;
    --reason) REASON="${2:-}"; shift 2 ;;
    --help|-h)
      sed -n '1,30p' "$0"
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
      log "ERROR: AWS credentials not configured (set AWS_PROFILE or AWS_ACCESS_KEY_ID)"
      exit 3
    fi
  fi
}

run_aws() {
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: aws $*"
    return 0
  fi
  aws "$@"
}

write_state_entry() {
  local start_time="$1"
  local reason="$2"
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: would append session entry to $STATE_FILE"
    return 0
  fi
  # Ensure file exists with sessions[] array
  if [[ ! -f "$STATE_FILE" ]]; then
    echo '{"sessions":[]}' > "$STATE_FILE"
  fi
  # Append entry using python (jq may not be installed everywhere)
  python3 - "$STATE_FILE" "$start_time" "$reason" \
    "${EC2_INSTANCE_IDS[*]}" "$RDS_DB_IDENTIFIER" <<'PY'
import json, sys, datetime
state_file, start_time, reason, ec2_ids, rds_id = sys.argv[1:6]
with open(state_file) as f:
    data = json.load(f)
if 'sessions' not in data:
    data['sessions'] = []
expected_stop = (datetime.datetime.fromisoformat(start_time.replace('Z','+00:00'))
                 + datetime.timedelta(hours=2)).strftime('%Y-%m-%dT%H:%M:%SZ')
data['sessions'].append({
    'start_time': start_time,
    'stop_time': None,
    'duration_minutes': None,
    'expected_stop_time': expected_stop,
    'reason': reason or 'unspecified',
    'ec2_instance_ids': ec2_ids.split(),
    'rds_db_identifier': rds_id,
    'status': 'started',
})
with open(state_file, 'w') as f:
    json.dump(data, f, indent=2)
PY
  log "Session entry written to $STATE_FILE (expected_stop ~2h)"
}

# ─────────────────────────────────────────────────────────────────
# EC2 start + wait
# ─────────────────────────────────────────────────────────────────
start_ec2() {
  log "Starting EC2 instances: ${EC2_INSTANCE_IDS[*]}"
  run_aws ec2 start-instances \
    --instance-ids "${EC2_INSTANCE_IDS[@]}" \
    --region "$AWS_REGION" \
    --output text >/dev/null
}

wait_ec2_running() {
  log "Waiting for EC2 instances to reach 'running' state..."
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: skipping wait"
    return 0
  fi
  aws ec2 wait instance-running \
    --instance-ids "${EC2_INSTANCE_IDS[@]}" \
    --region "$AWS_REGION"
  log "All EC2 instances RUNNING"
}

wait_ec2_status_ok() {
  log "Waiting for EC2 status checks (2/2)..."
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: skipping status check wait"
    return 0
  fi
  local elapsed=0
  while (( elapsed < TIMEOUT_SECONDS )); do
    local status
    status=$(aws ec2 describe-instance-status \
      --instance-ids "${EC2_INSTANCE_IDS[@]}" \
      --region "$AWS_REGION" \
      --query 'InstanceStatuses[].[InstanceId,InstanceStatus.Status,SystemStatus.Status]' \
      --output text 2>/dev/null || echo "pending")
    local ok_count
    ok_count=$(echo "$status" | awk '$2=="ok" && $3=="ok"' | wc -l)
    if (( ok_count == ${#EC2_INSTANCE_IDS[@]} )); then
      log "EC2 status checks 2/2 PASS"
      return 0
    fi
    log "  status: $ok_count/${#EC2_INSTANCE_IDS[@]} instances OK ($(echo "$status" | tr '\n' ';'))"
    sleep "$POLL_INTERVAL"
    elapsed=$((elapsed + POLL_INTERVAL))
  done
  log "ERROR: EC2 status checks did not reach 2/2 within $TIMEOUT_SECONDS s"
  return 1
}

# ─────────────────────────────────────────────────────────────────
# RDS start + wait
# ─────────────────────────────────────────────────────────────────
start_rds() {
  log "Starting RDS instance: $RDS_DB_IDENTIFIER"
  # Idempotent: if already started, AWS returns InvalidDBInstanceState — swallow it
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: aws rds start-db-instance --db-instance-identifier $RDS_DB_IDENTIFIER"
    return 0
  fi
  if ! aws rds start-db-instance \
        --db-instance-identifier "$RDS_DB_IDENTIFIER" \
        --region "$AWS_REGION" \
        --output text >/dev/null 2>&1; then
    local current
    current=$(aws rds describe-db-instances \
      --db-instance-identifier "$RDS_DB_IDENTIFIER" \
      --region "$AWS_REGION" \
      --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "unknown")
    log "  RDS already in state '$current' (probably starting or available)"
  fi
}

wait_rds_available() {
  log "Waiting for RDS to reach 'available' state..."
  if [[ "$DRY_RUN" == "true" ]]; then
    log "DRY-RUN: skipping RDS wait"
    return 0
  fi
  aws rds wait db-instance-available \
    --db-instance-identifier "$RDS_DB_IDENTIFIER" \
    --region "$AWS_REGION"
  log "RDS AVAILABLE"
}

# ─────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────
main() {
  local script_start
  script_start=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  log "=== Start stack ==="
  log "  Region: $AWS_REGION"
  log "  EC2:    ${EC2_INSTANCE_IDS[*]} (${EC2_INSTANCE_NAMES[*]})"
  log "  RDS:    $RDS_DB_IDENTIFIER"
  log "  Reason: ${REASON:-<unspecified>}"
  log "  Mode:   $([[ "$DRY_RUN" == "true" ]] && echo "DRY-RUN" || echo "REAL")"

  require_aws_cli

  # Kick off EC2 + RDS in parallel-ish (CLI calls return immediately)
  start_ec2
  start_rds

  # Wait for both
  wait_ec2_running
  wait_rds_available

  # Final health check for EC2 (RDS is health-checked above already)
  if ! wait_ec2_status_ok; then
    log "WARN: EC2 status checks incomplete; ssh-level access may not yet be ready"
    write_state_entry "$script_start" "$REASON"
    exit 2
  fi

  write_state_entry "$script_start" "$REASON"
  log "=== Stack STARTED ($(( $(date +%s) - $(date -d "$script_start" +%s 2>/dev/null || echo 0) )) s elapsed) ==="
  log ""
  log "Next steps:"
  log "  - Verify ALB targets healthy:  curl -sI https://api.kitehub.me/actuator/health"
  log "  - Smoke test endpoints:        bash scripts/smoke-test.sh"
  log "  - Stop when done:              bash scripts/aws/stop-stack.sh"
}

main "$@"
