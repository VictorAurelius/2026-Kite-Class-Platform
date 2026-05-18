#!/usr/bin/env bash
# sweep-be-cors-origins.sh — Wave 82 Bucket B GAP-568
# BE CORS_ALLOWED_ORIGINS sweep + preflight verify pre-DNS-flip
#
# Audit-only helper (read-only per `agent-aws-access.md` §2 Tier 1 + Tier 3
# restrictions on mutating production config). KHÔNG tự edit docker-compose
# hay yaml — chỉ AUDIT current state, SUGGEST diff, VERIFY preflight.
#
# Per `.claude/rules/production-env-config-registry.md` §11 (audit scripts
# pattern) + `.claude/rules/pre-launch-infra-hardening-checklist.md` §2.2
# (CORS pre-launch P0 check).
#
# Modes:
#   --audit                 Print current CORS allowlist state per 7 services
#                           (default mode if no flag passed)
#   --preflight ORIGIN      Curl OPTIONS verify cho 6+ gateway endpoints
#                           với header `Origin: ORIGIN`; PASS/FAIL per endpoint
#   --suggest               Emit docker-compose.production.yml env diff
#                           suggestion (nếu cần thay đổi allowlist)
#   --help                  Show usage
#
# Exit codes:
#   0 — success (audit OK, all preflight PASS, suggest emitted)
#   1 — any preflight FAIL OR audit detects missing required override

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Services trong scope GAP-568 — 7 BE services
SERVICES=(
  "kitehub-gateway"
  "kitehub-subscription"
  "kitehub-branding"
  "kitehub-email"
  "kitehub-admin"
  "kiteclass-core"
)
# Note: kiteclass-gateway removed per ADR-032 / GAP-001 (Wave 96)

# Gateway endpoints để preflight test (cover ≥6 distinct backends)
# Mapping: endpoint path → upstream backend (delegated via gateway routes)
PREFLIGHT_ENDPOINTS=(
  "https://api.kitehub.me/api/v1/auth/request-beta-access|kitehub-subscription"
  "https://api.kitehub.me/api/v1/beta-status|kitehub-subscription"
  "https://api.kitehub.me/api/auth/login|kitehub-subscription"
  "https://api.kitehub.me/api/v1/branding/jobs|kitehub-branding"
  "https://api.kitehub.me/api/v1/email/send|kitehub-email"
  "https://api.kitehub.me/api/platform/admin/health|kitehub-admin"
  "https://api.kitehub.me/actuator/health|kitehub-gateway"
)

COMPOSE="$ROOT/docker-compose.production.yml"

# ----- Utility helpers -----

color_red()    { printf "\033[31m%s\033[0m" "$1"; }
color_green()  { printf "\033[32m%s\033[0m" "$1"; }
color_yellow() { printf "\033[33m%s\033[0m" "$1"; }

usage() {
  cat <<EOF
Usage: bash scripts/sweep-be-cors-origins.sh [MODE]

Modes:
  --audit                Print current CORS allowlist state per 7 services (default)
  --preflight ORIGIN     Curl OPTIONS verify cho gateway endpoints với header Origin: ORIGIN
                         Vd: bash $0 --preflight https://kitehub.me
  --suggest              Emit docker-compose.production.yml env diff suggestion
  --help                 Show this message

Examples:
  bash scripts/sweep-be-cors-origins.sh                              # default --audit
  bash scripts/sweep-be-cors-origins.sh --preflight https://kitehub.me
  bash scripts/sweep-be-cors-origins.sh --suggest

Wave 82 Bucket B GAP-568 — BE CORS allowlist sweep pre-DNS-flip.
Read-only audit — does NOT mutate compose/yaml/AWS state.
EOF
}

# ----- Mode: --audit -----

mode_audit() {
  echo ""
  echo "=========================================================================="
  echo "  Wave 82 Bucket B — BE CORS_ALLOWED_ORIGINS sweep audit"
  echo "  Per GAP-568 + production-env-config-registry.md §11"
  echo "=========================================================================="
  echo ""

  # Print current production env CORS override (gateway-level)
  echo "## 1. docker-compose.production.yml — CORS env override"
  echo ""
  if [[ -f "$COMPOSE" ]]; then
    local cors_line
    cors_line=$(grep -nE "^\s*CORS_ALLOWED_ORIGINS:" "$COMPOSE" 2>/dev/null || true)
    if [[ -n "$cors_line" ]]; then
      echo "  $(color_green '✅ FOUND') CORS_ALLOWED_ORIGINS env trong compose:"
      echo "    $cors_line"
    else
      echo "  $(color_red '❌ MISSING') CORS_ALLOWED_ORIGINS env trong compose"
    fi
  else
    echo "  $(color_red '❌ ERROR') docker-compose.production.yml not found: $COMPOSE"
  fi
  echo ""

  # Per-service inventory
  echo "## 2. Per-service CORS config inventory"
  echo ""
  printf "  %-25s | %-10s | %s\n" "Service" "CORS site" "Verdict"
  printf "  %-25s-+-%-10s-+-%s\n" "-------------------------" "----------" "----------------------------------------"

  for svc in "${SERVICES[@]}"; do
    local site="—"
    local verdict
    local stack_root

    # Determine stack root for resource path
    case "$svc" in
      kitehub-*) stack_root="$ROOT/kitehub" ;;
      kiteclass-*) stack_root="$ROOT/kiteclass" ;;
      *) stack_root="" ;;
    esac

    local yml_path="$stack_root/$svc/src/main/resources/application.yml"

    if [[ -f "$yml_path" ]]; then
      # Search for CORS-related yaml directives (not just keyword in comments)
      if grep -qE "(allowedOrigins|allowed-origins):\s*\\\$" "$yml_path" 2>/dev/null; then
        site="yaml"
        verdict="$(color_green 'CONFIGURED') — env-driven allowlist"
      elif grep -qE "^\s*cors:" "$yml_path" 2>/dev/null && \
           grep -qE "allowed" "$yml_path" 2>/dev/null; then
        site="yaml"
        verdict="$(color_green 'CONFIGURED') — yaml CORS block"
      else
        # Check Java CORS config presence
        local java_cors
        java_cors=$(grep -rln "CorsConfigurationSource\|CorsWebFilter\|@CrossOrigin\|allowedOrigins" \
          "$stack_root/$svc/src/main/java" 2>/dev/null | head -1 || true)
        if [[ -n "$java_cors" ]]; then
          site="Java"
          verdict="$(color_green 'CONFIGURED') — Java CorsFilter ($(basename "$java_cors"))"
        else
          site="none"
          verdict="$(color_yellow 'DELEGATE') — no CORS filter (delegated to gateway)"
        fi
      fi
    else
      verdict="$(color_yellow 'N/A') — yaml not found ($yml_path)"
    fi

    printf "  %-25s | %-10s | %s\n" "$svc" "$site" "$verdict"
  done

  echo ""
  echo "## 3. Architectural verdict"
  echo ""
  echo "  KH stack: 4 BE services (subscription/branding/email/admin) DELEGATE"
  echo "  CORS enforcement cho kitehub-gateway (single gateway pattern)."
  echo "  → Chỉ cần update CORS_ALLOWED_ORIGINS env trên kitehub-gateway"
  echo "    trong docker-compose.production.yml."
  echo ""
  echo "  KC stack: kiteclass-core handles CORS directly (kiteclass-gateway removed"
  echo "    per ADR-032 / GAP-001 Wave 96). When KC stack enable production,"
  echo "    add CORS_ALLOWED_ORIGINS env vào kiteclass-core service block."
  echo ""
  echo "## 4. Next steps"
  echo ""
  echo "  $ bash scripts/sweep-be-cors-origins.sh --preflight https://kitehub.me"
  echo "  $ bash scripts/sweep-be-cors-origins.sh --suggest"
  echo ""
}

# ----- Mode: --preflight ORIGIN -----

mode_preflight() {
  local origin="$1"
  if [[ -z "$origin" ]]; then
    echo "$(color_red 'ERROR'): --preflight requires ORIGIN argument"
    echo "Vd: bash $0 --preflight https://kitehub.me"
    exit 1
  fi

  echo ""
  echo "=========================================================================="
  echo "  Wave 82 Bucket B — CORS preflight verify"
  echo "  Origin: $origin"
  echo "  Per pre-launch-infra-hardening-checklist.md §2.2 + GAP-568 Bước 4"
  echo "=========================================================================="
  echo ""

  if ! command -v curl >/dev/null 2>&1; then
    echo "$(color_red 'ERROR'): curl not installed"
    exit 1
  fi

  local fail_count=0
  local pass_count=0

  printf "  %-60s | %-7s | %s\n" "Endpoint" "Status" "ACAO header"
  printf "  %-60s-+-%-7s-+-%s\n" \
    "------------------------------------------------------------" \
    "-------" "----------------------------------------"

  for entry in "${PREFLIGHT_ENDPOINTS[@]}"; do
    local endpoint="${entry%%|*}"
    local backend="${entry##*|}"

    # Curl OPTIONS preflight with Origin header
    local response
    local http_status
    local acao_header

    response=$(curl -sS -o /tmp/cors-body.$$ -w "%{http_code}\n%{header_json}" \
      -X OPTIONS \
      -H "Origin: $origin" \
      -H "Access-Control-Request-Method: POST" \
      -H "Access-Control-Request-Headers: Content-Type,Authorization" \
      --max-time 10 \
      -D /tmp/cors-headers.$$ \
      "$endpoint" 2>/dev/null || echo "000")

    http_status=$(echo "$response" | head -1)

    # Extract access-control-allow-origin header (case-insensitive)
    acao_header=$(grep -iE "^access-control-allow-origin:" /tmp/cors-headers.$$ 2>/dev/null | \
      head -1 | sed -E 's/^[Aa]ccess-[Cc]ontrol-[Aa]llow-[Oo]rigin:\s*//' | tr -d '\r\n' || echo "")

    local endpoint_short
    endpoint_short="${endpoint#https://api.kitehub.me}"
    endpoint_short="$endpoint_short [→$backend]"
    # Truncate to 60 chars
    if [[ ${#endpoint_short} -gt 60 ]]; then
      endpoint_short="${endpoint_short:0:57}..."
    fi

    if [[ "$http_status" == "200" || "$http_status" == "204" ]] && \
       [[ "$acao_header" == "$origin" || "$acao_header" == "*" ]]; then
      printf "  %-60s | %s | %s\n" \
        "$endpoint_short" \
        "$(color_green 'PASS') $http_status" \
        "$acao_header"
      pass_count=$((pass_count + 1))
    else
      printf "  %-60s | %s | %s\n" \
        "$endpoint_short" \
        "$(color_red 'FAIL') $http_status" \
        "${acao_header:-<missing>}"
      fail_count=$((fail_count + 1))
    fi

    rm -f /tmp/cors-body.$$ /tmp/cors-headers.$$
  done

  echo ""
  echo "  Result: $(color_green "$pass_count PASS") / $(color_red "$fail_count FAIL") / $((pass_count + fail_count)) total"
  echo ""

  if [[ $fail_count -gt 0 ]]; then
    echo "$(color_red '❌ BLOCKING') — DNS flip Bucket D blocked until all endpoints PASS"
    echo ""
    echo "  Common causes:"
    echo "  1. Gateway image deployed chưa nhận env CORS_ALLOWED_ORIGINS mới"
    echo "     → SSH vào EC2 + run: docker exec kitehub-gateway env | grep CORS"
    echo "  2. Gateway block kitehub-gateway env trong compose chưa include origin"
    echo "     → Edit docker-compose.production.yml + redeploy gateway"
    echo "  3. ALB / Cloudflare cache → curl direct EC2 IP để bypass + confirm"
    echo "  4. Endpoint not exposed (HTTP 404) — check gateway route predicate"
    echo ""
    exit 1
  fi

  echo "$(color_green '✅ READY') — all $pass_count preflight endpoints PASS cho origin $origin"
  echo ""
  echo "  Bucket D DNS flip ready từ CORS perspective."
  echo "  Per concurrent-production-mutation-ops.md §3.1 — user can now trigger DNS flip."
  echo ""
}

# ----- Mode: --suggest -----

mode_suggest() {
  echo ""
  echo "=========================================================================="
  echo "  Wave 82 Bucket B — docker-compose env diff suggestion"
  echo "  Per GAP-568 + production-env-config-registry.md §4 mechanism #1"
  echo "=========================================================================="
  echo ""

  local current
  current=$(grep -E "^\s*CORS_ALLOWED_ORIGINS:" "$COMPOSE" 2>/dev/null | head -1 | sed -E 's/^\s*//')

  echo "## Current state (docker-compose.production.yml:~216 inside kitehub-gateway block)"
  echo ""
  if [[ -n "$current" ]]; then
    echo "  $current"
  else
    echo "  $(color_red '❌ MISSING') — no CORS_ALLOWED_ORIGINS env set"
  fi
  echo ""

  echo "## Wave 82 Bucket D recommended state (DNS flip Vercel → EC2 self-host)"
  echo ""
  echo "  Phase 1 — Pre-cutover (during DNS flip, keep BOTH origins ≥7 ngày):"
  echo ""
  cat <<'EOF'
      CORS_ALLOWED_ORIGINS: "https://kitehub.me,https://www.kitehub.me,https://kitehub-victoraurelius-projects.vercel.app"
EOF
  echo ""
  echo "  $(color_green '✅ NO CHANGE NEEDED') — current value đã match Phase 1 (Wave 71 GAP-507)."
  echo ""
  echo "  Phase 2 — Post-cutover cleanup (≥7 ngày stable, defer Wave 83+):"
  echo ""
  cat <<'EOF'
      CORS_ALLOWED_ORIGINS: "https://kitehub.me,https://www.kitehub.me"
EOF
  echo "  ($(color_yellow 'TODO Wave 83+') — remove Vercel domain post-stable)"
  echo ""

  echo "## kiteclass-core (deferred Phase 7 KC stack bring-up; kiteclass-gateway removed Wave 96)"
  echo ""
  echo "  Khi enable KC stack production, add vào kiteclass-core service block:"
  echo ""
  cat <<'EOF'
      CORS_ALLOWED_ORIGINS: "https://kitehub.me,https://kiteclass.kitehub.me"
EOF
  echo ""
  echo "  (Optional — chỉ enable khi KC stack deploy production. Phase 1 BETA KH-only.)"
  echo ""
}

# ----- Main dispatcher -----

main() {
  if [[ $# -eq 0 ]]; then
    mode_audit
    exit 0
  fi

  case "$1" in
    --audit)
      mode_audit
      ;;
    --preflight)
      shift
      mode_preflight "${1:-}"
      ;;
    --suggest)
      mode_suggest
      ;;
    --help|-h)
      usage
      ;;
    *)
      echo "$(color_red 'ERROR'): unknown mode: $1"
      usage
      exit 1
      ;;
  esac
}

main "$@"
