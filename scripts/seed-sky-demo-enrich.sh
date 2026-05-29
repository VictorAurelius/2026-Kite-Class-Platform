#!/usr/bin/env bash
#
# seed-sky-demo-enrich.sh — enrich the "Sky Education" demo tenant (KiteClass) so the
# dashboard looks ALIVE, not like a mockup (GAP-805 Bucket B).
#
# Why a SEPARATE script (not editing seed-thesis-demo-tenants.sh)?
#   - Avoids merge conflicts on the canonical thesis-demo seed.
#   - This script DEPENDS ON tenant_a (Sky Education) existing. Run order:
#         bash scripts/seed-thesis-demo-tenants.sh   # creates Sky tenant base (thin)
#         bash scripts/seed-sky-demo-enrich.sh        # adds depth + attendance/grade/payment
#
# What this seeds for Sky Education (idempotent — safe to re-run):
#   - 3 enriched classes (Anh ngữ 5A1 enriched, IELTS 6.5, Giao tiếp Tối)
#     using courses CLS-ENG-5A (reused) + 2 new courses (ENG-IELTS65, ENG-COMM-EVE).
#   - ~25 Vietnamese-named students per enriched class (diverse VN names).
#   - Enrollments linking each student to its class.
#   - class_sessions: several sessions per class.
#   - attendance: realistic mix present/absent/late/excused per session.
#   - grades: midterm + final + assignment component scores PLUS one finalized
#             entity-style grade (final_score / letter_grade / gpa / status).
#   - invoices + payment_records: tuition payment records (VND), mix paid / partial / unpaid.
#
# All sample data is VN-localized per .claude/rules/vn-localization-audit-checklist.md §3
# (Vietnamese names, VND amounts e.g. 3.500.000đ/khóa, VN class names). NO Lorem Ipsum / John Doe.
#
# Connection: identical to seed-thesis-demo-tenants.sh (kite-postgres / kiteclass_shared).
#   PG_CONTAINER  — default "kite-postgres"
#   PG_DB         — default "kiteclass_shared"
#   PG_USER       — default "$POSTGRES_USER" or "kitehub"
#   PG_HOST       — default "localhost"
#   PG_PORT       — default "5433"
#   USE_DOCKER    — "true" (default) routes via `docker exec`; "false" requires PGPASSWORD
#
# Usage:
#   bash scripts/seed-sky-demo-enrich.sh           # seed (default)
#   bash scripts/seed-sky-demo-enrich.sh --dry-run # print SQL without executing
#   bash scripts/seed-sky-demo-enrich.sh --cleanup # remove enriched data (keeps base tenant)
#   bash scripts/seed-sky-demo-enrich.sh --help    # show this help
#
# LIVE-RUN REQUIRES STACK UP: this script mutates kiteclass_shared via psql. It was
# authored + shellcheck-verified with the stack DOWN; a live seed run (and dashboard
# walk to confirm "alive" data) must happen once the local Docker stack is started
# via `bash kitehub/scripts/up.sh --profile full`.
#
# Per .claude/rules/agent-aws-access.md §4.3 Tier 3 — LOCAL-DEV ONLY by default. Do NOT
# run against production RDS without explicit authorization + pre-mutation state-check.

set -euo pipefail

# ---------- defaults ----------
PG_CONTAINER="${PG_CONTAINER:-kite-postgres}"
PG_DB="${PG_DB:-kiteclass_shared}"
PG_USER="${PG_USER:-${POSTGRES_USER:-kitehub}}"
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5433}"
USE_DOCKER="${USE_DOCKER:-true}"

# Sky Education tenant UUID (must match seed-thesis-demo-tenants.sh tenant_a)
# GAP-805: must match seed-thesis-demo-tenants.sh TENANT_A_ID + BrandingDataSeeder Sky id.
# a5e0… (was 11111111-… which collided with thanglong DEV_TENANT_ID → broke isolation).
SKY_ID="e8ff87e1-69fc-4842-a263-7385c68b4ffb"

# ---------- mode parsing ----------
MODE="seed"
case "${1:-}" in
    --dry-run)  MODE="dry-run" ;;
    --cleanup)  MODE="cleanup" ;;
    --help|-h)
        sed -n '2,52p' "$0" | sed 's/^# \{0,1\}//'
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
        if ! docker ps --format '{{.Names}}' | grep -q "^${PG_CONTAINER}$"; then
            echo "ERROR: container '$PG_CONTAINER' not running" >&2
            echo "  → run: bash kitehub/scripts/up.sh --profile full" >&2
            exit 3
        fi
        docker exec -i "$PG_CONTAINER" psql -v ON_ERROR_STOP=1 \
            -U "$PG_USER" -d "$PG_DB" <<<"$sql"
    else
        if [[ -z "${PGPASSWORD:-}" ]]; then
            echo "ERROR: PGPASSWORD env required when USE_DOCKER=false" >&2
            exit 4
        fi
        psql -v ON_ERROR_STOP=1 \
            -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$PG_DB" <<<"$sql"
    fi
}

# ---------- mode: cleanup ----------
do_cleanup() {
    log "Cleanup mode — removing ENRICHED Sky data (base tenant preserved)"
    log "  Targets only enriched classes (codes CLS-ENG-IELTS65, CLS-ENG-COMM-EVE) + their dependents"

    local sql
    sql=$(cat <<EOF
-- Break-glass: disable RLS (per V58 migration comment)
SET row_security = off;

BEGIN;

-- Delete in FK dependency order, scoped to enriched classes only.
-- Enriched classes identified by code prefix 'CLS-ENG-IELTS65' / 'CLS-ENG-COMM-EVE'
-- plus the enriched cohort students (email domain @sky-enrich.demo).

-- payment_records → invoices (enriched students)
DELETE FROM payment_records pr
USING invoices i, students s
WHERE pr.invoice_id = i.id
  AND i.student_id = s.id
  AND s.instance_id = '${SKY_ID}'
  AND s.email LIKE '%@sky-enrich.demo';

DELETE FROM invoices i
USING students s
WHERE i.student_id = s.id
  AND s.instance_id = '${SKY_ID}'
  AND s.email LIKE '%@sky-enrich.demo';

-- grades / attendance / enrollments tied to enriched students
DELETE FROM grades     WHERE instance_id = '${SKY_ID}'
  AND student_id IN (SELECT id FROM students WHERE instance_id = '${SKY_ID}' AND email LIKE '%@sky-enrich.demo');

DELETE FROM attendance WHERE instance_id = '${SKY_ID}'
  AND student_id IN (SELECT id FROM students WHERE instance_id = '${SKY_ID}' AND email LIKE '%@sky-enrich.demo');

DELETE FROM enrollments WHERE instance_id = '${SKY_ID}'
  AND student_id IN (SELECT id FROM students WHERE instance_id = '${SKY_ID}' AND email LIKE '%@sky-enrich.demo');

-- class_sessions for enriched classes
DELETE FROM class_sessions cs
USING classes c
WHERE cs.class_id = c.id
  AND c.instance_id = '${SKY_ID}'
  AND c.code IN ('CLS-ENG-IELTS65', 'CLS-ENG-COMM-EVE');

-- enriched classes + courses
DELETE FROM classes WHERE instance_id = '${SKY_ID}' AND code IN ('CLS-ENG-IELTS65', 'CLS-ENG-COMM-EVE');
DELETE FROM courses WHERE instance_id = '${SKY_ID}' AND code IN ('ENG-IELTS65', 'ENG-COMM-EVE');

-- enriched students
DELETE FROM students WHERE instance_id = '${SKY_ID}' AND email LIKE '%@sky-enrich.demo';

COMMIT;

SELECT 'sky enriched students remaining' AS label, count(*) FROM students WHERE instance_id = '${SKY_ID}' AND email LIKE '%@sky-enrich.demo';
EOF
)
    run_sql "$sql"
    log "Cleanup complete"
}

# ---------- mode: seed ----------
do_seed() {
    log "Seed mode — enriching Sky Education ($SKY_ID)"
    log "  USE_DOCKER=$USE_DOCKER  PG_CONTAINER=$PG_CONTAINER  PG_DB=$PG_DB  PG_USER=$PG_USER"

    local sql
    sql=$(cat <<'PLPGSQL'
-- Break-glass: disable RLS for seeding (per V58 migration comment §22-23).
-- Seed runs as table owner with FORCE ROW LEVEL SECURITY applied.
SET row_security = off;

DO $seed$
DECLARE
    sky_id     UUID := 'e8ff87e1-69fc-4842-a263-7385c68b4ffb';
    teacher_id BIGINT;
    rec        RECORD;

    -- Class definitions: code, name, course_code, course_name, tuition (VND), max
    -- Anh ngữ 5A1 reuses existing course ENG-5A from seed-thesis. The other two are new.
    classes_def CONSTANT TEXT[][] := ARRAY[
        ARRAY['CLS-ENG-5A',       'Lớp Anh ngữ 5A1',          'ENG-5A',       'Anh ngữ Cấp 5',            '3500000', '30'],
        ARRAY['CLS-ENG-IELTS65',  'Lớp IELTS 6.5 Cấp tốc',    'ENG-IELTS65',  'Luyện thi IELTS 6.5',      '6500000', '20'],
        ARRAY['CLS-ENG-COMM-EVE', 'Lớp Giao tiếp Tối',        'ENG-COMM-EVE', 'Anh ngữ Giao tiếp Buổi tối','2800000', '25']
    ];

    -- 30 diverse Vietnamese student names. Each enriched class draws a slice.
    vn_names CONSTANT TEXT[] := ARRAY[
        'Trần Thị Hồng', 'Nguyễn Văn An', 'Phạm Thị Mai', 'Lê Văn Quang',
        'Hoàng Thị Lan', 'Vũ Minh Tuấn', 'Đặng Thị Hương', 'Bùi Văn Dũng',
        'Đỗ Thị Thanh', 'Ngô Quốc Bảo', 'Dương Thị Kim', 'Lý Văn Hùng',
        'Phan Thị Ngọc', 'Trương Minh Khôi', 'Hồ Thị Yến', 'Đinh Văn Phúc',
        'Mai Thị Trang', 'Cao Văn Sơn', 'Lưu Thị Diệu', 'Tạ Quang Vinh',
        'Châu Thị Bích', 'Võ Văn Tài', 'Đoàn Thị Nhung', 'Huỳnh Minh Đức',
        'Trịnh Thị Hà', 'Lâm Văn Thịnh', 'Quách Thị Loan', 'Nông Văn Kiên',
        'Tô Thị Tuyết', 'Hà Quang Long'
    ];

    -- NOTE: all holding variables are v_-prefixed to avoid PL/pgSQL shadowing of
    -- table column names (e.g. plain `class_id` in `WHERE class_id = class_id` is
    -- always-true ambiguous). Qualified column refs + v_ vars eliminate that bug.
    v_teacher_id    BIGINT;
    v_class_id      BIGINT;
    v_course_id     BIGINT;
    v_student_id    BIGINT;
    v_enroll_id     BIGINT;
    v_session_id    BIGINT;
    v_invoice_id    BIGINT;
    v_tuition       NUMERIC(12,2);
    v_cohort_size   INT;
    v_ci            INT;   -- class index
    v_si            INT;   -- student index
    v_sess          INT;
    v_name_idx      INT;
    v_stu_name      TEXT;
    v_stu_email     TEXT;
    v_att_status    TEXT;
    v_final_sc      NUMERIC(5,2);
    v_letter        VARCHAR(5);
    v_gpa           NUMERIC(3,2);
    v_grade_status  VARCHAR(20);
    v_amount_paid   NUMERIC(12,2);
    v_inv_status    TEXT;
    v_paymethod     TEXT;
    base_session_date DATE := CURRENT_DATE - INTERVAL '21 days';
BEGIN
    -- Resolve Sky's teacher (created by seed-thesis-demo-tenants.sh). Fallback: create one.
    SELECT t.id INTO v_teacher_id FROM teachers t WHERE t.instance_id = sky_id ORDER BY t.id LIMIT 1;
    IF v_teacher_id IS NULL THEN
        INSERT INTO teachers (instance_id, name, email, phone, department, specialization, status, created_at, updated_at)
        VALUES (sky_id, 'Trần Thị Hồng', 'gv.hong@sky-enrich.demo', '0901 234 567', 'Anh ngữ', 'IELTS, Giao tiếp', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id INTO v_teacher_id;
    END IF;

    -- Iterate each enriched class definition.
    FOR v_ci IN 1 .. array_length(classes_def, 1) LOOP
        v_tuition := classes_def[v_ci][5]::NUMERIC;

        -- Ensure course exists (reuse if seed-thesis already created it).
        SELECT c.id INTO v_course_id FROM courses c
          WHERE c.instance_id = sky_id AND c.code = classes_def[v_ci][3] LIMIT 1;
        IF v_course_id IS NULL THEN
            INSERT INTO courses (instance_id, code, name, description, status, created_at, updated_at)
            VALUES (sky_id, classes_def[v_ci][3], classes_def[v_ci][4],
                    'Khóa học ' || classes_def[v_ci][4] || ' — Sky Education', 'PUBLISHED',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id INTO v_course_id;
        END IF;

        -- Ensure class exists (reuse if present, else create).
        SELECT cl.id INTO v_class_id FROM classes cl
          WHERE cl.instance_id = sky_id AND cl.code = classes_def[v_ci][1] LIMIT 1;
        IF v_class_id IS NULL THEN
            INSERT INTO classes (instance_id, course_id, code, name, teacher_id,
                                 start_date, end_date, max_students, tuition_amount, tuition_type, status,
                                 created_at, updated_at)
            VALUES (sky_id, v_course_id, classes_def[v_ci][1], classes_def[v_ci][2], NULL::uuid,
                    base_session_date, base_session_date + INTERVAL '90 days',
                    classes_def[v_ci][6]::INT, v_tuition, 'fixed', 'IN_PROGRESS',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id INTO v_class_id;
        END IF;

        -- Create 6 class sessions per class (3 weeks × 2 sessions/week).
        FOR v_sess IN 1 .. 6 LOOP
            SELECT cs.id INTO v_session_id FROM class_sessions cs
              WHERE cs.class_id = v_class_id AND cs.session_date = base_session_date + (v_sess * 3)
              LIMIT 1;
            IF v_session_id IS NULL THEN
                INSERT INTO class_sessions (class_id, session_number, session_date, start_time, end_time, topic, status, created_at, updated_at)
                VALUES (v_class_id, v_sess, base_session_date + (v_sess * 3), '18:00', '20:00',
                        'Buổi ' || v_sess || ' — ' || classes_def[v_ci][2], 'completed',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
            END IF;
        END LOOP;

        -- Cohort size: capped at class max_students and the 30-name array length.
        v_cohort_size := LEAST(classes_def[v_ci][6]::INT, array_length(vn_names, 1));

        FOR v_si IN 1 .. v_cohort_size LOOP
            v_name_idx  := ((v_si - 1) % array_length(vn_names, 1)) + 1;
            v_stu_name  := vn_names[v_name_idx];
            -- Unique enrich email per (class, slot) to avoid cross-class student collision.
            v_stu_email := lower('hs' || v_si || '-' || replace(classes_def[v_ci][1], '_', '-') || '@sky-enrich.demo');

            SELECT s.id INTO v_student_id FROM students s
              WHERE s.instance_id = sky_id AND s.email = v_stu_email LIMIT 1;
            IF v_student_id IS NULL THEN
                INSERT INTO students (instance_id, name, email, phone, status, created_at, updated_at)
                VALUES (sky_id, v_stu_name, v_stu_email,
                        '09' || lpad(((v_si * 7 + v_ci * 13) % 100000000)::TEXT, 8, '0'),
                        'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id INTO v_student_id;
            END IF;

            -- Enrollment (unique on class_id+student_id).
            SELECT e.id INTO v_enroll_id FROM enrollments e
              WHERE e.class_id = v_class_id AND e.student_id = v_student_id LIMIT 1;
            IF v_enroll_id IS NULL THEN
                INSERT INTO enrollments (instance_id, class_id, student_id, enrolled_at, status, created_at, updated_at)
                VALUES (sky_id, v_class_id, v_student_id, base_session_date, 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
            END IF;

            -- ATTENDANCE: one record per session, realistic mix.
            -- Distribution skewed to present (~75%), with late / absent / excused sprinkled in.
            FOR rec IN
                SELECT cs.id AS sid, cs.session_number AS sn FROM class_sessions cs
                  WHERE cs.class_id = v_class_id ORDER BY cs.session_number
            LOOP
                v_att_status := CASE ((v_si + rec.sn) % 13)
                    WHEN 0 THEN 'absent'
                    WHEN 1 THEN 'late'
                    WHEN 2 THEN 'excused'
                    WHEN 7 THEN 'late'
                    ELSE 'present'
                END;
                INSERT INTO attendance (instance_id, session_id, student_id, status, check_in_time, marked_by, marked_at, created_at, updated_at)
                VALUES (sky_id, rec.sid, v_student_id, v_att_status,
                        CASE WHEN v_att_status IN ('present', 'late') THEN CURRENT_TIMESTAMP ELSE NULL END,
                        v_teacher_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (session_id, student_id) DO NOTHING;
            END LOOP;

            -- GRADES (legacy component-style rows: midterm + assignment + final).
            -- score on 0..10 scale per V1 chk_grades_score; deterministic spread per index.
            -- Guarded by NOT EXISTS so re-run is idempotent.
            IF NOT EXISTS (
                SELECT 1 FROM grades g
                WHERE g.instance_id = sky_id AND g.class_id = v_class_id AND g.student_id = v_student_id
                  AND g.grade_type = 'final'
            ) THEN
                INSERT INTO grades (instance_id, class_id, student_id, grade_type, title, score, max_score, weight, feedback, graded_date, status, pass_threshold, deleted, version, created_at, updated_at)
                VALUES
                  (sky_id, v_class_id, v_student_id, 'midterm',    'Kiểm tra giữa kỳ', round((6.0 + ((v_si * 3) % 40) / 10.0)::numeric, 1), 10, 0.30, 'Cần luyện thêm ngữ pháp', base_session_date + 10, 'IN_PROGRESS', 50.0, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                  (sky_id, v_class_id, v_student_id, 'assignment', 'Bài tập về nhà',   round((7.0 + ((v_si * 5) % 30) / 10.0)::numeric, 1), 10, 0.20, 'Hoàn thành tốt',         base_session_date + 14, 'IN_PROGRESS', 50.0, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                  (sky_id, v_class_id, v_student_id, 'final',      'Thi cuối kỳ',      round((6.5 + ((v_si * 7) % 35) / 10.0)::numeric, 1), 10, 0.50, 'Tiến bộ rõ rệt',         base_session_date + 20, 'IN_PROGRESS', 50.0, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
            END IF;

            -- One FINALIZED entity-style grade per student per class (dashboard reads final_score/letter/gpa).
            v_final_sc := round((55.0 + ((v_si * 11) % 45))::numeric, 1);  -- 55..99 on 0-100 scale
            IF    v_final_sc >= 85 THEN v_letter := 'A';  v_gpa := 4.0;
            ELSIF v_final_sc >= 70 THEN v_letter := 'B';  v_gpa := 3.0;
            ELSIF v_final_sc >= 55 THEN v_letter := 'C';  v_gpa := 2.0;
            ELSE                        v_letter := 'D';  v_gpa := 1.0;
            END IF;
            v_grade_status := CASE WHEN v_final_sc >= 50 THEN 'PASSED' ELSE 'FAILED' END;

            IF NOT EXISTS (
                SELECT 1 FROM grades g
                WHERE g.instance_id = sky_id AND g.class_id = v_class_id AND g.student_id = v_student_id
                  AND g.final_score IS NOT NULL AND g.deleted = false
            ) THEN
                INSERT INTO grades (instance_id, class_id, student_id, final_score, letter_grade, gpa, status, pass_threshold, comments, calculated_at, finalized_at, finalized_by, deleted, version, created_at, updated_at)
                VALUES (sky_id, v_class_id, v_student_id, v_final_sc, v_letter, v_gpa, v_grade_status, 50.0,
                        'Điểm tổng kết khóa học', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, v_teacher_id, false, 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
            END IF;

            -- INVOICE + PAYMENT: tuition fee. Mix paid / partial / unpaid by index slot.
            SELECT inv.id INTO v_invoice_id FROM invoices inv
              WHERE inv.instance_id = sky_id AND inv.student_id = v_student_id AND inv.class_id = v_class_id LIMIT 1;
            IF v_invoice_id IS NULL THEN
                -- Payment state distribution by (index mod 4): 1=partial, 2=unpaid, else paid.
                CASE (v_si % 4)
                    WHEN 1 THEN v_amount_paid := round(v_tuition / 2, 0); v_inv_status := 'partially_paid';
                    WHEN 2 THEN v_amount_paid := 0;                       v_inv_status := 'pending';
                    ELSE        v_amount_paid := v_tuition;               v_inv_status := 'paid';
                END CASE;

                INSERT INTO invoices (instance_id, invoice_number, student_id, class_id, period_start, period_end,
                                      subtotal, discount, total, amount_paid, issue_date, due_date, status, notes,
                                      created_at, updated_at)
                VALUES (sky_id,
                        'INV-SKY-' || classes_def[v_ci][1] || '-' || lpad(v_si::TEXT, 3, '0'),
                        v_student_id, v_class_id, base_session_date, base_session_date + INTERVAL '90 days',
                        v_tuition, 0, v_tuition, v_amount_paid,
                        base_session_date, base_session_date + INTERVAL '15 days', v_inv_status,
                        'Học phí khóa ' || classes_def[v_ci][2], CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id INTO v_invoice_id;

                -- Payment record(s) for paid/partial invoices (cash / bank / VietQR mix).
                IF v_amount_paid > 0 THEN
                    v_paymethod := CASE (v_si % 3)
                        WHEN 0 THEN 'CASH'
                        WHEN 1 THEN 'BANK_TRANSFER'
                        ELSE        'VIETQR'
                    END;
                    INSERT INTO payment_records (instance_id, invoice_id, method, amount, paid_at, note, recorded_by, created_at, deleted, version)
                    VALUES (sky_id, v_invoice_id, v_paymethod, v_amount_paid, base_session_date + INTERVAL '2 days',
                            'Thu học phí ' || classes_def[v_ci][2], v_teacher_id, CURRENT_TIMESTAMP, false, 0);
                END IF;
            END IF;
        END LOOP;
    END LOOP;
END
$seed$;
PLPGSQL
)
    run_sql "$sql"

    if [[ "$MODE" != "dry-run" ]]; then
        log "Verifying enriched data..."
        run_sql "SET row_security = off;
SELECT 'sky enriched classes'   AS label, count(*) FROM classes        WHERE instance_id = '${SKY_ID}' AND code IN ('CLS-ENG-5A','CLS-ENG-IELTS65','CLS-ENG-COMM-EVE');
SELECT 'sky enriched students'  AS label, count(*) FROM students       WHERE instance_id = '${SKY_ID}' AND email LIKE '%@sky-enrich.demo';
SELECT 'sky enrollments'        AS label, count(*) FROM enrollments    WHERE instance_id = '${SKY_ID}';
SELECT 'sky attendance rows'    AS label, count(*) FROM attendance     WHERE instance_id = '${SKY_ID}';
SELECT 'sky grade rows'         AS label, count(*) FROM grades         WHERE instance_id = '${SKY_ID}';
SELECT 'sky invoices'           AS label, count(*) FROM invoices       WHERE instance_id = '${SKY_ID}';
SELECT 'sky payment_records'    AS label, count(*) FROM payment_records pr WHERE pr.instance_id = '${SKY_ID}';"
    fi

    log "Enrich complete"
    log ""
    log "Sky Education demo now has attendance + grade + payment depth for dashboard demo."
    log "LIVE-RUN reminder: this script must be run once with stack UP to actually populate data,"
    log "  then walk the KiteClass dashboard to confirm attendance/grade/payment surfaces look alive."
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
