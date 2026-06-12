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
# Two complementary checks (both run every invocation):
#
#   CHECK A (original) — "MISSING production override":
#     default = dev placeholder (localhost / mock / kite-mailhog / dev hostnames)
#     that production does NOT override → PRODUCTION breaks.
#     Cross-checks docker-compose.production.yml + fetch-secrets.sh.
#
#   CHECK B (GAP-802 cơ chế #5) — "LOCAL dead-link risk":
#     default = PRODUCTION domain (kitehub.me / kitehub.me) that the LOCAL
#     dev compose (kitehub/docker-compose.kitehub.yml) does NOT override →
#     LOCAL emails/links embed prod domain → dead-link when testing locally.
#     This is the INVERSE direction of CHECK A. Closes GAP-801 part 3 class:
#       `kitehub.beta.signup-base-url:${...:https://kitehub.me}` default ships
#       prod domain; local dev sends emails with dead links to prod.
#     Cross-checks kitehub/docker-compose.kitehub.yml (LOCAL compose).
#
# Exit codes:
#   0 — all suspect defaults overridden OR explicitly marked acceptable
#       (CHECK B local-deadlink findings are WARN-only by default)
#   1 — CHECK A: missing override → production would use placeholder default
#       OR (with --strict-local) CHECK B local-deadlink findings present
#
# Usage:
#   bash scripts/audit-env-coverage.sh                  # check (local-deadlink = WARN)
#   bash scripts/audit-env-coverage.sh --json           # machine-readable
#   bash scripts/audit-env-coverage.sh --strict-local   # local-deadlink also → exit 1
#   bash scripts/audit-env-coverage.sh --json --strict-local

set -euo pipefail

# Parse flags (order-independent). --json = machine output, --strict-local =
# escalate CHECK B local-deadlink findings to exit 1.
JSON=0
STRICT_LOCAL=0
for arg in "$@"; do
  case "$arg" in
    --json)         JSON=1 ;;
    --strict-local) STRICT_LOCAL=1 ;;
    *) echo "Unknown flag: $arg" >&2; echo "Usage: $0 [--json] [--strict-local]" >&2; exit 2 ;;
  esac
done

# AUDIT_ROOT override allows fixture-based unit tests to point at synthetic repo layout
# (per `scripts/tests/test-audit-env-coverage.sh`). Default = real repo root.
ROOT="${AUDIT_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"

# CHECK A — Suspect default patterns (production would break with these):
SUSPECT_PATTERNS='localhost|mock|noreply@localhost|noreply@kitehub\.local|kite-mailhog|http://kitehub-frontend|http://kiteclass-frontend'

# CHECK B (GAP-802 cơ chế #5) — Production-domain default patterns. A ${VAR:default}
# whose default points at a real production domain is a LOCAL dead-link risk unless
# the local dev compose overrides it. Configurable.
PROD_DOMAIN_PATTERNS='kitehub\.me|kiteclass\.vn'

# Files to scan. find may exit non-zero if optional dir missing (e.g., fixture tests
# without kiteclass/); tolerate via `|| true` so `set -e` doesn't abort script.
YAMLS=$( { find "$ROOT/kitehub" "$ROOT/kiteclass" -path '*/src/main/resources/application*.yml' 2>/dev/null || true; } | sort)
# GAP-802 cơ chế #5: also scan Java @Value("${...}") annotations — the GAP-801 part 3
# signup-base-url prod-domain default lives in a @Value, not in application.yml.
JAVA_SRCS=$( { find "$ROOT/kitehub" "$ROOT/kiteclass" -path '*/src/main/java/*.java' 2>/dev/null || true; } | sort)
COMPOSE="$ROOT/docker-compose.production.yml"
FETCH_SECRETS="$ROOT/scripts/fetch-secrets.sh"
# GAP-802 cơ chế #5: LOCAL dev compose (distinct from production compose above).
LOCAL_COMPOSE="$ROOT/kitehub/docker-compose.kitehub.yml"

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

# GAP-802 cơ chế #5: prod-domain defaults intentionally pointing at production at
# every env (no local override needed) — exempt from CHECK B local-deadlink flag.
# Default empty: every prod-domain default flagged for review (conservative). Add a
# var here only with rationale citing why local-points-to-prod is CORRECT for it.
ACCEPTABLE_PROD_DOMAINS=(
  # (none yet — all prod-domain defaults flagged for review by design)
)

is_acceptable() {
  local var="$1"
  for ok in "${ACCEPTABLE_DEFAULTS[@]}"; do
    [[ "$var" == "$ok" ]] && return 0
  done
  return 1
}

is_acceptable_prod_domain() {
  local var="$1"
  for ok in "${ACCEPTABLE_PROD_DOMAINS[@]}"; do
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

# GAP-802 cơ chế #5: is the var overridden in the LOCAL dev compose? If yes, local
# uses a local-friendly value (e.g., http://localhost:3000) instead of the prod-domain
# default → no dead-link risk. Checks `kitehub/docker-compose.kitehub.yml` env blocks.
is_overridden_local() {
  local var="$1"
  # YAML map style:  KEY: value   (environment: block in local compose)
  grep -qE "^\s*${var}:\s+" "$LOCAL_COMPOSE" 2>/dev/null && return 0
  # KEY=value style (rare in compose env list form)
  grep -qE "^\s*${var}=" "$LOCAL_COMPOSE" 2>/dev/null && return 0
  return 1
}

declare -a FINDINGS_MISSING=()
declare -a FINDINGS_OK=()
declare -a FINDINGS_ACCEPTED=()
# GAP-802 cơ chế #5: separate bucket for LOCAL dead-link findings (CHECK B).
declare -a FINDINGS_LOCAL_DEADLINK=()

# Helper: classify a single ${VAR:default} occurrence. Used by both YAML and Java scans.
classify_prod_domain() {
  local rel="$1" line="$2" var="$3" default="$4"
  # CHECK B (GAP-802 cơ chế #5): default points at a production domain?
  if [[ "$default" =~ $PROD_DOMAIN_PATTERNS ]]; then
    if is_acceptable_prod_domain "$var"; then
      : # intentionally points at prod at every env — exempt
    elif is_overridden_local "$var"; then
      : # local compose overrides → no dead-link risk
    else
      FINDINGS_LOCAL_DEADLINK+=("$rel:$line $var=$default (LOCAL → prod domain, no local override)")
    fi
  fi
}

# === Scan YAML files ===
# Combined pattern: match a ${VAR:default} whose default contains EITHER a CHECK A
# suspect token OR a CHECK B prod-domain token. We then re-classify per-line.
for yaml in $YAMLS; do
  rel="${yaml#$ROOT/}"
  while IFS='|' read -r line var default; do
    [[ -z "$var" ]] && continue
    # CHECK B classification (prod-domain → local dead-link) — independent of CHECK A.
    classify_prod_domain "$rel" "$line" "$var" "$default"
    # CHECK A classification (suspect placeholder → missing production override).
    if [[ "$default" =~ $SUSPECT_PATTERNS ]]; then
      if is_acceptable "$var"; then
        FINDINGS_ACCEPTED+=("$rel:$line $var=$default (ACCEPTED)")
      elif is_overridden "$var"; then
        FINDINGS_OK+=("$rel:$line $var=$default (overridden)")
      else
        FINDINGS_MISSING+=("$rel:$line $var=$default")
      fi
    fi
  done < <(grep -nE "\\\$\\{[A-Z_][A-Z0-9_]*:[^}]*(${SUSPECT_PATTERNS}|${PROD_DOMAIN_PATTERNS})[^}]*\\}" "$yaml" 2>/dev/null \
    | sed -E 's/^([0-9]+):.*\$\{([A-Z_][A-Z0-9_]*):([^}]*)\}.*/\1|\2|\3/' \
    | head -20)
done

# === Scan Java @Value annotations (GAP-802 cơ chế #5 — prod-domain only) ===
# The signup-base-url prod-domain default (GAP-801 part 3) lives in a @Value, not yml.
# @Value uses ${dotted.config.key:default}. We map dotted key → SCREAMING_SNAKE_CASE
# env var (Spring relaxed binding) for the local-override lookup. Only CHECK B runs here
# (CHECK A production-override semantics are yml/compose-scoped per original design).
for java in $JAVA_SRCS; do
  rel="${java#$ROOT/}"
  while IFS='|' read -r line key default; do
    [[ -z "$key" ]] && continue
    # Map Spring dotted/kebab key → env var: dots/hyphens → underscore, uppercase.
    var=$(echo "$key" | tr '.-' '__' | tr '[:lower:]' '[:upper:]')
    classify_prod_domain "$rel" "$line" "$var" "$default"
  done < <(grep -nE "@Value\(\"\\\$\\{[a-zA-Z0-9_.-]+:[^}]*(${PROD_DOMAIN_PATTERNS})[^}]*\\}\"\)" "$java" 2>/dev/null \
    | sed -E 's/^([0-9]+):.*\$\{([a-zA-Z0-9_.-]+):([^}]*)\}.*/\1|\2|\3/' \
    | head -20)
done

MISSING_COUNT=${#FINDINGS_MISSING[@]}
OK_COUNT=${#FINDINGS_OK[@]}
ACCEPTED_COUNT=${#FINDINGS_ACCEPTED[@]}
LOCAL_DEADLINK_COUNT=${#FINDINGS_LOCAL_DEADLINK[@]}

# Exit code: CHECK A missing-production always → exit 1. CHECK B local-deadlink → exit 1
# only with --strict-local; otherwise WARN-only.
compute_exit() {
  if [[ $MISSING_COUNT -gt 0 ]]; then echo 1; return; fi
  if [[ $STRICT_LOCAL -eq 1 && $LOCAL_DEADLINK_COUNT -gt 0 ]]; then echo 1; return; fi
  echo 0
}

# Output
if [[ $JSON -eq 1 ]]; then
  printf '{"missing":%d,"ok":%d,"accepted":%d,"local_deadlink":%d}\n' \
    "$MISSING_COUNT" "$OK_COUNT" "$ACCEPTED_COUNT" "$LOCAL_DEADLINK_COUNT"
  exit "$(compute_exit)"
fi

echo "=== Production env coverage audit ==="
echo

if [[ $MISSING_COUNT -gt 0 ]]; then
  echo "❌ MISSING production override ($MISSING_COUNT findings) [CHECK A — prod breaks]:"
  printf '   %s\n' "${FINDINGS_MISSING[@]}"
  echo
fi

# GAP-802 cơ chế #5: LOCAL dead-link findings (inverse direction of CHECK A).
if [[ $LOCAL_DEADLINK_COUNT -gt 0 ]]; then
  if [[ $STRICT_LOCAL -eq 1 ]]; then
    echo "❌ LOCAL dead-link risk ($LOCAL_DEADLINK_COUNT findings) [CHECK B — local → prod domain, --strict-local]:"
  else
    echo "⚠️  LOCAL dead-link risk ($LOCAL_DEADLINK_COUNT findings) [CHECK B — local → prod domain, WARN]:"
  fi
  printf '   %s\n' "${FINDINGS_LOCAL_DEADLINK[@]}"
  echo "   Fix: add the var to kitehub/docker-compose.kitehub.yml env block with a local value"
  echo "        (e.g., http://localhost:3000), OR add to ACCEPTABLE_PROD_DOMAINS if local-points-to-prod is intended."
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

if [[ $STRICT_LOCAL -eq 1 && $LOCAL_DEADLINK_COUNT -gt 0 ]]; then
  echo "FAIL (--strict-local): $LOCAL_DEADLINK_COUNT local dead-link risk(s)."
  echo "Fix: override the var in kitehub/docker-compose.kitehub.yml OR add to ACCEPTABLE_PROD_DOMAINS."
  exit 1
fi

if [[ $LOCAL_DEADLINK_COUNT -gt 0 ]]; then
  echo "PASS (with WARN): production overrides OK; $LOCAL_DEADLINK_COUNT local dead-link risk(s) — review (run --strict-local to enforce)."
else
  echo "PASS: all suspect defaults overridden or accepted; no local dead-link risk."
fi
exit 0
