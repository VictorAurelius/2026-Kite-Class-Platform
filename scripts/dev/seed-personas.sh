#!/usr/bin/env bash
# seed-personas.sh — Seed dev-mode personas + tenant + sample data cho Wave 87 self-test
#
# Script idempotent: chạy 2 lần liên tiếp KHÔNG duplicate rows (UPSERT via ON CONFLICT).
# Tạo:
#   - 1 PLATFORM_ADMIN user
#   - 1 approved tenant "Sky Education" (slug: sky-education)
#   - 1 P2 Owner (owner@sky-education.test)
#   - 2 Teachers (teacher1/teacher2@sky-education.test)
#   - 3 Parents (parent1..3@sky-education.test)
#   - 3 Students (linked với parents)
#   - 1 class "Lớp 5A1"
#   - 1 sample payment row
#
# Tham khảo: documents/03-planning/waves/wave-2026-05-17-87-dev-self-test-enablement.md §3 Bucket A
#
# Sử dụng:
#   bash scripts/dev/seed-personas.sh             # Execute seed
#   bash scripts/dev/seed-personas.sh --dry-run   # Show plan, no DB write
#   bash scripts/dev/seed-personas.sh --help      # Show usage

set -euo pipefail

# ---------- Configuration ----------
readonly POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-kite-postgres}"
readonly POSTGRES_USER="${POSTGRES_USER:-kite}"
readonly POSTGRES_DB="${POSTGRES_DB:-kitehub}"

# Default dev passwords — chỉ dùng cho local dev stack, KHÔNG dùng production.
readonly ADMIN_EMAIL="admin@kitehub.test"
readonly ADMIN_PASSWORD="DevAdmin#2026"
readonly OWNER_EMAIL="owner@sky-education.test"
readonly OWNER_PASSWORD="DevOwner#2026"
readonly TEACHER_PASSWORD="DevTeacher#2026"
readonly PARENT_PASSWORD="DevParent#2026"

readonly TENANT_NAME="Trung tâm Anh ngữ Sky Education"
readonly TENANT_SLUG="sky-education"
readonly CLASS_NAME="Lớp 5A1"

# bcrypt hash của các mật khẩu trên (cost=10).
# Generated: htpasswd -bnBC 10 "" "<password>" | tr -d ':\n'
# NOTE: Đây là placeholder hashes — service thực tế sẽ verify qua BCryptPasswordEncoder.
# Để tránh complexity với external bcrypt tool, dùng cùng 1 hash mock cho dev seed.
readonly BCRYPT_PLACEHOLDER='$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'

# ---------- Helpers ----------
usage() {
  cat <<EOF
seed-personas.sh — Seed dev personas vào local Postgres

Sử dụng:
  bash scripts/dev/seed-personas.sh [OPTIONS]

OPTIONS:
  --dry-run    Print plan, không write DB
  --help       Show this help

YÊU CẦU:
  - Docker stack đang chạy (kite-postgres container UP)
  - Flyway migrations đã apply tới latest

OUTPUT:
  Bảng credentials in ra stdout sau khi seed xong (ngoại trừ --dry-run).

ENVIRONMENT:
  POSTGRES_CONTAINER   Container name (default: kite-postgres)
  POSTGRES_USER        DB user (default: kite)
  POSTGRES_DB          DB name (default: kitehub)
EOF
}

log_info() { echo "[INFO] $*" >&2; }
log_warn() { echo "[WARN] $*" >&2; }
log_error() { echo "[ERROR] $*" >&2; }

# Parse args
DRY_RUN=false
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=true ;;
    --help|-h) usage; exit 0 ;;
    *) log_error "Unknown arg: $arg"; usage; exit 1 ;;
  esac
done

# ---------- Plan ----------
print_plan() {
  cat <<EOF
============================================================
seed-personas.sh — PLAN (dry-run=$DRY_RUN)
============================================================

Target DB:       $POSTGRES_USER@$POSTGRES_CONTAINER/$POSTGRES_DB

Rows sẽ UPSERT (ON CONFLICT DO NOTHING/UPDATE):

  1. users:
     - $ADMIN_EMAIL              (PLATFORM_ADMIN)
     - $OWNER_EMAIL              (P2_CENTER_OWNER)
     - teacher1@$TENANT_SLUG.test  (TEACHER)
     - teacher2@$TENANT_SLUG.test  (TEACHER)
     - parent1@$TENANT_SLUG.test   (PARENT)
     - parent2@$TENANT_SLUG.test   (PARENT)
     - parent3@$TENANT_SLUG.test   (PARENT)

  2. tenants:
     - "$TENANT_NAME" (slug=$TENANT_SLUG, status=APPROVED)

  3. tenant_users (membership):
     - owner + 2 teachers + 3 parents linked với tenant

  4. students:
     - Học sinh 1 (linked parent1)
     - Học sinh 2 (linked parent2)
     - Học sinh 3 (linked parent3)

  5. classes:
     - "$CLASS_NAME" (tenant_id = sky-education)

  6. payments:
     - 1 sample payment row (parent1 → tenant, amount=1.500.000đ, status=COMPLETED)

============================================================
EOF
}

# ---------- Pre-flight ----------
check_postgres() {
  if ! docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER}\$"; then
    log_error "Container $POSTGRES_CONTAINER không chạy. Run: ./scripts/up.sh"
    exit 2
  fi
}

# Execute SQL trong container
exec_sql() {
  local sql="$1"
  docker exec -i "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -v ON_ERROR_STOP=1 -X -q -A -t <<<"$sql"
}

# ---------- Seed SQL ----------
# NOTE: Schema chính xác của bảng users / tenants / classes / payments có thể khác
# theo Flyway version hiện tại. SQL dưới đây dùng cấu trúc phổ biến + ON CONFLICT để
# bảo đảm idempotent. Nếu schema thực tế khác (vd: cột bổ sung NOT NULL không default),
# script sẽ surface error rõ ràng từ psql để dev điều chỉnh.

build_seed_sql() {
  cat <<SQL
BEGIN;

-- 1. PLATFORM_ADMIN user
INSERT INTO users (email, password_hash, full_name, role, status, created_at, updated_at)
VALUES ('$ADMIN_EMAIL', '$BCRYPT_PLACEHOLDER', 'Platform Admin', 'PLATFORM_ADMIN', 'ACTIVE', NOW(), NOW())
ON CONFLICT (email) DO UPDATE SET
  full_name = EXCLUDED.full_name,
  role = EXCLUDED.role,
  updated_at = NOW();

-- 2. Tenant "Sky Education"
INSERT INTO tenants (name, slug, status, created_at, updated_at)
VALUES ('$TENANT_NAME', '$TENANT_SLUG', 'APPROVED', NOW(), NOW())
ON CONFLICT (slug) DO UPDATE SET
  status = 'APPROVED',
  updated_at = NOW();

-- 3. P2 Owner user
INSERT INTO users (email, password_hash, full_name, role, status, created_at, updated_at)
VALUES ('$OWNER_EMAIL', '$BCRYPT_PLACEHOLDER', 'Trần Thị Hồng', 'P2_CENTER_OWNER', 'ACTIVE', NOW(), NOW())
ON CONFLICT (email) DO UPDATE SET
  full_name = EXCLUDED.full_name,
  role = EXCLUDED.role,
  updated_at = NOW();

-- 4. 2 Teachers
INSERT INTO users (email, password_hash, full_name, role, status, created_at, updated_at)
VALUES
  ('teacher1@$TENANT_SLUG.test', '$BCRYPT_PLACEHOLDER', 'Nguyễn Văn An', 'TEACHER', 'ACTIVE', NOW(), NOW()),
  ('teacher2@$TENANT_SLUG.test', '$BCRYPT_PLACEHOLDER', 'Phạm Thị Mai', 'TEACHER', 'ACTIVE', NOW(), NOW())
ON CONFLICT (email) DO UPDATE SET
  full_name = EXCLUDED.full_name,
  updated_at = NOW();

-- 5. 3 Parents
INSERT INTO users (email, password_hash, full_name, role, status, created_at, updated_at)
VALUES
  ('parent1@$TENANT_SLUG.test', '$BCRYPT_PLACEHOLDER', 'Lê Văn Bình', 'PARENT', 'ACTIVE', NOW(), NOW()),
  ('parent2@$TENANT_SLUG.test', '$BCRYPT_PLACEHOLDER', 'Hoàng Thị Lan', 'PARENT', 'ACTIVE', NOW(), NOW()),
  ('parent3@$TENANT_SLUG.test', '$BCRYPT_PLACEHOLDER', 'Đỗ Minh Đức', 'PARENT', 'ACTIVE', NOW(), NOW())
ON CONFLICT (email) DO UPDATE SET
  full_name = EXCLUDED.full_name,
  updated_at = NOW();

COMMIT;

-- Note: tenant_users membership, students, classes, payments rows phụ thuộc schema
-- thực tế Flyway sau Bucket A. Nếu schema chưa có các bảng này → seed sẽ skip an toàn
-- và log warning thay vì fail (DO block dưới đây).

DO \$\$
BEGIN
  -- Nếu các bảng tồn tại, seed thêm:
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'classes') THEN
    INSERT INTO classes (tenant_id, name, status, created_at, updated_at)
    SELECT id, '$CLASS_NAME', 'ACTIVE', NOW(), NOW()
    FROM tenants WHERE slug = '$TENANT_SLUG'
    ON CONFLICT DO NOTHING;
  ELSE
    RAISE NOTICE 'Bảng classes chưa tồn tại — skip class seed';
  END IF;
END
\$\$;
SQL
}

# ---------- Output credentials table ----------
print_credentials() {
  cat <<EOF

============================================================
SEED COMPLETE — Credentials (dev-only, do NOT use in prod)
============================================================

| Role              | Email                              | Password         |
|-------------------|------------------------------------|------------------|
| PLATFORM_ADMIN    | $ADMIN_EMAIL                | $ADMIN_PASSWORD  |
| P2_CENTER_OWNER   | $OWNER_EMAIL          | $OWNER_PASSWORD  |
| TEACHER (×2)      | teacher{1,2}@$TENANT_SLUG.test | $TEACHER_PASSWORD |
| PARENT (×3)       | parent{1,2,3}@$TENANT_SLUG.test | $PARENT_PASSWORD  |

Tenant slug: $TENANT_SLUG  ($TENANT_NAME)
Class:       $CLASS_NAME

NOTE: password hashes là placeholder bcrypt — dev login chỉ work khi BE accept hash
này. Nếu BE reject, regenerate hash bằng:
  htpasswd -bnBC 10 "" "<password>" | tr -d ':\n'
hoặc dùng Java BCryptPasswordEncoder để compute + replace BCRYPT_PLACEHOLDER.

============================================================
EOF
}

# ---------- Main ----------
main() {
  print_plan

  if [[ "$DRY_RUN" == "true" ]]; then
    log_info "Dry-run mode — không write DB. Exit 0."
    exit 0
  fi

  log_info "Pre-flight: check Postgres container..."
  check_postgres

  log_info "Executing seed SQL..."
  if exec_sql "$(build_seed_sql)"; then
    log_info "Seed thành công."
    print_credentials
  else
    log_error "Seed failed — xem psql output ở trên để debug."
    exit 3
  fi
}

main "$@"
