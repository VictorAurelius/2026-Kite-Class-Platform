#!/usr/bin/env bash
#
# self-test-preflight.sh — Kiểm tra 6 cổng (gate) bắt buộc trước khi dev
# thực hiện walk-through 94 USER-VERIFY + 27 INSUFFICIENT_SPEC rows trong
# `phase-1-beta-acceptance-self-test.csv` (Wave 87 dev self-test enablement).
#
# 6 cổng kiểm tra:
#   Gate 1 — Docker stack: 6 container thiết yếu đang chạy (healthy)
#   Gate 2 — Flyway: schema version mới nhất đã apply trên kite-postgres
#   Gate 3 — Admin role canonical: BE seed `PLATFORM_ADMIN` ↔ FE role-guard khớp
#   Gate 4 — ALB HTTPS:443: AWS Load Balancer phản hồi (chấp nhận self-signed)
#   Gate 5 — Cloudflare DNS: `api.kitehub.me` resolve về IP/CNAME
#   Gate 6 — Resend API key valid + verified recipients list non-empty
#
# Exit code:
#   0  → cả 6 cổng pass ("✅ All gates green")
#   1+ → cổng đầu tiên fail (in tên cổng + chi tiết lỗi)
#
# Sử dụng:
#   bash scripts/dev/self-test-preflight.sh
#   bash scripts/dev/self-test-preflight.sh --help
#
# Tham chiếu: documents/03-planning/waves/wave-2026-05-17-87-dev-self-test-enablement.md §3 Bucket B
# Rule: .claude/rules/pre-handoff-self-test-completeness.md §2.4

set -uo pipefail

# ----- Color codes (POSIX-safe; degrade gracefully nếu terminal không support) -----
if [[ -t 1 ]]; then
  RED=$'\033[0;31m'
  GREEN=$'\033[0;32m'
  YELLOW=$'\033[0;33m'
  BLUE=$'\033[0;34m'
  BOLD=$'\033[1m'
  RESET=$'\033[0m'
else
  RED=""; GREEN=""; YELLOW=""; BLUE=""; BOLD=""; RESET=""
fi

# ----- Config (overridable via env) -----
ALB_URL="${ALB_URL:-https://kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com/}"
API_DOMAIN="${API_DOMAIN:-api.kitehub.me}"
ENV_TEST_FILE="${ENV_TEST_FILE:-.env.test}"

REQUIRED_CONTAINERS=(
  "kite-postgres"
  "kite-redis"
  "kite-rabbitmq"
  "kite-minio"
  "kite-gateway"
  "kitehub-admin"
)

# ----- Helpers -----
usage() {
  cat <<EOF
${BOLD}self-test-preflight.sh${RESET} — Kiểm tra 6 cổng bắt buộc trước khi dev self-test.

${BOLD}Sử dụng:${RESET}
  bash scripts/dev/self-test-preflight.sh [options]

${BOLD}Options:${RESET}
  -h, --help    Hiển thị help này.

${BOLD}Environment overrides:${RESET}
  ALB_URL          URL AWS ALB (default: kitehub-alb-... .ap-southeast-1)
  API_DOMAIN       Domain API để check DNS (default: api.kitehub.me)
  ENV_TEST_FILE    Đường dẫn .env.test (default: .env.test)
  RESEND_API_KEY   Resend API key (đọc từ env hoặc .env.test)

${BOLD}Exit codes:${RESET}
  0   Tất cả 6 cổng pass
  1+  Có cổng fail (xem tên cổng + chi tiết)
EOF
}

gate_pass() {
  printf "${GREEN}✅ Gate %s — %s${RESET}\n" "$1" "$2"
}

gate_fail() {
  printf "${RED}❌ Gate %s — %s${RESET}\n" "$1" "$2"
  printf "${RED}   Lỗi: %s${RESET}\n" "$3"
}

gate_header() {
  printf "${BLUE}▶ Gate %s — %s...${RESET}\n" "$1" "$2"
}

# Load .env.test nếu có (cho RESEND_API_KEY, etc.)
if [[ -f "$ENV_TEST_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a; source "$ENV_TEST_FILE"; set +a
fi

# ----- Parse args -----
case "${1:-}" in
  -h|--help)
    usage
    exit 0
    ;;
esac

printf "${BOLD}=== Wave 87 self-test preflight ===${RESET}\n"
printf "Kiểm tra 6 cổng trước khi dev thực hiện walk-through.\n\n"

FAIL_COUNT=0

# ============================================================================
# Gate 1 — Docker stack health
# ============================================================================
gate_header 1 "Docker stack — 6 container thiết yếu running"
if ! command -v docker >/dev/null 2>&1; then
  gate_fail 1 "Docker stack" "Lệnh 'docker' không tồn tại trong PATH"
  FAIL_COUNT=$((FAIL_COUNT + 1))
else
  RUNNING_CONTAINERS=$(docker ps --format '{{.Names}}' 2>/dev/null || echo "")
  MISSING=()
  for c in "${REQUIRED_CONTAINERS[@]}"; do
    if ! grep -qx "$c" <<<"$RUNNING_CONTAINERS"; then
      MISSING+=("$c")
    fi
  done

  if [[ ${#MISSING[@]} -eq 0 ]]; then
    gate_pass 1 "Docker stack OK (${#REQUIRED_CONTAINERS[@]}/6 container running)"
  else
    gate_fail 1 "Docker stack" "Thiếu container: ${MISSING[*]} — chạy 'bash kitehub/scripts/up.sh' để start stack"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
fi

# ============================================================================
# Gate 2 — Flyway latest version
# ============================================================================
gate_header 2 "Flyway schema version — latest đã apply"
if [[ $FAIL_COUNT -gt 0 ]] && ! docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "kite-postgres"; then
  gate_fail 2 "Flyway version" "kite-postgres không running — skip query"
  FAIL_COUNT=$((FAIL_COUNT + 1))
else
  FLYWAY_VERSION=$(docker exec kite-postgres psql -U kitehub -d kitehub -tAc \
    "SELECT version FROM flyway_schema_history WHERE success=true ORDER BY installed_rank DESC LIMIT 1;" \
    2>/dev/null | tr -d '[:space:]' || echo "")

  if [[ -n "$FLYWAY_VERSION" ]]; then
    gate_pass 2 "Flyway version = V${FLYWAY_VERSION}"
  else
    gate_fail 2 "Flyway version" "Không query được flyway_schema_history (DB chưa migrate hoặc credential sai)"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
fi

# ============================================================================
# Gate 3 — Admin role canonical match (BE seed ↔ FE role-guard)
# ============================================================================
gate_header 3 "Admin role canonical — BE 'PLATFORM_ADMIN' ↔ FE role-guard"
BE_HITS=$(grep -rE "\"PLATFORM_ADMIN\"|'PLATFORM_ADMIN'|PLATFORM_ADMIN" \
  kitehub/kitehub-admin/src/main/java/ \
  kitehub/kitehub-subscription/src/main/java/ \
  2>/dev/null | wc -l)

FE_PLATFORM_ADMIN=$(grep -rE "PLATFORM_ADMIN" \
  kitehub/kitehub-frontend/src/ 2>/dev/null | wc -l)

FE_ADMIN_LITERAL=$(grep -rE "['\"](ADMIN)['\"]" \
  kitehub/kitehub-frontend/src/lib/auth-helpers.ts \
  kitehub/kitehub-frontend/src/components/RoleGuard.tsx \
  kitehub/kitehub-frontend/src/hooks/use-role.ts \
  2>/dev/null | grep -vE "PLATFORM_ADMIN" | wc -l)

if [[ $BE_HITS -gt 0 ]] && [[ $FE_PLATFORM_ADMIN -gt 0 ]] && [[ $FE_ADMIN_LITERAL -eq 0 ]]; then
  gate_pass 3 "Admin role canonical match (BE=${BE_HITS} hits, FE=${FE_PLATFORM_ADMIN} hits)"
elif [[ $FE_ADMIN_LITERAL -gt 0 ]]; then
  gate_fail 3 "Admin role mismatch" "FE còn ${FE_ADMIN_LITERAL} chỗ dùng literal 'ADMIN' (cần 'PLATFORM_ADMIN') — xem GAP-518"
  FAIL_COUNT=$((FAIL_COUNT + 1))
elif [[ $BE_HITS -eq 0 ]]; then
  gate_fail 3 "Admin role mismatch" "Không tìm thấy 'PLATFORM_ADMIN' trong BE seed Java code"
  FAIL_COUNT=$((FAIL_COUNT + 1))
else
  gate_fail 3 "Admin role mismatch" "Không tìm thấy 'PLATFORM_ADMIN' trong FE code (role-guard hoặc auth-helpers)"
  FAIL_COUNT=$((FAIL_COUNT + 1))
fi

# ============================================================================
# Gate 4 — ALB HTTPS:443 reachable
# ============================================================================
gate_header 4 "ALB HTTPS:443 — production load balancer reachable"
if ! command -v curl >/dev/null 2>&1; then
  gate_fail 4 "ALB HTTPS:443" "curl không tồn tại trong PATH"
  FAIL_COUNT=$((FAIL_COUNT + 1))
else
  ALB_HTTP_CODE=$(curl -sI -k --max-time 10 -o /dev/null -w "%{http_code}" "$ALB_URL" 2>/dev/null || echo "000")
  if [[ "$ALB_HTTP_CODE" =~ ^[2-5][0-9][0-9]$ ]]; then
    gate_pass 4 "ALB HTTPS:443 reachable (HTTP ${ALB_HTTP_CODE})"
  else
    gate_fail 4 "ALB HTTPS:443" "ALB không phản hồi (got: ${ALB_HTTP_CODE}; url: ${ALB_URL}) — check AWS console + security group"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
fi

# ============================================================================
# Gate 5 — Cloudflare DNS resolve api.kitehub.me
# ============================================================================
gate_header 5 "Cloudflare DNS — ${API_DOMAIN} resolve"
if ! command -v dig >/dev/null 2>&1; then
  gate_fail 5 "DNS resolve" "lệnh 'dig' không tồn tại — cài 'dnsutils' (apt) hoặc 'bind' (brew)"
  FAIL_COUNT=$((FAIL_COUNT + 1))
else
  DNS_RESULT=$(dig +short +time=5 +tries=2 "$API_DOMAIN" 2>/dev/null | head -5 | tr '\n' ' ')
  if [[ -n "${DNS_RESULT// }" ]]; then
    gate_pass 5 "DNS ${API_DOMAIN} → ${DNS_RESULT}"
  else
    gate_fail 5 "DNS resolve" "${API_DOMAIN} không resolve (NXDOMAIN hoặc timeout) — check Cloudflare DNS record"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
fi

# ============================================================================
# Gate 6 — Resend API key valid + verified recipients non-empty
# ============================================================================
gate_header 6 "Resend API key valid + verified recipients non-empty"
if [[ -z "${RESEND_API_KEY:-}" ]]; then
  gate_fail 6 "Resend API key" "RESEND_API_KEY không set (env hoặc ${ENV_TEST_FILE})"
  FAIL_COUNT=$((FAIL_COUNT + 1))
else
  RESEND_HTTP=$(curl -sI -H "Authorization: Bearer ${RESEND_API_KEY}" \
    --max-time 10 -o /dev/null -w "%{http_code}" \
    "https://api.resend.com/api-keys" 2>/dev/null || echo "000")

  RESEND_RECIPIENTS="${RESEND_VERIFIED_RECIPIENTS:-}"

  if [[ "$RESEND_HTTP" == "200" ]] && [[ -n "$RESEND_RECIPIENTS" ]]; then
    RECIPIENT_COUNT=$(echo "$RESEND_RECIPIENTS" | tr ',' '\n' | grep -c "@" || echo "0")
    gate_pass 6 "Resend API key valid + ${RECIPIENT_COUNT} verified recipients"
  elif [[ "$RESEND_HTTP" != "200" ]]; then
    gate_fail 6 "Resend API key" "API key không valid (HTTP ${RESEND_HTTP}) — rotate key hoặc check Resend dashboard"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  else
    gate_fail 6 "Resend recipients" "RESEND_VERIFIED_RECIPIENTS empty trong ${ENV_TEST_FILE} — cần list email aliases (vd: admin@kitehub.me,owner@sky-education.test)"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
fi

# ============================================================================
# Summary
# ============================================================================
echo ""
if [[ $FAIL_COUNT -eq 0 ]]; then
  printf "${GREEN}${BOLD}✅ All gates green — sẵn sàng walk-through self-test${RESET}\n"
  exit 0
else
  printf "${RED}${BOLD}❌ %d cổng fail — fix trước khi walk-through${RESET}\n" "$FAIL_COUNT"
  printf "${YELLOW}Tham khảo: scripts/dev/README.md §Troubleshooting${RESET}\n"
  exit 1
fi
