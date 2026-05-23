#!/usr/bin/env bash
#
# seed-thesis-demo-tenants.sh — seed 2 demo tenants với separated data cho thesis multi-tenant demo
#
# Usage:
#   bash scripts/seed-thesis-demo-tenants.sh           # seed (default mode)
#   bash scripts/seed-thesis-demo-tenants.sh --dry-run # print SQL without executing
#   bash scripts/seed-thesis-demo-tenants.sh --cleanup # remove demo tenants
#   bash scripts/seed-thesis-demo-tenants.sh --help    # show this help
#
# What this seeds (idempotent — safe to re-run):
#
#   tenant_a (Sky Education)
#     instance_id: 11111111-1111-1111-1111-111111111111
#     name:        Trung tâm Anh ngữ Sky Education
#     owner email: hong.tran@sky-edu.demo
#     classes:     "Lớp Anh ngữ 5A1", "Lớp Anh ngữ 7B"
#     students:    Trần Thị Hồng, Nguyễn Văn An
#
#   tenant_b (Quang Minh)
#     instance_id: 22222222-2222-2222-2222-222222222222
#     name:        Trung tâm Toán Quang Minh
#     owner email: minh.le@quang-minh.demo
#     classes:     "Lớp Toán 9B", "Lớp Toán 10A"
#     students:    Phạm Thị Mai, Lê Văn Quang
#
# Connection (default — local Docker stack per kitehub/docker-compose.kitehub.yml):
#   container: kite-postgres
#   db:        kiteclass_shared  (KiteClass core tenant data)
#   user:      ${POSTGRES_USER:-kitehub}
#
# Override via env vars:
#   PG_CONTAINER  — default "kite-postgres"
#   PG_DB         — default "kiteclass_shared"
#   PG_USER       — default "$POSTGRES_USER" or "kitehub"
#   PG_HOST       — default "localhost"  (when using psql outside container)
#   PG_PORT       — default "5433"       (host port; container internal = 5432)
#   USE_DOCKER    — "true" (default) routes via `docker exec`; "false" requires PGPASSWORD env + psql installed
#
# Production AWS execution defer per GAP-652 acceptance criteria
#   ("scripts/seed-thesis-demo-tenants.sh") — runtime DB seed scheduled Wave thesis-2
#   post AWS account restore (GAP-612).
#
# Per .claude/rules/agent-aws-access.md §4.3 Tier 3 (mutation) — this script is
# LOCAL-DEV ONLY by default. Do NOT run against production RDS without explicit
# user authorization + pre-mutation state-check audit artifact per
# .claude/rules/pre-mutation-state-check.md §3.

set -euo pipefail

# ---------- defaults ----------
PG_CONTAINER="${PG_CONTAINER:-kite-postgres}"
PG_DB="${PG_DB:-kiteclass_shared}"
PG_USER="${PG_USER:-${POSTGRES_USER:-kitehub}}"
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5433}"
USE_DOCKER="${USE_DOCKER:-true}"

# Demo tenant UUIDs (fixed for idempotency — same UUID every run)
TENANT_A_ID="11111111-1111-1111-1111-111111111111"
TENANT_A_NAME="Trung tâm Anh ngữ Sky Education"
# shellcheck disable=SC2034  # SLUG kept for documentation + future FrontendInstance seed extension
TENANT_A_SLUG="sky-education"
TENANT_A_OWNER_EMAIL="hong.tran@sky-edu.demo"

TENANT_B_ID="22222222-2222-2222-2222-222222222222"
TENANT_B_NAME="Trung tâm Toán Quang Minh"
# shellcheck disable=SC2034  # SLUG kept for documentation + future FrontendInstance seed extension
TENANT_B_SLUG="quang-minh"
TENANT_B_OWNER_EMAIL="minh.le@quang-minh.demo"

# ---------- mode parsing ----------
MODE="seed"
case "${1:-}" in
    --dry-run)  MODE="dry-run" ;;
    --cleanup)  MODE="cleanup" ;;
    --help|-h)
        sed -n '2,40p' "$0" | sed 's/^# \{0,1\}//'
        exit 0
        ;;
    "") ;;
    *)
        echo "ERROR: unknown flag '$1' — use --help for usage" >&2
        exit 2
        ;;
esac

# ---------- helpers ----------
log() { printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"; }

run_sql() {
    local sql="$1"
    if [[ "$MODE" == "dry-run" ]]; then
        echo "----- DRY-RUN SQL -----"
        echo "$sql"
        echo "----- END SQL -----"
        return 0
    fi

    if [[ "$USE_DOCKER" == "true" ]]; then
        # Route via docker exec — assumes stack already up via kitehub/scripts/up.sh
        if ! docker ps --format '{{.Names}}' | grep -q "^${PG_CONTAINER}$"; then
            echo "ERROR: container '$PG_CONTAINER' not running" >&2
            echo "  → run: bash kitehub/scripts/up.sh --profile full" >&2
            exit 3
        fi
        docker exec -i "$PG_CONTAINER" psql -v ON_ERROR_STOP=1 \
            -U "$PG_USER" -d "$PG_DB" <<<"$sql"
    else
        # Direct psql via host port (5433); requires PGPASSWORD env
        if [[ -z "${PGPASSWORD:-}" ]]; then
            echo "ERROR: PGPASSWORD env required when USE_DOCKER=false" >&2
            exit 4
        fi
        psql -v ON_ERROR_STOP=1 \
            -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$PG_DB" <<<"$sql"
    fi
}

verify_seeded() {
    local tenant_id="$1"
    local expected_classes="$2"
    if [[ "$MODE" == "dry-run" ]]; then
        log "[dry-run] would verify tenant=$tenant_id has $expected_classes classes"
        return 0
    fi

    # Bypass RLS via superuser context (kitehub user owns tables — FORCE RLS applies even to owner)
    # Use SET row_security = off as break-glass per V58 migration comment
    local count
    count=$(docker exec -i "$PG_CONTAINER" psql -tA -U "$PG_USER" -d "$PG_DB" -c \
        "SET row_security = off; SELECT count(*) FROM classes WHERE instance_id = '$tenant_id'::uuid AND deleted = false;" \
        2>/dev/null | tr -d '[:space:]')

    if [[ "$count" == "$expected_classes" ]]; then
        log "✓ tenant $tenant_id has $count classes (expected $expected_classes)"
    else
        log "✗ tenant $tenant_id has $count classes (expected $expected_classes)"
        return 1
    fi
}

# ---------- mode: cleanup ----------
do_cleanup() {
    log "Cleanup mode — removing demo tenants $TENANT_A_ID + $TENANT_B_ID"

    local sql
    sql=$(cat <<EOF
-- Break-glass: disable RLS for cleanup (per V58 migration comment §22-23)
SET row_security = off;

BEGIN;

-- Delete in dependency order (FK references)
DELETE FROM enrollments WHERE instance_id IN ('${TENANT_A_ID}', '${TENANT_B_ID}');
DELETE FROM students    WHERE instance_id IN ('${TENANT_A_ID}', '${TENANT_B_ID}');
DELETE FROM classes     WHERE instance_id IN ('${TENANT_A_ID}', '${TENANT_B_ID}');
DELETE FROM courses     WHERE instance_id IN ('${TENANT_A_ID}', '${TENANT_B_ID}');
DELETE FROM teachers    WHERE instance_id IN ('${TENANT_A_ID}', '${TENANT_B_ID}');

COMMIT;

-- Confirm
SELECT 'tenant_a remaining classes' AS label, count(*) FROM classes WHERE instance_id = '${TENANT_A_ID}';
SELECT 'tenant_b remaining classes' AS label, count(*) FROM classes WHERE instance_id = '${TENANT_B_ID}';
EOF
)
    run_sql "$sql"
    log "Cleanup complete"
}

# ---------- mode: seed ----------
do_seed() {
    log "Seed mode — creating tenant_a ($TENANT_A_NAME) + tenant_b ($TENANT_B_NAME)"
    log "  USE_DOCKER=$USE_DOCKER  PG_CONTAINER=$PG_CONTAINER  PG_DB=$PG_DB  PG_USER=$PG_USER"

    local sql
    sql=$(cat <<EOF
-- Break-glass: disable RLS for seeding (per V58 migration comment §22-23)
-- Required because seed runs as table owner with FORCE ROW LEVEL SECURITY applied
SET row_security = off;

BEGIN;

-- ============================================================================
-- TENANT A: Sky Education
-- ============================================================================

-- Teacher (referenced by classes)
INSERT INTO teachers (instance_id, name, email, phone, department, specialization, status, created_at, updated_at)
VALUES (
    '${TENANT_A_ID}'::uuid,
    'Trần Thị Hồng',
    '${TENANT_A_OWNER_EMAIL}',
    '0901 234 567',
    'Anh ngữ',
    'IELTS, TOEFL',
    'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

-- Course (referenced by classes via FK)
INSERT INTO courses (instance_id, code, name, description, status, created_at, updated_at)
VALUES (
    '${TENANT_A_ID}'::uuid,
    'ENG-5A',
    'Anh ngữ Cấp 5',
    'Khóa Anh ngữ cấp 5 — Sky Education',
    'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    '${TENANT_A_ID}'::uuid,
    'ENG-7B',
    'Anh ngữ Cấp 7',
    'Khóa Anh ngữ cấp 7 — Sky Education',
    'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

-- Classes
INSERT INTO classes (instance_id, course_id, code, name, teacher_id,
                     start_date, end_date, max_students, tuition_amount, tuition_type, status,
                     created_at, updated_at)
SELECT
    '${TENANT_A_ID}'::uuid,
    c.id,
    'CLS-' || c.code,
    CASE c.code
        WHEN 'ENG-5A' THEN 'Lớp Anh ngữ 5A1'
        WHEN 'ENG-7B' THEN 'Lớp Anh ngữ 7B'
    END,
    (SELECT id FROM teachers WHERE instance_id = '${TENANT_A_ID}'::uuid LIMIT 1),
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '90 days',
    30,
    1500000.00,
    'fixed',
    'ongoing',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM courses c
WHERE c.instance_id = '${TENANT_A_ID}'::uuid
  AND NOT EXISTS (
      SELECT 1 FROM classes cls
      WHERE cls.instance_id = c.instance_id AND cls.code = 'CLS-' || c.code
  );

-- Students
INSERT INTO students (instance_id, name, email, phone, status, created_at, updated_at)
VALUES (
    '${TENANT_A_ID}'::uuid,
    'Trần Thị Hồng',
    'student.hong.a@sky-edu.demo',
    '0903 111 222',
    'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    '${TENANT_A_ID}'::uuid,
    'Nguyễn Văn An',
    'student.an.a@sky-edu.demo',
    '0903 333 444',
    'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- TENANT B: Quang Minh
-- ============================================================================

-- Teacher
INSERT INTO teachers (instance_id, name, email, phone, department, specialization, status, created_at, updated_at)
VALUES (
    '${TENANT_B_ID}'::uuid,
    'Lê Quang Minh',
    '${TENANT_B_OWNER_EMAIL}',
    '0902 555 666',
    'Toán học',
    'Toán nâng cao THCS-THPT',
    'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

-- Courses
INSERT INTO courses (instance_id, code, name, description, status, created_at, updated_at)
VALUES (
    '${TENANT_B_ID}'::uuid,
    'MATH-9',
    'Toán Cấp 9',
    'Khóa Toán cấp 9 — Quang Minh',
    'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    '${TENANT_B_ID}'::uuid,
    'MATH-10',
    'Toán Cấp 10',
    'Khóa Toán cấp 10 — Quang Minh',
    'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

-- Classes
INSERT INTO classes (instance_id, course_id, code, name, teacher_id,
                     start_date, end_date, max_students, tuition_amount, tuition_type, status,
                     created_at, updated_at)
SELECT
    '${TENANT_B_ID}'::uuid,
    c.id,
    'CLS-' || c.code,
    CASE c.code
        WHEN 'MATH-9'  THEN 'Lớp Toán 9B'
        WHEN 'MATH-10' THEN 'Lớp Toán 10A'
    END,
    (SELECT id FROM teachers WHERE instance_id = '${TENANT_B_ID}'::uuid LIMIT 1),
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '90 days',
    25,
    1800000.00,
    'fixed',
    'ongoing',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM courses c
WHERE c.instance_id = '${TENANT_B_ID}'::uuid
  AND NOT EXISTS (
      SELECT 1 FROM classes cls
      WHERE cls.instance_id = c.instance_id AND cls.code = 'CLS-' || c.code
  );

-- Students
INSERT INTO students (instance_id, name, email, phone, status, created_at, updated_at)
VALUES (
    '${TENANT_B_ID}'::uuid,
    'Phạm Thị Mai',
    'student.mai.b@quang-minh.demo',
    '0904 777 888',
    'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    '${TENANT_B_ID}'::uuid,
    'Lê Văn Quang',
    'student.quang.b@quang-minh.demo',
    '0904 999 000',
    'ACTIVE',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

COMMIT;

-- ============================================================================
-- VERIFICATION (post-seed counts)
-- ============================================================================
SELECT 'tenant_a classes' AS label, count(*) FROM classes  WHERE instance_id = '${TENANT_A_ID}';
SELECT 'tenant_a students' AS label, count(*) FROM students WHERE instance_id = '${TENANT_A_ID}';
SELECT 'tenant_b classes' AS label, count(*) FROM classes  WHERE instance_id = '${TENANT_B_ID}';
SELECT 'tenant_b students' AS label, count(*) FROM students WHERE instance_id = '${TENANT_B_ID}';
EOF
)
    run_sql "$sql"

    if [[ "$MODE" != "dry-run" ]]; then
        log "Verifying seed..."
        verify_seeded "$TENANT_A_ID" "2"
        verify_seeded "$TENANT_B_ID" "2"
    fi

    log "Seed complete"
    log ""
    log "Demo credentials for defense walkthrough:"
    log "  Sky Education  — instance_id=$TENANT_A_ID  owner=$TENANT_A_OWNER_EMAIL"
    log "  Quang Minh     — instance_id=$TENANT_B_ID  owner=$TENANT_B_OWNER_EMAIL"
    log ""
    log "Next: per documents/08-thesis/defense/multi-tenant-demo-script.md"
}

# ---------- main ----------
log "Mode: $MODE"

case "$MODE" in
    dry-run|seed) do_seed ;;
    cleanup)      do_cleanup ;;
    *)
        echo "ERROR: invalid mode '$MODE'" >&2
        exit 2
        ;;
esac
