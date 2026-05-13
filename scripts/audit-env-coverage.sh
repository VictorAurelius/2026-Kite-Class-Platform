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
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Suspect default patterns (production would break with these):
SUSPECT_PATTERNS='localhost|mock|noreply@localhost|noreply@kitehub\.local|kite-mailhog|http://kitehub-frontend|http://kiteclass-frontend'

# Files to scan
YAMLS=$(find "$ROOT/kitehub" "$ROOT/kiteclass" -path '*/src/main/resources/application*.yml' 2>/dev/null | sort)
COMPOSE="$ROOT/docker-compose.production.yml"
FETCH_SECRETS="$ROOT/scripts/fetch-secrets.sh"

# Vars known acceptable to leave at default (e.g. observability when not provisioned)
ACCEPTABLE_DEFAULTS=(
  "OTEL_EXPORTER_OTLP_ENDPOINT"   # No OTel collector deployed Phase 1 BETA
  "AI_OLLAMA_BASE_URL"            # AI deferred Phase 2 (ADR-026)
  "PAYMENT_RETURN_URL"            # Payment deferred Phase 1.5
  "PAYMENT_NOTIFY_URL"            # Payment deferred Phase 1.5
  "SMTP_HOST"                     # Resend used, not SMTP
  "SMTP_PORT"                     # Resend used, not SMTP
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
  return 1
}

declare -a FINDINGS_MISSING
declare -a FINDINGS_OK
declare -a FINDINGS_ACCEPTED

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
if [[ ${#FINDINGS_MISSING[@]} -gt 0 ]]; then
  echo "❌ MISSING production override (${#FINDINGS_MISSING[@]} findings):"
  printf '   %s\n' "${FINDINGS_MISSING[@]}"
  echo
fi
echo "✅ OK overridden (${#FINDINGS_OK[@]}):"
printf '   %s\n' "${FINDINGS_OK[@]}" | head -10
[[ ${#FINDINGS_OK[@]} -gt 10 ]] && echo "   ... (${#FINDINGS_OK[@]} total)"
echo
echo "⚠️  Accepted-default (${#FINDINGS_ACCEPTED[@]}):"
printf '   %s\n' "${FINDINGS_ACCEPTED[@]}" | head -10
echo

if [[ ${#FINDINGS_MISSING[@]} -gt 0 ]]; then
  echo "FAIL: ${#FINDINGS_MISSING[@]} production env override(s) missing."
  echo "Fix: add to docker-compose.production.yml or fetch-secrets.sh."
  echo "OR if intentional, add to ACCEPTABLE_DEFAULTS in this script + registry doc."
  exit 1
fi

echo "PASS: all suspect defaults overridden or accepted."
exit 0
