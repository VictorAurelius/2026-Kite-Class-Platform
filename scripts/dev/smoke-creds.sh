#!/usr/bin/env bash
# smoke-creds.sh — In bảng credentials cho dev self-test trong 1 màn hình
#
# Ưu tiên đọc credentials từ AWS Secrets Manager (secret_id=kite/dev/smoke-creds).
# Nếu secret chưa tồn tại HOẶC không có AWS CLI → fallback đọc từ .env.test
# (sao chép từ .env.test.example).
#
# Tham khảo: documents/03-planning/waves/wave-2026-05-17-87-dev-self-test-enablement.md §3 Bucket A
#
# Sử dụng:
#   bash scripts/dev/smoke-creds.sh           # In credentials table
#   bash scripts/dev/smoke-creds.sh --help    # Show usage

set -euo pipefail

: "${AWS_PROFILE:=dev-admin}"
: "${AWS_REGION:=ap-southeast-1}"
: "${SECRET_ID:=kite/dev/smoke-creds}"
: "${ENV_FILE:=.env.test}"
readonly ENV_EXAMPLE=".env.test.example"

usage() {
  cat <<EOF
smoke-creds.sh — In credentials cho dev self-test

Sử dụng:
  bash scripts/dev/smoke-creds.sh [OPTIONS]

OPTIONS:
  --help       Show this help

NGUỒN credentials (priority order):
  1. AWS Secrets Manager: $SECRET_ID (profile: $AWS_PROFILE, region: $AWS_REGION)
  2. Fallback: $ENV_FILE (sao chép từ $ENV_EXAMPLE)

ENVIRONMENT:
  AWS_PROFILE   AWS CLI profile (default: dev-admin)
  AWS_REGION    AWS region (default: ap-southeast-1)
  SECRET_ID     Secret name (default: kite/dev/smoke-creds)
  ENV_FILE      Fallback env file (default: .env.test)
EOF
}

log_info() { echo "[INFO] $*" >&2; }
log_warn() { echo "[WARN] $*" >&2; }
log_error() { echo "[ERROR] $*" >&2; }

for arg in "$@"; do
  case "$arg" in
    --help|-h) usage; exit 0 ;;
    *) log_error "Unknown arg: $arg"; usage; exit 1 ;;
  esac
done

# ---------- Source 1: AWS Secrets Manager ----------
try_aws_secret() {
  if ! command -v aws >/dev/null 2>&1; then
    log_warn "AWS CLI không cài đặt — skip Secrets Manager."
    return 1
  fi

  log_info "Thử đọc AWS secret: $SECRET_ID (profile=$AWS_PROFILE, region=$AWS_REGION)..."
  local result
  if result=$(aws secretsmanager get-secret-value \
        --secret-id "$SECRET_ID" \
        --profile "$AWS_PROFILE" \
        --region "$AWS_REGION" \
        --query SecretString \
        --output text 2>&1); then
    echo "$result"
    return 0
  else
    log_warn "Không đọc được secret: $result"
    return 1
  fi
}

# ---------- Source 2: .env.test fallback ----------
try_env_file() {
  if [[ ! -f "$ENV_FILE" ]]; then
    log_warn "$ENV_FILE chưa tồn tại."
    if [[ -f "$ENV_EXAMPLE" ]]; then
      cat <<EOF >&2

============================================================
Để tạo $ENV_FILE, copy từ template:

  cp $ENV_EXAMPLE $ENV_FILE
  # Sau đó edit $ENV_FILE — fill in giá trị thực tế

============================================================
EOF
    fi
    return 1
  fi

  # shellcheck disable=SC1090
  source "$ENV_FILE"
  return 0
}

# ---------- Render credentials table ----------
render_table() {
  cat <<EOF

============================================================
Dev Self-Test Credentials  (Wave 87 Bucket A)
============================================================

| Role              | Email                                    | Password                |
|-------------------|------------------------------------------|-------------------------|
| PLATFORM_ADMIN    | ${ADMIN_EMAIL:-admin@kitehub.test}             | ${ADMIN_PASSWORD:-<unset>} |
| P2_CENTER_OWNER   | ${OWNER_EMAIL:-owner@sky-education.test}       | ${OWNER_PASSWORD:-<unset>} |
| TEACHER (×2)      | teacher{1,2}@${TENANT_SLUG:-sky-education}.test       | ${TEACHER_PASSWORD:-<unset>} |
| PARENT (×3)       | parent{1,2,3}@${TENANT_SLUG:-sky-education}.test      | ${PARENT_PASSWORD:-<unset>} |

Tenant slug:        ${TENANT_SLUG:-sky-education}
Resend test inbox:  ${RESEND_TEST_INBOX_URL:-https://resend.com/emails  (cập nhật khi setup)}

NOTE: chỉ dùng cho local dev stack, KHÔNG dùng production.
Source: $CREDS_SOURCE

============================================================
EOF
}

# ---------- Main ----------
main() {
  CREDS_SOURCE="(unknown)"

  if secret_json=$(try_aws_secret); then
    CREDS_SOURCE="AWS Secrets Manager ($SECRET_ID)"
    # Parse JSON keys → bash vars (yêu cầu jq)
    if command -v jq >/dev/null 2>&1; then
      ADMIN_EMAIL=$(jq -r '.ADMIN_EMAIL // empty' <<<"$secret_json")
      ADMIN_PASSWORD=$(jq -r '.ADMIN_PASSWORD // empty' <<<"$secret_json")
      OWNER_EMAIL=$(jq -r '.OWNER_EMAIL // empty' <<<"$secret_json")
      OWNER_PASSWORD=$(jq -r '.OWNER_PASSWORD // empty' <<<"$secret_json")
      TEACHER_PASSWORD=$(jq -r '.TEACHER_PASSWORD // empty' <<<"$secret_json")
      PARENT_PASSWORD=$(jq -r '.PARENT_PASSWORD // empty' <<<"$secret_json")
      TENANT_SLUG=$(jq -r '.TENANT_SLUG // empty' <<<"$secret_json")
      RESEND_TEST_INBOX_URL=$(jq -r '.RESEND_TEST_INBOX_URL // empty' <<<"$secret_json")
    else
      log_warn "jq chưa cài đặt — không parse được secret JSON. Fallback env file."
      try_env_file || true
      CREDS_SOURCE="$ENV_FILE (jq missing, AWS read OK nhưng không parse)"
    fi
  elif try_env_file; then
    CREDS_SOURCE="$ENV_FILE"
  else
    log_error "Không có nguồn credentials nào available."
    log_error "Tạo $ENV_FILE từ $ENV_EXAMPLE HOẶC tạo AWS secret $SECRET_ID."
    exit 2
  fi

  render_table
}

main "$@"
