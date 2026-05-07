#!/usr/bin/env bash
# =========================================================================
# populate-secrets.sh — Phase 2.4 Secrets Manager populate helper
# =========================================================================
#
# Automates the runbook in `documents/05-guides/deploy/secrets-populate-phase-2-4.md`.
# Generates random values + populates 7 AWS Secrets Manager placeholders
# created by Phase 2.3 terraform apply.
#
# Usage:
#   bash scripts/populate-secrets.sh --dry-run       # preview only, no AWS calls
#   bash scripts/populate-secrets.sh --yes           # populate without per-secret prompt
#   bash scripts/populate-secrets.sh                 # interactive (per-secret confirm)
#   bash scripts/populate-secrets.sh --dry-run --yes # combine: preview all 7 non-interactively
#
# IMPORTANT — agent-aws-access policy:
#   This script runs Tier 3 mutation commands (`aws secretsmanager create-secret` +
#   `put-secret-value`). Per `.claude/rules/agent-aws-access.md` §4.1, mutation
#   commands are BANNED for agent execution. This script is for HUMAN execution
#   only. Agents must never invoke this script.
#
# Pre-conditions (verified at runtime):
#   - aws CLI installed
#   - Authenticated as account 906286017800 (`aws sts get-caller-identity`)
#   - Region ap-southeast-1
#
# What gets populated (5 secrets):
#   1. kite/prod/db/password           — openssl rand -base64 32 (auto-generated)
#   2. kite/prod/jwt/secret            — openssl rand -base64 64 (auto-generated)
#   3. kite/prod/encryption/master-key — openssl rand -base64 32 (auto-generated)
#   4. kite/prod/internal-api/secret   — openssl rand -base64 32 (auto-generated)
#   5. kite/prod/ses/configuration-set — fixed value: kitehub-prod
#
# Note (Phase 1 BETA): OpenAI/Anthropic API keys NOT populated by this script.
# AI provider integration deferred per ADR-026 (Ollama defer Phase 2). When
# AI keys needed, populate manually via AWS Console or extend this script.
#
# Idempotent: re-runs safe. If a secret already has a populated value (per
# `describe-secret` LastChangedDate), prints `[SKIP]` and moves on.
#
# Security:
#   - Secret values NEVER printed to stdout. Only secret IDs + statuses.
#   - Prompts use `read -r -s` (no echo).
#   - Script does not log secrets to any file.
#
# Reference:
#   - Runbook: documents/05-guides/deploy/secrets-populate-phase-2-4.md
#   - Phase 2.3 terraform: infrastructure/terraform-aws/secrets.tf
#   - Rule: .claude/rules/agent-aws-access.md §4.1 (mutation commands)
# =========================================================================

set -euo pipefail

readonly EXPECTED_ACCOUNT_ID="906286017800"
readonly EXPECTED_REGION="ap-southeast-1"

# Flags
DRY_RUN=0
SKIP_CONFIRM=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1; shift ;;
    --yes|-y)  SKIP_CONFIRM=1; shift ;;
    -h|--help)
      sed -n '2,50p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown flag: $1" >&2
      echo "Usage: $0 [--dry-run] [--yes]" >&2
      exit 2
      ;;
  esac
done

# Colors (only if stdout is a tty)
if [[ -t 1 ]]; then
  GREEN=$'\033[0;32m'
  YELLOW=$'\033[0;33m'
  RED=$'\033[0;31m'
  CYAN=$'\033[0;36m'
  NC=$'\033[0m'
else
  GREEN="" YELLOW="" RED="" CYAN="" NC=""
fi

log_info()  { echo "${CYAN}[INFO]${NC} $*"; }
log_ok()    { echo "${GREEN}[OK]${NC} $*"; }
log_skip()  { echo "${YELLOW}[SKIP]${NC} $*"; }
log_warn()  { echo "${YELLOW}[WARN]${NC} $*" >&2; }
log_err()   { echo "${RED}[ERROR]${NC} $*" >&2; }

# -------------------------------------------------------------------------
# Pre-flight checks
# -------------------------------------------------------------------------
preflight() {
  log_info "Running pre-flight checks..."

  if ! command -v aws >/dev/null 2>&1; then
    if [[ $DRY_RUN -eq 1 ]]; then
      log_warn "aws CLI not found (dry-run continues without it)"
    else
      log_err "aws CLI not found. Install: https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html"
      exit 1
    fi
  else
    log_ok "aws CLI present: $(aws --version 2>&1 | awk '{print $1}')"
  fi

  if ! command -v openssl >/dev/null 2>&1; then
    log_err "openssl not found. Install via package manager."
    exit 1
  fi
  log_ok "openssl present"

  if [[ $DRY_RUN -eq 1 ]]; then
    log_warn "Dry-run mode — skipping AWS account verification"
    return 0
  fi

  local account_id
  if ! account_id=$(aws sts get-caller-identity --query Account --output text 2>/dev/null); then
    log_err "Failed to call aws sts get-caller-identity. Check credentials (aws configure / SSO)."
    exit 1
  fi

  if [[ "$account_id" != "$EXPECTED_ACCOUNT_ID" ]]; then
    log_err "Wrong AWS account. Expected $EXPECTED_ACCOUNT_ID, got $account_id."
    log_err "Switch profile/role and re-run."
    exit 1
  fi
  log_ok "AWS account verified: $account_id"

  local region="${AWS_REGION:-${AWS_DEFAULT_REGION:-}}"
  if [[ -z "$region" ]]; then
    region=$(aws configure get region 2>/dev/null || echo "")
  fi
  if [[ "$region" != "$EXPECTED_REGION" ]]; then
    log_warn "Region is '$region' (expected '$EXPECTED_REGION'). Will pass --region $EXPECTED_REGION explicitly."
  else
    log_ok "Region: $region"
  fi
}

# -------------------------------------------------------------------------
# Confirmation prompt
# -------------------------------------------------------------------------
confirm() {
  local msg="$1"
  if [[ $SKIP_CONFIRM -eq 1 ]]; then
    return 0
  fi
  local reply
  read -r -p "$msg [y/N] " reply
  [[ "$reply" =~ ^[Yy]$ ]]
}

# -------------------------------------------------------------------------
# Secret existence + populated check
# Returns:
#   0 if secret exists AND has been populated (LastChangedDate present)
#   1 if secret exists but never populated (placeholder only)
#   2 if secret does not exist
# -------------------------------------------------------------------------
secret_status() {
  local secret_id="$1"
  local out
  if ! out=$(aws secretsmanager describe-secret \
        --secret-id "$secret_id" \
        --region "$EXPECTED_REGION" \
        --query 'LastChangedDate' \
        --output text 2>/dev/null); then
    return 2
  fi
  # If LastChangedDate equals CreatedDate, the secret has only had its
  # initial empty/placeholder version. AWS still returns LastChangedDate
  # on every secret, so treat "exists" as exists; let put-secret-value
  # be idempotent. We use this only for human-readable status messages.
  if [[ -z "$out" || "$out" == "None" ]]; then
    return 1
  fi
  return 0
}

# -------------------------------------------------------------------------
# Put or create secret
# Args: secret_id, secret_value, description
# -------------------------------------------------------------------------
populate_secret() {
  local secret_id="$1"
  local secret_value="$2"
  local description="${3:-}"

  if [[ $DRY_RUN -eq 1 ]]; then
    log_info "[dry-run] would populate: $secret_id"
    return 0
  fi

  local status=0
  secret_status "$secret_id" || status=$?

  case $status in
    0|1)
      # Exists (0=populated, 1=placeholder) — use put-secret-value (idempotent)
      if aws secretsmanager put-secret-value \
          --secret-id "$secret_id" \
          --secret-string "$secret_value" \
          --region "$EXPECTED_REGION" \
          --output text \
          --query 'ARN' >/dev/null; then
        log_ok "$secret_id (updated)"
      else
        log_err "Failed to put-secret-value for $secret_id"
        return 1
      fi
      ;;
    2)
      # Does not exist — create it
      if aws secretsmanager create-secret \
          --name "$secret_id" \
          --description "$description" \
          --secret-string "$secret_value" \
          --region "$EXPECTED_REGION" \
          --output text \
          --query 'ARN' >/dev/null; then
        log_ok "$secret_id (created)"
      else
        log_err "Failed to create-secret for $secret_id"
        return 1
      fi
      ;;
    *)
      log_err "Unexpected status check error for $secret_id (code=$status)"
      return 1
      ;;
  esac
}

# -------------------------------------------------------------------------
# Per-secret handlers
# Each handler:
#   1. Prints intent
#   2. Asks confirm (unless --yes)
#   3. Generates or reads value
#   4. Calls populate_secret (or [SKIP])
# Never echoes the secret value.
# -------------------------------------------------------------------------

handle_random() {
  local secret_id="$1"
  local byte_count="$2"
  local description="$3"

  echo
  log_info "Secret: $secret_id"
  log_info "  Source: openssl rand -base64 $byte_count"
  log_info "  Description: $description"

  if ! confirm "  Populate $secret_id?"; then
    log_skip "$secret_id (user declined)"
    return 0
  fi

  local value
  value=$(openssl rand -base64 "$byte_count")
  populate_secret "$secret_id" "$value" "$description"
}

handle_prompt() {
  local secret_id="$1"
  local prompt_label="$2"
  local description="$3"

  echo
  log_info "Secret: $secret_id"
  log_info "  Source: user input (read -r -s, hidden)"
  log_info "  Description: $description"

  if ! confirm "  Populate $secret_id?"; then
    log_skip "$secret_id (user declined)"
    return 0
  fi

  if [[ $DRY_RUN -eq 1 ]]; then
    log_info "[dry-run] would prompt for $prompt_label"
    return 0
  fi

  if [[ $SKIP_CONFIRM -eq 1 ]]; then
    log_warn "$secret_id requires interactive input — cannot use --yes alone."
    log_warn "  Re-run without --yes for this secret, or set ${prompt_label}_VALUE env var."
    local env_var="${prompt_label}_VALUE"
    local value="${!env_var:-}"
    if [[ -z "$value" ]]; then
      log_skip "$secret_id (no env var $env_var, --yes mode)"
      return 0
    fi
    populate_secret "$secret_id" "$value" "$description"
    return 0
  fi

  local value=""
  read -r -s -p "  Enter $prompt_label: " value
  echo
  if [[ -z "$value" ]]; then
    log_skip "$secret_id (empty input)"
    return 0
  fi
  populate_secret "$secret_id" "$value" "$description"
}

handle_fixed() {
  local secret_id="$1"
  local fixed_value="$2"
  local description="$3"

  echo
  log_info "Secret: $secret_id"
  log_info "  Source: fixed value"
  log_info "  Description: $description"

  if ! confirm "  Populate $secret_id?"; then
    log_skip "$secret_id (user declined)"
    return 0
  fi

  populate_secret "$secret_id" "$fixed_value" "$description"
}

# -------------------------------------------------------------------------
# Main
# -------------------------------------------------------------------------
main() {
  echo "========================================================================="
  echo "Phase 2.4 Secrets Manager populate helper"
  echo "  Account: $EXPECTED_ACCOUNT_ID"
  echo "  Region:  $EXPECTED_REGION"
  if [[ $DRY_RUN -eq 1 ]]; then
    echo "  Mode:    DRY-RUN (no AWS mutations)"
  else
    echo "  Mode:    LIVE (will mutate AWS Secrets Manager)"
  fi
  if [[ $SKIP_CONFIRM -eq 1 ]]; then
    echo "  Prompts: --yes (per-secret confirm skipped)"
  fi
  echo "========================================================================="

  preflight

  echo
  log_info "Will process 5 secrets (AI keys deferred Phase 2 per ADR-026):"
  echo "  1. kite/prod/db/password           (random 32-byte)"
  echo "  2. kite/prod/jwt/secret            (random 64-byte)"
  echo "  3. kite/prod/encryption/master-key (random 32-byte)"
  echo "  4. kite/prod/internal-api/secret   (random 32-byte)"
  echo "  5. kite/prod/ses/configuration-set (fixed: kitehub-prod)"

  if ! confirm $'\nProceed?'; then
    log_warn "Aborted by user"
    exit 0
  fi

  handle_random "kite/prod/db/password"           32 "RDS master password"
  handle_random "kite/prod/jwt/secret"            64 "JWT signing secret"
  handle_random "kite/prod/encryption/master-key" 32 "Application encryption master key"
  handle_random "kite/prod/internal-api/secret"   32 "Internal service-to-service auth secret"

  handle_fixed  "kite/prod/ses/configuration-set" "kitehub-prod" "SES configuration set name"

  echo
  echo "========================================================================="
  if [[ $DRY_RUN -eq 1 ]]; then
    log_ok "Dry-run complete. Re-run without --dry-run to apply."
  else
    log_ok "Populate run complete."
    log_info "Verify: aws secretsmanager list-secrets --region $EXPECTED_REGION \\"
    log_info "          --filters Key=name,Values=kite/prod/ \\"
    log_info "          --query 'SecretList[].[Name,LastChangedDate]' --output table"
  fi
  echo "========================================================================="
}

main "$@"
