#!/usr/bin/env bash
#
# populate-secrets.sh — Phase 2.4 helper for AWS Secrets Manager
#
# Per .claude/rules/agent-aws-access.md §4.1: secretsmanager put-secret-value
# is Tier 3 mutation — HUMAN execution only. Agent does NOT run this script.
#
# Per terraform reality (infrastructure/terraform-aws/secrets.tf, verified
# 2026-05-08 via terraform apply outputs):
#
#   AUTO-POPULATED by terraform random_password (no user-action needed):
#     kitehub/production/db-password    -- random 32 chars (rds.tf)
#     kitehub/production/jwt-secret     -- random 64 chars
#     kitehub/production/encryption-key -- base64 random 32 bytes
#
#   PLACEHOLDERS (need explicit user-action):
#     kitehub/production/rabbitmq-default-creds  -- REQUIRED Phase 1 BETA
#     kitehub/production/ai-openai-api-key       -- DEFERRED Phase 2 (ADR-026)
#     kitehub/production/ai-anthropic-api-key    -- DEFERRED Phase 2 (ADR-026)
#     kitehub/production/cloudflare-api-token    -- DEFERRED Phase 2
#     kitehub/production/ses-smtp-credentials    -- N/A Stream A pivot to Resend
#                                                   (RESEND_API_KEY in GH Secret)
#
# This script populates ONLY the 1 required Phase 1 BETA placeholder
# (rabbitmq-default-creds) with auto-generated random user/pass JSON.
# The 4 deferred placeholders left empty; populate when feature reactivated.
#
# Usage:
#   bash scripts/populate-secrets.sh --dry-run      # preview
#   bash scripts/populate-secrets.sh --yes          # execute

set -uo pipefail

DRY_RUN=0
SKIP_CONFIRM=0
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    --yes)     SKIP_CONFIRM=1 ;;
    -h|--help)
      sed -n '/^#/,/^$/p' "$0" | head -32 | sed 's/^# \?//'
      exit 0 ;;
  esac
done

REGION="ap-southeast-1"
ACCOUNT_ID="906286017800"
PROJECT_NAME="kitehub"
ENVIRONMENT="production"

# Color output
if [[ -t 1 ]]; then
  G='\033[0;32m'; Y='\033[0;33m'; R='\033[0;31m'; B='\033[0;36m'; N='\033[0m'
else
  G=''; Y=''; R=''; B=''; N=''
fi

log_info() { echo -e "${B}[INFO]${N} $*"; }
log_ok()   { echo -e "${G}[OK]${N}   $*"; }
log_warn() { echo -e "${Y}[WARN]${N} $*"; }
log_err()  { echo -e "${R}[ERR]${N}  $*" >&2; }

confirm() {
  [[ $SKIP_CONFIRM -eq 1 ]] && return 0
  read -r -p "$1 [y/N] " ans
  [[ "${ans,,}" == "y" || "${ans,,}" == "yes" ]]
}

preflight() {
  command -v openssl &>/dev/null || { log_err "openssl not installed"; exit 1; }
  command -v jq &>/dev/null || { log_err "jq not installed"; exit 1; }

  if [[ $DRY_RUN -eq 1 ]]; then
    if ! command -v aws &>/dev/null; then
      log_warn "aws CLI not installed (OK for --dry-run; required for live)"
    fi
    log_ok "Region:      $REGION"
    return 0
  fi

  command -v aws &>/dev/null || { log_err "aws CLI not installed"; exit 1; }
  actual_account=$(aws sts get-caller-identity --query Account --output text 2>/dev/null || echo "")
  if [[ "$actual_account" != "$ACCOUNT_ID" ]]; then
    log_err "Wrong AWS account: expected $ACCOUNT_ID, got '$actual_account'"
    log_err "Run: aws sts get-caller-identity"
    exit 1
  fi
  log_ok "AWS account: $actual_account"
  log_ok "Region:      $REGION"
}

put_secret_json() {
  local name="$1"
  local payload="$2"
  local secret_id="${PROJECT_NAME}/${ENVIRONMENT}/${name}"

  if [[ $DRY_RUN -eq 1 ]]; then
    local size
    size=$(printf '%s' "$payload" | wc -c)
    log_info "[dry-run] PUT $secret_id (JSON, $size bytes)"
    return 0
  fi

  if aws secretsmanager describe-secret --secret-id "$secret_id" --region "$REGION" &>/dev/null; then
    if aws secretsmanager put-secret-value \
         --secret-id "$secret_id" \
         --secret-string "$payload" \
         --region "$REGION" >/dev/null; then
      log_ok "PUT $secret_id"
    else
      log_err "Failed PUT $secret_id"
      return 1
    fi
  else
    log_err "Secret does NOT exist: $secret_id"
    log_err "Run terraform apply to create the placeholder first"
    return 1
  fi
}

main() {
  echo "========================================================================="
  echo "Phase 2.4 Secrets Populate -- Phase 1 BETA scope"
  echo "========================================================================="
  echo "  Account: $ACCOUNT_ID"
  echo "  Region:  $REGION"
  if [[ $DRY_RUN -eq 1 ]]; then
    echo "  Mode:    DRY-RUN"
  else
    echo "  Mode:    LIVE"
  fi
  echo "========================================================================="

  preflight

  echo
  log_info "Phase 1 BETA secrets to populate (1 required):"
  echo "  1. ${PROJECT_NAME}/${ENVIRONMENT}/rabbitmq-default-creds  (auto-gen JSON)"
  echo
  log_info "Deferred placeholders (left empty -- populate when feature reactivated):"
  echo "  - ai-openai-api-key      (Phase 2 per ADR-026)"
  echo "  - ai-anthropic-api-key   (Phase 2 per ADR-026)"
  echo "  - cloudflare-api-token   (Phase 2)"
  echo "  - ses-smtp-credentials   (N/A -- Resend pivot via RESEND_API_KEY)"
  echo
  log_info "Auto-populated by terraform random_password (no action needed):"
  echo "  - db-password, jwt-secret, encryption-key"

  if ! confirm $'\nProceed?'; then
    log_warn "Aborted by user"
    exit 0
  fi

  local rmq_user="kite_admin_$(openssl rand -hex 4)"
  local rmq_pass
  rmq_pass=$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)
  local rmq_payload
  rmq_payload=$(jq -n --arg u "$rmq_user" --arg p "$rmq_pass" \
    '{username: $u, password: $p}')

  put_secret_json "rabbitmq-default-creds" "$rmq_payload" || exit 1

  echo
  echo "========================================================================="
  if [[ $DRY_RUN -eq 1 ]]; then
    log_ok "Dry-run complete. Re-run without --dry-run to apply."
  else
    log_ok "Populate run complete. 1 secret populated, 4 deferred."
  fi
  echo "========================================================================="
}

main "$@"
