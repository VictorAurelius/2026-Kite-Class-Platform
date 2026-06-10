#!/usr/bin/env bash
#
# fetch-secrets.sh — Phase 1 BETA Secrets Manager → /etc/kite/.env populator
#
# Fetches secrets from AWS Secrets Manager (using EC2 instance profile,
# no static keys) and writes them into /etc/kite/.env for docker-compose.production.yml.
#
# Run on kh-backend EC2 BEFORE `docker compose up`.
# Run via SSM session OR as part of deploy-prod.sh.
#
# Per .claude/rules/agent-aws-access.md §2.2: get-secret-value is Tier 2
# (always-confirm) — but on EC2 with instance profile + secretsmanager:GetSecretValue
# scoped to kitehub/production/* this is the legitimate runtime path. Agent
# does NOT run this; it runs on EC2 as part of bootstrap.
#
# Required EC2 instance profile permissions (provisioned by terraform):
#   secretsmanager:GetSecretValue on arn:aws:secretsmanager:*:*:secret:kitehub/production/*
#   secretsmanager:DescribeSecret on same scope

set -euo pipefail

REGION="ap-southeast-1"
ENV_FILE="/etc/kite/.env"
LOG="${LOG:-/var/log/kite-bootstrap.log}"

log() { echo "[$(date -u +%FT%TZ)] $*" | tee -a "$LOG"; }

# Ensure /etc/kite directory exists with restricted permissions
sudo mkdir -p /etc/kite
sudo chown root:docker /etc/kite
sudo chmod 0750 /etc/kite

log "Fetching Phase 1 BETA secrets from Secrets Manager..."

fetch_secret() {
  local name="$1"
  local field="${2:-}"  # optional JSON field for json secrets

  local value
  value=$(aws secretsmanager get-secret-value \
    --secret-id "kitehub/production/${name}" \
    --region "$REGION" \
    --query SecretString --output text 2>/dev/null) || {
    log "WARN: Secret kitehub/production/${name} not found OR access denied"
    return 1
  }

  if [[ -n "$field" ]]; then
    echo "$value" | jq -r ".$field"
  else
    echo "$value"
  fi
}

# Fetch all required secrets
DB_PAYLOAD=$(fetch_secret db-password)
DB_USERNAME=$(echo "$DB_PAYLOAD" | jq -r .username)
DB_PASSWORD=$(echo "$DB_PAYLOAD" | jq -r .password)
DB_HOST=$(echo "$DB_PAYLOAD" | jq -r .host)
DB_PORT=$(echo "$DB_PAYLOAD" | jq -r .port)
DB_NAME=$(echo "$DB_PAYLOAD" | jq -r .dbname)

JWT_SECRET=$(fetch_secret jwt-secret)
# Wave 81 Bucket F — 3 fail-fast guards trong kitehub-subscription mandate
# production-set values (>=32 bytes, NOT dev default). Per code sweep
# `grep -rnE "isDevDefault|MUST be set" kitehub/*/src/main/java`:
#   1. ChallengeTokenService (Wave 79 GAP-509) → JWT_CHALLENGE_SECRET
#   2. TotpSecretCipher (Wave 72b GAP-516) → TOTP_ENCRYPTION_KEY
#   3. InvitationTokenService (Wave 78 GAP-548) → KITEHUB_STAFF_INVITATION_SIGNING_SECRET
# Secrets được tạo manual qua AWS Secrets Manager `--secret-string file:///dev/stdin`
# (stdin pipe pattern — không leak qua chat). Reference: Wave 81 jwt-secret-fix runbook.
JWT_CHALLENGE_SECRET=$(fetch_secret jwt-challenge-secret)
TOTP_ENCRYPTION_KEY_VALUE=$(fetch_secret totp-encryption-key)
STAFF_INVITATION_SIGNING_SECRET=$(fetch_secret staff-invitation-signing-secret)
ENCRYPTION_KEY=$(fetch_secret encryption-key)

# RabbitMQ creds (populated by populate-secrets.sh — may be empty if not yet run)
if RMQ_PAYLOAD=$(fetch_secret rabbitmq-default-creds 2>/dev/null); then
  RMQ_USER=$(echo "$RMQ_PAYLOAD" | jq -r .username 2>/dev/null || echo "")
  RMQ_PASS=$(echo "$RMQ_PAYLOAD" | jq -r .password 2>/dev/null || echo "")
fi
if [[ -z "${RMQ_USER:-}" || "$RMQ_USER" == "null" ]]; then
  log "WARN: rabbitmq-default-creds empty — generating ephemeral creds (run populate-secrets.sh + redeploy for persistence)"
  RMQ_USER="kite_admin_$(openssl rand -hex 4)"
  RMQ_PASS=$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)
fi

# Resend (Stream A per ADR-025 — HTTP API replaces SMTP path)
# GAP-508 Phase 2: pull from AWS Secrets Manager kitehub/production/resend-api-key.
# Secret payload schema: {"api_key":"re_...","from_email":"noreply@kitehub.me","from_name":"KiteHub Beta"}
RESEND_API_KEY=""
AWS_SES_FROM_EMAIL_FROM_SECRET=""
AWS_SES_FROM_NAME_FROM_SECRET=""
if RESEND_PAYLOAD=$(fetch_secret resend-api-key 2>/dev/null); then
  # GAP-572 — accept BOTH schemas: JSON wrapper {api_key,from_email,from_name} OR plain string re_...
  # Detect first char: '{' = JSON parse; else = treat entire value as api_key (defaults for from_*).
  if [[ "${RESEND_PAYLOAD:0:1}" == "{" ]]; then
    RESEND_API_KEY=$(echo "$RESEND_PAYLOAD" | jq -r '.api_key // empty' 2>/dev/null || echo "")
    AWS_SES_FROM_EMAIL_FROM_SECRET=$(echo "$RESEND_PAYLOAD" | jq -r '.from_email // empty' 2>/dev/null || echo "")
    AWS_SES_FROM_NAME_FROM_SECRET=$(echo "$RESEND_PAYLOAD" | jq -r '.from_name // empty' 2>/dev/null || echo "")
  else
    RESEND_API_KEY="$RESEND_PAYLOAD"
    log "INFO: Resend secret stored as plain string; api_key extracted; from_email/from_name use defaults. Re-store as JSON to override defaults."
  fi
fi
# Fail-safe: allow caller-provided env override (legacy / dev path)
RESEND_API_KEY="${RESEND_API_KEY:-${RESEND_API_KEY_FALLBACK:-}}"
if [[ -z "$RESEND_API_KEY" ]]; then
  log "WARN: kitehub/production/resend-api-key not found or empty — emails will NOT deliver. See documents/05-guides/deploy/resend-provisioning-runbook.md"
fi
# Derive from-email/from-name with sensible fallbacks
AWS_SES_FROM_EMAIL="${AWS_SES_FROM_EMAIL_FROM_SECRET:-${AWS_SES_FROM_EMAIL:-noreply@kitehub.me}}"
AWS_SES_FROM_NAME="${AWS_SES_FROM_NAME_FROM_SECRET:-${AWS_SES_FROM_NAME:-KiteHub Beta}}"

# SePay API key (Wave flow-kh3-3 — payment webhook Apikey auth)
# Pulled from AWS Secrets Manager kitehub/production/sepay-api-key (plain string,
# vendor-set manually post-apply). May be empty until SePay merchant account provisioned.
# Binds to kitehub.payment.sepay.api-key (SEPAY_API_KEY env). When empty, the webhook
# Apikey check fails closed (401) — no payment confirmations process until configured.
SEPAY_API_KEY=""
if SEPAY_PAYLOAD=$(fetch_secret sepay-api-key 2>/dev/null); then
  SEPAY_API_KEY="$SEPAY_PAYLOAD"
fi
SEPAY_API_KEY="${SEPAY_API_KEY:-${SEPAY_API_KEY_FALLBACK:-}}"
if [[ -z "$SEPAY_API_KEY" ]]; then
  log "WARN: kitehub/production/sepay-api-key not found or empty — payment webhook will reject all SePay calls (401). Configure via SePay dashboard + AWS console post-apply."
fi

# AI Branding generation providers (GAP-1117 / ADR-037 Amendment). Both optional —
# when empty, kitehub-branding runs MOCK mode (Vietnamese sample copy + logo/placeholder
# banner; pipeline never crashes). Provisioning: documents/05-guides/deploy/ai-branding-provider-setup-runbook.md
#   - gemini-api-key  → TEMPLATE copy/HTML (Gemini free-tier; AI_PROVIDER=gemini)
#   - openai-api-key  → FULL_AI GPT-5.5 image (PREMIUM/ENTERPRISE only)
GEMINI_API_KEY=""
if GEMINI_PAYLOAD=$(fetch_secret gemini-api-key 2>/dev/null); then
  GEMINI_API_KEY="$GEMINI_PAYLOAD"
fi
GEMINI_API_KEY="${GEMINI_API_KEY:-${GEMINI_API_KEY_FALLBACK:-}}"
if [[ -z "$GEMINI_API_KEY" ]]; then
  log "INFO: kitehub/production/gemini-api-key not set — AI Branding TEMPLATE copy runs MOCK (Vietnamese sample). Set to enable real Gemini generation."
fi

OPENAI_API_KEY=""
if OPENAI_PAYLOAD=$(fetch_secret openai-api-key 2>/dev/null); then
  OPENAI_API_KEY="$OPENAI_PAYLOAD"
fi
OPENAI_API_KEY="${OPENAI_API_KEY:-${OPENAI_API_KEY_FALLBACK:-}}"
if [[ -z "$OPENAI_API_KEY" ]]; then
  log "INFO: kitehub/production/openai-api-key not set — FULL_AI banner (GPT-5.5) disabled; PREMIUM/ENTERPRISE fall back to TEMPLATE. Set to enable."
fi

# Provider selection: prefer Gemini when its key is present, else keep configured default.
AI_PROVIDER="${AI_PROVIDER:-$([[ -n "$GEMINI_API_KEY" ]] && echo gemini || echo openai)}"

# Zalo OA credentials (GAP-063 — notification channel; mock until ZALO_PROVIDER=live)
# Pulled from AWS Secrets Manager kitehub/production/zalo-oa-credentials.
# Secret payload schema: JSON {"oa_id":"<numeric>","access_token":"<token>"}.
# Binds to ZALO_OA_ID + ZALO_ACCESS_TOKEN. Empty until Zalo OA business account verified;
# kitehub-email stays on ZaloOAMockClient (deterministic canned responses) when unset.
ZALO_OA_ID=""
ZALO_ACCESS_TOKEN=""
if ZALO_PAYLOAD=$(fetch_secret zalo-oa-credentials 2>/dev/null); then
  if [[ "${ZALO_PAYLOAD:0:1}" == "{" ]]; then
    ZALO_OA_ID=$(echo "$ZALO_PAYLOAD" | jq -r '.oa_id // empty' 2>/dev/null || echo "")
    ZALO_ACCESS_TOKEN=$(echo "$ZALO_PAYLOAD" | jq -r '.access_token // empty' 2>/dev/null || echo "")
  else
    log "WARN: zalo-oa-credentials not JSON {oa_id, access_token} — Zalo channel stays mock. Re-store as JSON per account-prep/zalo-oa-setup-runbook.md."
  fi
fi
ZALO_OA_ID="${ZALO_OA_ID:-${ZALO_OA_ID_FALLBACK:-}}"
ZALO_ACCESS_TOKEN="${ZALO_ACCESS_TOKEN:-${ZALO_ACCESS_TOKEN_FALLBACK:-}}"
if [[ -z "$ZALO_ACCESS_TOKEN" ]]; then
  log "INFO: kitehub/production/zalo-oa-credentials not set — Zalo notification channel stays in mock mode (ZALO_PROVIDER unset). Configure post-apply to go live."
fi

# Write .env file
sudo tee "$ENV_FILE" > /dev/null <<ENVEOF
# Generated by fetch-secrets.sh — DO NOT EDIT BY HAND
# Regenerate: bash scripts/fetch-secrets.sh
# Generated at: $(date -u +%FT%TZ)

# Image version (set by deploy-prod.sh — default = current Wave 81 deployed tag
# without the "v" prefix per ECR convention; Wave 81 fix: stale "v0.9.0-beta-staging.8"
# default caused /etc/kite/.env corruption when fetch-secrets.sh re-run standalone)
KITE_VERSION=${KITE_VERSION:-0.9.0-beta-staging.14}

# Database (RDS — Phase 2.3 outputs)
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}

# Redis (self-hosted on EC2)
REDIS_HOST=kite-redis
REDIS_PORT=6379
REDIS_PASSWORD=
SPRING_DATA_REDIS_HOST=kite-redis
SPRING_DATA_REDIS_PORT=6379

# RabbitMQ (self-hosted on EC2)
RABBITMQ_USER=${RMQ_USER}
RABBITMQ_PASS=${RMQ_PASS}
SPRING_RABBITMQ_HOST=kite-rabbitmq
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=${RMQ_USER}
SPRING_RABBITMQ_PASSWORD=${RMQ_PASS}

# Auth secrets
# Wave 81 Bucket F attempt 3 — dual-write TOTP key cho cả 2 binding paths:
#   - TOTP_ENCRYPTION_KEY: matches subscription application yml line 109 yaml binding
#   - KITEHUB_AUTH_TOTP_ENCRYPTION_KEY: Spring relaxed binding cho admin (yaml-less)
# Both names point to same secret value. Wave 82+ cleanup: rename yaml + drop alias.
JWT_SECRET=${JWT_SECRET}
JWT_CHALLENGE_SECRET=${JWT_CHALLENGE_SECRET}
TOTP_ENCRYPTION_KEY=${TOTP_ENCRYPTION_KEY_VALUE}
KITEHUB_AUTH_TOTP_ENCRYPTION_KEY=${TOTP_ENCRYPTION_KEY_VALUE}
KITEHUB_STAFF_INVITATION_SIGNING_SECRET=${STAFF_INVITATION_SIGNING_SECRET}
ENCRYPTION_MASTER_KEY=${ENCRYPTION_KEY}

# Email (Resend — Stream A per ADR-025)
RESEND_API_KEY=${RESEND_API_KEY}
AWS_SES_FROM_EMAIL=${AWS_SES_FROM_EMAIL}
AWS_SES_FROM_NAME=${AWS_SES_FROM_NAME}

# Payment — SePay webhook Apikey auth (Wave flow-kh3-3)
SEPAY_API_KEY=${SEPAY_API_KEY}

# AI Branding generation (GAP-1117 / ADR-037). Empty → MOCK mode (no crash).
AI_PROVIDER=${AI_PROVIDER}
GEMINI_API_KEY=${GEMINI_API_KEY}
OPENAI_API_KEY=${OPENAI_API_KEY}

# Notification — Zalo OA channel (GAP-063); ZALO_PROVIDER stays mock until access_token present
ZALO_PROVIDER=${ZALO_PROVIDER:-mock}
ZALO_OA_ID=${ZALO_OA_ID}
ZALO_ACCESS_TOKEN=${ZALO_ACCESS_TOKEN}

# Feature flags — kiteclass-core parent portal (Wave auth-2 Bucket C / GAP-1014)
# Public (non-secret) feature flag, override mechanism #1 per production-env-config-registry.md §4.
# Binds kiteclass-core application.yml:318 parent-portal.enabled (default ${PARENT_PORTAL_ENABLED:false}).
# Production MUST set true so parent (and pulled-forward teacher) KC-native login surface is reachable
# (per Wave auth-1 ops-readiness audit P1-3). Default true here; deploy-prod.sh / SSM env can override.
#
# PDPL NOTE: parent portal exposes child/student data to a parent account, which assumes a working
# consent gate (parent-child link verified + consent recorded) is active. Enabling this flag in
# production = asserting that gate is live. If consent enforcement is not yet wired for a tenant,
# set PARENT_PORTAL_ENABLED=false for that deploy until consent gate ships.
#
# NOTE (GAP-444 / Phase 7): kiteclass-core is NOT yet declared in docker-compose.production.yml, so
# this var has no production consumer until the KC stack prod deploy lands. It is written here ahead
# of that deploy so the chain is ready; see env-vars-registry.md "kc-core production deploy deferred"
# note for the deferred production-parity item.
PARENT_PORTAL_ENABLED=${PARENT_PORTAL_ENABLED:-true}

# Region pinning
AWS_REGION=ap-southeast-1
ENVEOF

sudo chown root:docker "$ENV_FILE"
sudo chmod 0640 "$ENV_FILE"

log "Wrote ${ENV_FILE} ($(wc -l < <(sudo cat "$ENV_FILE")) lines)"
log "Secrets fetch complete"
