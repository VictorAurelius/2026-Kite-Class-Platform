#!/usr/bin/env bash
#
# self-test-reset.sh — Reset DB state + Redis cache giữa các lần dev walk-through
# (Wave 87 dev self-test enablement, Bucket B).
#
# Hành động:
#   1. TRUNCATE 4 bảng dev-state:
#        - beta_requests
#        - admin_audit_logs
#        - tenant_invitations
#        - parent_invitations
#   2. Redis FLUSH các key:
#        - auth:*       (JWT blacklist, session cache)
#        - ratelimit:*  (rate-limiter counters)
#   3. Re-invoke `scripts/dev/seed-personas.sh` (Bucket A — script này gọi sau khi
#      truncate; nếu Bucket A chưa merge, script in cảnh báo + tiếp tục).
#
# Use case: dev abort mid-walkthrough → DB pollute → cần clean state để lặp lại
# (xem outside-in finding #3 "DB state pollution mid-walkthrough", Wave 87 §Q3).
#
# Sử dụng:
#   bash scripts/dev/self-test-reset.sh                # thực thi truncate + flush + seed
#   bash scripts/dev/self-test-reset.sh --dry-run      # in plan, KHÔNG modify DB/Redis
#   bash scripts/dev/self-test-reset.sh --help         # hiển thị help
#
# Tham chiếu: documents/03-planning/waves/wave-2026-05-17-87-dev-self-test-enablement.md §3 Bucket B
# Rule: .claude/rules/agent-aws-access.md (DB write = Tier 3 → cần explicit confirm,
#       nhưng đây là DEV environment local, không phải production)

set -euo pipefail

# ----- Color codes -----
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

# ----- Config -----
DRY_RUN=0
PG_CONTAINER="${PG_CONTAINER:-kite-postgres}"
REDIS_CONTAINER="${REDIS_CONTAINER:-kite-redis}"
PG_USER="${PG_USER:-kitehub}"
PG_DB="${PG_DB:-kitehub}"

TABLES_TO_TRUNCATE=(
  "beta_requests"
  "admin_audit_logs"
  "tenant_invitations"
  "parent_invitations"
)

REDIS_KEY_PATTERNS=(
  "auth:*"
  "ratelimit:*"
)

# ----- Helpers -----
usage() {
  cat <<EOF
${BOLD}self-test-reset.sh${RESET} — Reset DB + Redis cho dev self-test walk-through.

${BOLD}Sử dụng:${RESET}
  bash scripts/dev/self-test-reset.sh [options]

${BOLD}Options:${RESET}
  --dry-run     In plan (sẽ TRUNCATE bảng nào, FLUSH pattern nào) — KHÔNG modify
  -h, --help    Hiển thị help này

${BOLD}Bảng sẽ TRUNCATE:${RESET}
$(printf "  - %s\n" "${TABLES_TO_TRUNCATE[@]}")

${BOLD}Redis key patterns sẽ DEL:${RESET}
$(printf "  - %s\n" "${REDIS_KEY_PATTERNS[@]}")

${BOLD}Environment overrides:${RESET}
  PG_CONTAINER     postgres container name (default: kite-postgres)
  REDIS_CONTAINER  redis container name (default: kite-redis)
  PG_USER          DB user (default: kitehub)
  PG_DB            DB name (default: kitehub)

${BOLD}WARNING:${RESET} CHỈ chạy trên DEV environment local. KHÔNG chạy trên staging/production.
EOF
}

log_info() { printf "${BLUE}ℹ %s${RESET}\n" "$1"; }
log_ok() { printf "${GREEN}✓ %s${RESET}\n" "$1"; }
log_warn() { printf "${YELLOW}⚠ %s${RESET}\n" "$1"; }
log_err() { printf "${RED}✗ %s${RESET}\n" "$1"; }

# ----- Parse args -----
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      log_err "Option không nhận: $1"
      usage
      exit 2
      ;;
  esac
done

# ----- Safety check: phải ở DEV environment -----
if [[ "${NODE_ENV:-development}" == "production" ]] || [[ "${SPRING_PROFILES_ACTIVE:-}" == "production" ]]; then
  log_err "REFUSE: NODE_ENV/SPRING_PROFILES_ACTIVE = production. Script này CHỈ cho DEV."
  exit 3
fi

printf "${BOLD}=== Wave 87 self-test reset ===${RESET}\n"
if [[ $DRY_RUN -eq 1 ]]; then
  printf "${YELLOW}[DRY-RUN MODE] — không modify DB/Redis${RESET}\n"
fi
echo ""

# ----- Verify containers running -----
if [[ $DRY_RUN -eq 0 ]]; then
  if ! docker ps --format '{{.Names}}' | grep -qx "$PG_CONTAINER"; then
    log_err "Container ${PG_CONTAINER} không running — chạy 'bash kitehub/scripts/up.sh' trước"
    exit 4
  fi
  if ! docker ps --format '{{.Names}}' | grep -qx "$REDIS_CONTAINER"; then
    log_err "Container ${REDIS_CONTAINER} không running"
    exit 4
  fi
fi

# ============================================================================
# Step 1: TRUNCATE tables
# ============================================================================
log_info "Step 1/3: TRUNCATE ${#TABLES_TO_TRUNCATE[@]} bảng dev-state"
TRUNCATE_SQL="TRUNCATE TABLE $(IFS=,; echo "${TABLES_TO_TRUNCATE[*]}") RESTART IDENTITY CASCADE;"

if [[ $DRY_RUN -eq 1 ]]; then
  printf "  ${YELLOW}[DRY-RUN] sẽ chạy:${RESET}\n"
  printf "    docker exec %s psql -U %s -d %s -c \"%s\"\n" "$PG_CONTAINER" "$PG_USER" "$PG_DB" "$TRUNCATE_SQL"
else
  if docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -c "$TRUNCATE_SQL" >/dev/null 2>&1; then
    log_ok "TRUNCATE thành công: ${TABLES_TO_TRUNCATE[*]}"
  else
    log_warn "TRUNCATE fail (có thể 1-2 bảng chưa tồn tại) — thử từng bảng"
    for tbl in "${TABLES_TO_TRUNCATE[@]}"; do
      if docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" \
           -c "TRUNCATE TABLE ${tbl} RESTART IDENTITY CASCADE;" >/dev/null 2>&1; then
        log_ok "  ${tbl} truncated"
      else
        log_warn "  ${tbl} — skip (table không tồn tại hoặc lỗi)"
      fi
    done
  fi
fi

# ============================================================================
# Step 2: Redis FLUSH key patterns
# ============================================================================
echo ""
log_info "Step 2/3: Redis DEL ${#REDIS_KEY_PATTERNS[@]} key patterns"

for pattern in "${REDIS_KEY_PATTERNS[@]}"; do
  if [[ $DRY_RUN -eq 1 ]]; then
    printf "  ${YELLOW}[DRY-RUN] sẽ chạy:${RESET}\n"
    printf "    docker exec %s redis-cli --scan --pattern '%s' | xargs -r redis-cli DEL\n" \
      "$REDIS_CONTAINER" "$pattern"
  else
    # SCAN + DEL pattern; --scan không block Redis như KEYS
    KEY_COUNT=$(docker exec "$REDIS_CONTAINER" sh -c \
      "redis-cli --scan --pattern '${pattern}' | head -10000 | xargs -r redis-cli DEL 2>/dev/null | wc -l" \
      2>/dev/null || echo "0")
    log_ok "Pattern '${pattern}' → DEL batches (${KEY_COUNT} response lines)"
  fi
done

# ============================================================================
# Step 3: Re-invoke seed-personas.sh (Bucket A)
# ============================================================================
echo ""
log_info "Step 3/3: Re-invoke scripts/dev/seed-personas.sh"

SEED_SCRIPT="scripts/dev/seed-personas.sh"
if [[ -x "$SEED_SCRIPT" ]]; then
  if [[ $DRY_RUN -eq 1 ]]; then
    printf "  ${YELLOW}[DRY-RUN] sẽ chạy:${RESET} bash %s\n" "$SEED_SCRIPT"
  else
    if bash "$SEED_SCRIPT"; then
      log_ok "Seed personas re-run thành công"
    else
      log_err "seed-personas.sh fail — xem output bên trên"
      exit 5
    fi
  fi
else
  log_warn "${SEED_SCRIPT} chưa tồn tại (Bucket A chưa merge). Reset DB/Redis OK, nhưng seed bỏ qua."
  log_warn "Sau khi Bucket A merge, chạy 'bash ${SEED_SCRIPT}' thủ công để re-seed."
fi

# ============================================================================
# Summary
# ============================================================================
echo ""
if [[ $DRY_RUN -eq 1 ]]; then
  printf "${YELLOW}${BOLD}[DRY-RUN] Plan hiển thị xong — không có thay đổi${RESET}\n"
else
  printf "${GREEN}${BOLD}✅ Reset hoàn tất — sẵn sàng walk-through round mới${RESET}\n"
fi
exit 0
