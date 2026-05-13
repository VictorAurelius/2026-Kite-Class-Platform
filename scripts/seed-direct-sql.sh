#!/usr/bin/env bash
#
# seed-direct-sql.sh — Wave 67 GAP-500 Path B (direct SQL seed, bypass Spring)
#
# Runs ON kh-backend EC2 via SSM send-command. Pure psql + bcrypt; bypasses
# ProductionSeedRunner per GAP-500 retry budget exhaustion (5 retries failed).
#
# Idempotent: ON CONFLICT (id) DO NOTHING. Safe re-run.
#
# Per .claude/rules/agent-aws-access.md §4 + release-deploy-standard.md §9 —
# user-triggered via workflow_dispatch confirm=SEED.

set -euo pipefail

REGION="ap-southeast-1"
ENV_FILE="/etc/kite/.env"
LOG="${LOG:-/var/log/kite-seed-direct.log}"
SEED_ADMIN_EMAIL="${SEED_ADMIN_EMAIL:-admin@kitehub.me}"
SEED_ADMIN_ID="00000000-0000-0000-0000-000000000001"
SEED_ADMIN_NAME="Platform Admin"
SEED_ADMIN_ROLE="PLATFORM_ADMIN"

log() { echo "[$(date -u +%FT%TZ)] $*" | sudo tee -a "$LOG"; }

log "==================== seed-direct-sql.sh START ===================="

# 1. Ensure tools installed
log "Ensuring postgresql client + python3 bcrypt installed..."
if ! command -v psql >/dev/null 2>&1; then
  log "Installing postgresql client..."
  sudo dnf install -y postgresql15 >/dev/null 2>&1 || sudo dnf install -y postgresql >/dev/null 2>&1
fi
if ! python3 -c "import bcrypt" 2>/dev/null; then
  log "Installing python3 bcrypt via pip..."
  sudo dnf install -y python3-pip >/dev/null 2>&1 || true
  sudo pip3 install --quiet bcrypt
fi
log "Tools ready: psql=$(command -v psql), python3-bcrypt installed"

# 2. Source DB credentials from /etc/kite/.env (populated by fetch-secrets.sh)
if [[ ! -f "$ENV_FILE" ]]; then
  log "ERROR: $ENV_FILE missing — run deploy first to populate"
  exit 1
fi
set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a
log "DB target: $DB_HOST:$DB_PORT/$DB_NAME (user=$DB_USERNAME)"

# 3. Fetch seed-admin-password from Secrets Manager
log "Fetching kitehub/production/seed-admin-password..."
SEED_ADMIN_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id kitehub/production/seed-admin-password \
  --region "$REGION" --query SecretString --output text 2>/dev/null) || {
    log "ERROR: failed to fetch seed-admin-password"
    exit 1
  }
[[ -z "$SEED_ADMIN_PASSWORD" || "$SEED_ADMIN_PASSWORD" == "null" ]] && {
  log "ERROR: seed-admin-password empty/null"
  exit 1
}
log "Password fetched (length=${#SEED_ADMIN_PASSWORD})"

# 4. Compute bcrypt hash (strength 12, matches BCryptPasswordEncoder default)
log "Computing bcrypt hash (strength 12)..."
HASH=$(SEED_PASS="$SEED_ADMIN_PASSWORD" python3 -c "
import os, bcrypt
pw = os.environ['SEED_PASS'].encode('utf-8')
print(bcrypt.hashpw(pw, bcrypt.gensalt(12)).decode('utf-8'))
")
[[ -z "$HASH" || ${#HASH} -lt 50 ]] && {
  log "ERROR: bcrypt hash invalid (length=${#HASH})"
  exit 1
}
log "Hash computed (length=${#HASH}, prefix=${HASH:0:7})"

# 5. INSERT admin user (idempotent — ON CONFLICT by email per V9 UNIQUE constraint)
log "Inserting admin user: $SEED_ADMIN_EMAIL ($SEED_ADMIN_ROLE)..."
export PGPASSWORD="$DB_PASSWORD"
INSERT_OUT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -tA -v ON_ERROR_STOP=1 <<SQL
INSERT INTO users (id, email, name, password_hash, role)
VALUES (
    '$SEED_ADMIN_ID',
    '$SEED_ADMIN_EMAIL',
    '$SEED_ADMIN_NAME',
    '$HASH',
    '$SEED_ADMIN_ROLE'
)
ON CONFLICT (email) DO NOTHING
RETURNING id, email, role;
SQL
)
unset PGPASSWORD

if [[ -n "$INSERT_OUT" ]]; then
  log "✅ Admin user INSERTED: $INSERT_OUT"
else
  log "⚠️  Admin user already exists (ON CONFLICT skipped) — verifying via SELECT..."
fi

# 6. Verify via SELECT
log "Verification — querying users + system_config + tenant config..."
export PGPASSWORD="$DB_PASSWORD"
VERIFY=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -tA -v ON_ERROR_STOP=1 <<SQL
SELECT 'admin_users', COUNT(*) FROM users WHERE role = 'PLATFORM_ADMIN' AND email = '$SEED_ADMIN_EMAIL'
UNION ALL SELECT 'system_config_rows', COUNT(*) FROM system_config
UNION ALL SELECT 'platform_tenant_config', COUNT(*) FROM system_config WHERE config_key = 'platform_tenant_id';
SQL
)
unset PGPASSWORD

log "Verification results:"
echo "$VERIFY" | sudo tee -a "$LOG"

# Check expected counts
ADMIN_COUNT=$(echo "$VERIFY" | grep "^admin_users" | cut -f2 -d'|')
CONFIG_COUNT=$(echo "$VERIFY" | grep "^system_config_rows" | cut -f2 -d'|')
TENANT_COUNT=$(echo "$VERIFY" | grep "^platform_tenant_config" | cut -f2 -d'|')

if [[ "${ADMIN_COUNT:-0}" -ge 1 ]] && [[ "${CONFIG_COUNT:-0}" -ge 3 ]] && [[ "${TENANT_COUNT:-0}" -ge 1 ]]; then
  log "✅ All counts pass — admin=$ADMIN_COUNT, config=$CONFIG_COUNT, tenant=$TENANT_COUNT"
  log "==================== seed-direct-sql.sh DONE ===================="
  exit 0
else
  log "❌ Verification FAILED — admin=$ADMIN_COUNT (need ≥1), config=$CONFIG_COUNT (need ≥3), tenant=$TENANT_COUNT (need ≥1)"
  log "==================== seed-direct-sql.sh FAILED (verify) ===================="
  exit 1
fi
