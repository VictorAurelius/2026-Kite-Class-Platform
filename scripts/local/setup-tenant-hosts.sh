#!/usr/bin/env bash
#
# setup-tenant-hosts.sh — map each active tenant subdomain to 127.0.0.1 in
# /etc/hosts so the local stack is reachable by REAL subdomain
# (http://<slug>.kiteclass.local:3000/), exactly like production
# (<slug>.kiteclass.com) — replacing the ?tenant=<slug> preview query param.
#
# Vì sao: FE middleware (kiteclass-frontend/src/middleware.ts) resolve tenant
# theo Host subdomain → cần hostname >=3 phần (parts[0]=slug). `<slug>.localhost`
# chỉ 2 phần nên KHÔNG nhận; `<slug>.kiteclass.local` (3 phần) hoạt động đúng.
#
# Usage:
#   bash scripts/local/setup-tenant-hosts.sh             # apply (sudo ghi /etc/hosts)
#   bash scripts/local/setup-tenant-hosts.sh --dry-run   # in block + URL, không ghi
#   bash scripts/local/setup-tenant-hosts.sh --print      # chỉ in URL (gồm nip.io fallback)
#   bash scripts/local/setup-tenant-hosts.sh --auto       # best-effort: ghi nếu sudo -n OK, else in URL (up.sh dùng)
#
# Env overrides:
#   LOCAL_TENANT_BASE   base domain (default: kiteclass.local)
#   KITE_FE_PORT        FE port      (default: 3000)
#   PG_CONTAINER        pg container (default: kite-postgres)
#   PG_DB / PG_USER     (default: kitehub / kitehub)
set -euo pipefail

BASE="${LOCAL_TENANT_BASE:-kiteclass.local}"
PORT="${KITE_FE_PORT:-3000}"
PG_CONTAINER="${PG_CONTAINER:-kite-postgres}"
PG_DB="${PG_DB:-kitehub}"
PG_USER="${PG_USER:-kitehub}"
HOSTS_FILE="/etc/hosts"
BEGIN="# >>> kite local tenant hosts (managed by scripts/local/setup-tenant-hosts.sh) >>>"
END="# <<< kite local tenant hosts <<<"

MODE="apply"
case "${1:-}" in
  --dry-run) MODE="dry-run" ;;
  --print)   MODE="print" ;;
  --auto)    MODE="auto" ;;
  "")        MODE="apply" ;;
  *) echo "Unknown arg: $1" >&2; exit 2 ;;
esac

# CI / non-local guard — job chỉ áp dụng cho dev env local.
if [ -n "${CI:-}" ]; then
  echo "[tenant-hosts] CI env detected — bỏ qua (chỉ chạy local)."; exit 0
fi

# Postgres phải chạy local mới query được subdomain.
if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$PG_CONTAINER"; then
  echo "[tenant-hosts] '$PG_CONTAINER' chưa chạy — bỏ qua (chạy ./scripts/up.sh trước)."; exit 0
fi

# Active tenants (ACTIVE/TRIAL, chưa xóa) — gateway chỉ resolve các status này.
mapfile -t SUBS < <(
  docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -tA -c \
    "SELECT subdomain FROM instances
       WHERE deleted=false AND status IN ('ACTIVE','TRIAL') AND subdomain IS NOT NULL
       ORDER BY subdomain;" 2>/dev/null | sed '/^$/d'
)

if [ "${#SUBS[@]}" -eq 0 ]; then
  echo "[tenant-hosts] Không có tenant ACTIVE/TRIAL nào trong DB — bỏ qua."; exit 0
fi

# Build managed block (one alias per line for readability).
build_block() {
  printf '%s\n' "$BEGIN"
  local s
  for s in "${SUBS[@]}"; do printf '127.0.0.1 %s.%s\n' "$s" "$BASE"; done
  printf '%s\n' "$END"
}

print_urls() {
  echo ""
  echo "Landing per-tenant (subdomain host — giống production):"
  local s
  for s in "${SUBS[@]}"; do
    printf '  • http://%s.%s:%s/\n' "$s" "$BASE" "$PORT"
  done
  echo ""
  echo "Fallback zero-config (nip.io — không cần sửa hosts, dùng từ browser Windows):"
  for s in "${SUBS[@]}"; do
    printf '  • http://%s.127.0.0.1.nip.io:%s/\n' "$s" "$PORT"
  done
}

apply_hosts() {
  local tmp; tmp="$(mktemp)"
  # Strip old managed block, then append fresh one.
  awk -v b="$BEGIN" -v e="$END" '
    $0==b {skip=1; next}
    skip && $0==e {skip=0; next}
    !skip {print}
  ' "$HOSTS_FILE" > "$tmp"
  build_block >> "$tmp"
  sudo cp "$HOSTS_FILE" "${HOSTS_FILE}.kite.bak" 2>/dev/null || true
  sudo cp "$tmp" "$HOSTS_FILE"
  rm -f "$tmp"
  echo "[tenant-hosts] ✅ Đã cập nhật $HOSTS_FILE (${#SUBS[@]} tenant). Backup: ${HOSTS_FILE}.kite.bak"
  # WSL: browser Windows cần Windows hosts riêng — in block để paste (không tự ghi, cần admin).
  if grep -qi microsoft /proc/version 2>/dev/null; then
    echo ""
    echo "[tenant-hosts] WSL2 phát hiện — nếu mở bằng browser Windows, thêm vào"
    printf '  %s (Notepad as Administrator):\n' 'C:\Windows\System32\drivers\etc\hosts'
    local s; for s in "${SUBS[@]}"; do printf '    127.0.0.1 %s.%s\n' "$s" "$BASE"; done
    echo "  (hoặc dùng URL nip.io bên dưới — không cần sửa hosts.)"
  fi
}

case "$MODE" in
  dry-run)
    echo "[tenant-hosts] DRY-RUN — block sẽ ghi vào $HOSTS_FILE:"; echo ""
    build_block; print_urls ;;
  print)
    print_urls ;;
  apply)
    apply_hosts; print_urls ;;
  auto)
    if sudo -n true 2>/dev/null; then
      apply_hosts
    else
      echo "[tenant-hosts] sudo cần mật khẩu — bỏ qua ghi /etc/hosts (non-fatal)."
      echo "[tenant-hosts] Để có host .$BASE: chạy 'sudo bash scripts/local/setup-tenant-hosts.sh'"
    fi
    print_urls ;;
esac
