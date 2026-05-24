#!/usr/bin/env bash
# audit-env-coverage.sh — production env-var coverage audit
#
# Scans application*.yml for ${VAR:default} patterns where default is a
# placeholder (localhost, mock, noreply@localhost, etc.). Cross-checks
# against docker-compose.production.yml overrides + fetch-secrets.sh
# writes + registry doc.
#
# Per `.claude/rules/production-env-config-registry.md` v1.0.0.
#
# Exit codes:
#   0 — all suspect defaults overridden OR explicitly marked acceptable
#   1 — missing override → production would use placeholder default
#
# Usage:
#   bash scripts/audit-env-coverage.sh                  # check
#   bash scripts/audit-env-coverage.sh --json           # machine-readable

set -euo pipefail
# AUDIT_ROOT override allows fixture-based unit tests to point at synthetic repo layout
# (per `scripts/tests/test-audit-env-coverage.sh`). Default = real repo root.
ROOT="${AUDIT_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"

# Suspect default patterns (production would break with these):
SUSPECT_PATTERNS='localhost|mock|noreply@localhost|noreply@kitehub\.local|kite-mailhog|http://kitehub-frontend|http://kiteclass-frontend'

# Files to scan. find may exit non-zero if optional dir missing (e.g., fixture tests
# without kiteclass/); tolerate via `|| true` so `set -e` doesn't abort script.
YAMLS=$( { find "$ROOT/kitehub" "$ROOT/kiteclass" -path '*/src/main/resources/application*.yml' 2>/dev/null || true; } | sort)
COMPOSE="$ROOT/docker-compose.production.yml"
FETCH_SECRETS="$ROOT/scripts/fetch-secrets.sh"

# Vars known acceptable to leave at default. Per `production-env-config-registry.md` §5
# acceptable-default exception conditions. Each row PHẢI có rationale citing:
#   - Feature deferred (which phase / ADR / GAP)
#   - Mechanism unused (which alternative path used)
#   - Observability not provisioned (which gap tracks)
# Wave br-4 Bucket A (GAP-508 Phase 3) — eliminated 10 false positives reducing
# audit noise from 16→6 actionable findings.
ACCEPTABLE_DEFAULTS=(
  # === Observability — deferred Phase 1.5+ (GAP-115 backlog) ===
  "OTEL_EXPORTER_OTLP_ENDPOINT"   # No OTel collector deployed Phase 1 BETA per GAP-115
  # === AI features — Phase 2 per ADR-026 ===
  "AI_OLLAMA_BASE_URL"            # AI deferred Phase 2 (ADR-026)
  "OPENAI_API_KEY"                # AI Phase 2 fallback (kitehub-branding mock OK Phase 1 BETA)
  # === Payment — Phase 1.5 deferred per release-1-deploy-plan ===
  "PAYMENT_RETURN_URL"            # Payment deferred Phase 1.5
  "PAYMENT_NOTIFY_URL"            # Payment deferred Phase 1.5
  # === SMTP — Resend HTTP API used (ADR-025 Stream A) ===
  "SMTP_HOST"                     # Resend HTTP API used, not SMTP (ADR-025 Stream A)
  "SMTP_PORT"                     # Same as above
  # === S3 endpoint — Phase 1 BETA assets via Vercel; MinIO local-only ===
  "S3_ENDPOINT"                   # Phase 1 BETA uses native AWS S3 (no endpoint override); MinIO local-dev only
  # === AWS native credentials — never in app config production ===
  "AWS_ACCESS_KEY_ID"             # Production: EC2 instance profile via IMDSv2 (no static key per `agent-aws-access.md`); branding mock OK
  "AWS_SECRET_ACCESS_KEY"         # Same as above
  # === CDN — Phase 1 BETA assets via Vercel; CDN deferred ===
  "CDN_DOMAIN"                    # Phase 1 BETA assets via Vercel; CDN deferred (GAP-371 backlog)
  # === Admin / master DB — bootstrap path via psql direct, not app runtime ===
  "DATABASE_MASTER_HOST"          # Admin bootstrap path; production uses managed RDS, admin SQL via psql direct
  "DATABASE_ADMIN_URL"            # Same as above (per `env-vars-registry.md` accepted defaults table row 7)
)

is_acceptable() {
  local var="$1"
  for ok in "${ACCEPTABLE_DEFAULTS[@]}"; do
    [[ "$var" == "$ok" ]] && return 0
  done
  return 1
}

is_overridden() {
  local var="$1"
  # Check docker-compose production env block
  grep -qE "^\s*${var}:\s+" "$COMPOSE" 2>/dev/null && return 0
  # Check fetch-secrets.sh writes this var
  grep -qE "^${var}=" "$FETCH_SECRETS" 2>/dev/null && return 0
  # Spring-relaxed binding aliases — Spring resolves these pairs automatically.
  # Wave br-4 Bucket A: removed 8 false positives by treating bound-pair as override.
  local alias=""
  case "$var" in
    DATABASE_URL)        alias="SPRING_DATASOURCE_URL" ;;
    RABBITMQ_HOST)       alias="SPRING_RABBITMQ_HOST" ;;
    SPRING_REDIS_HOST)   alias="SPRING_DATA_REDIS_HOST" ;;
    STORAGE_S3_ENDPOINT) alias="S3_ENDPOINT" ;;
  esac
  if [[ -n "$alias" ]]; then
    grep -qE "^${alias}=" "$FETCH_SECRETS" 2>/dev/null && return 0
    grep -qE "^\s*${alias}:\s+" "$COMPOSE" 2>/dev/null && return 0
    # Direct overridden alias also acceptable
    is_acceptable "$alias" && return 0
  fi
  return 1
}

declare -a FINDINGS_MISSING=()
declare -a FINDINGS_OK=()
declare -a FINDINGS_ACCEPTED=()

for yaml in $YAMLS; do
  rel="${yaml#$ROOT/}"
  # Extract ${VAR:default} where default matches suspect pattern
  while IFS='|' read -r line var default; do
    [[ -z "$var" ]] && continue
    if is_acceptable "$var"; then
      FINDINGS_ACCEPTED+=("$rel:$line $var=$default (ACCEPTED)")
    elif is_overridden "$var"; then
      FINDINGS_OK+=("$rel:$line $var=$default (overridden)")
    else
      FINDINGS_MISSING+=("$rel:$line $var=$default")
    fi
  done < <(grep -nE "\\\$\\{[A-Z_][A-Z0-9_]*:[^}]*(${SUSPECT_PATTERNS})[^}]*\\}" "$yaml" 2>/dev/null \
    | sed -E 's/^([0-9]+):.*\$\{([A-Z_][A-Z0-9_]*):([^}]*)\}.*/\1|\2|\3/' \
    | head -20)
done

# Output
if [[ "${1:-}" == "--json" ]]; then
  printf '{"missing":%d,"ok":%d,"accepted":%d}\n' \
    "${#FINDINGS_MISSING[@]}" "${#FINDINGS_OK[@]}" "${#FINDINGS_ACCEPTED[@]}"
  exit $([ ${#FINDINGS_MISSING[@]} -eq 0 ] && echo 0 || echo 1)
fi

echo "=== Production env coverage audit ==="
echo
MISSING_COUNT=${#FINDINGS_MISSING[@]}
OK_COUNT=${#FINDINGS_OK[@]}
ACCEPTED_COUNT=${#FINDINGS_ACCEPTED[@]}

if [[ $MISSING_COUNT -gt 0 ]]; then
  echo "❌ MISSING production override ($MISSING_COUNT findings):"
  printf '   %s\n' "${FINDINGS_MISSING[@]}"
  echo
fi
echo "✅ OK overridden ($OK_COUNT):"
if [[ $OK_COUNT -gt 0 ]]; then
  printf '   %s\n' "${FINDINGS_OK[@]}" | head -10
  [[ $OK_COUNT -gt 10 ]] && echo "   ... ($OK_COUNT total)"
fi
echo
echo "⚠️  Accepted-default ($ACCEPTED_COUNT):"
if [[ $ACCEPTED_COUNT -gt 0 ]]; then
  printf '   %s\n' "${FINDINGS_ACCEPTED[@]}" | head -10
fi
echo

if [[ $MISSING_COUNT -gt 0 ]]; then
  echo "FAIL: $MISSING_COUNT production env override(s) missing."
  echo "Fix: add to docker-compose.production.yml or fetch-secrets.sh."
  echo "OR if intentional, add to ACCEPTABLE_DEFAULTS in this script + registry doc."
  exit 1
fi

echo "PASS: all suspect defaults overridden or accepted."
exit 0
