-- GAP-323 Phase 1A (Wave 18b1 Bucket F): per-period (per-tiết) attendance for K-12 schools.
--
-- K-12 fundamentally differs from centers: 5-10 tiết/day, each tiết a different
-- bộ môn teacher (TT 22/2021/TT-BGDĐT). The legacy `attendance` table stays in
-- place for CENTER tenants (per-day model). K-12 tenants
-- (`tenant.vertical_type = 'K12_SCHOOL'`, see kitehub-subscription V24) populate
-- this new table.
--
-- Phase 1A scope: schema + read-only API. Write API, GVCN mobile UI, daily
-- roll-up view (vắng cả ngày = vắng ≥7 tiết), and concurrent load test
-- (30 GVCN) ship in GAP-323b. GradeFormulaService + state machine ship in
-- GAP-323c.
--
-- Backward compat: existing CENTER tenants are unaffected. CENTER tenants
-- continue to write to `attendance`, never to `attendance_period`.

CREATE TABLE attendance_period (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,

    -- Domain columns
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    subject_section_id BIGINT NOT NULL,
    period_no INTEGER NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    recorded_by BIGINT NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    notes VARCHAR(500),

    -- BaseEntity audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_att_period_status CHECK (
        status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED', 'MAKEUP')
    ),
    CONSTRAINT chk_att_period_no_positive CHECK (period_no > 0)
);

-- BR-PERIOD-ATT-003: unique per (student, subject_section, date, period_no, instance, deleted)
CREATE UNIQUE INDEX uk_att_period_student_section_date_period
    ON attendance_period(student_id, subject_section_id, date, period_no, instance_id)
    WHERE deleted = FALSE;

-- Query indexes
CREATE INDEX idx_att_period_student_date
    ON attendance_period(student_id, date);

CREATE INDEX idx_att_period_class_date
    ON attendance_period(class_id, date);

CREATE INDEX idx_att_period_subject_section
    ON attendance_period(subject_section_id);

CREATE INDEX idx_att_period_instance_id
    ON attendance_period(instance_id);

CREATE INDEX idx_att_period_deleted
    ON attendance_period(deleted);

CREATE INDEX idx_att_period_recorded_by
    ON attendance_period(recorded_by);

COMMENT ON TABLE attendance_period IS
    'GAP-323 Phase 1A: per-tiết attendance for K-12 tenants (TT 22/2021/TT-BGDĐT). CENTER tenants continue to use `attendance` (per-day). Phase 1B will add CHECK constraint enforcing tenant.vertical_type = K12_SCHOOL writes here exclusively.';

COMMENT ON COLUMN attendance_period.subject_section_id IS
    'FK to subject_sections.id (GAP-054 Phase 1). Identifies môn + lớp bộ môn for this period.';

COMMENT ON COLUMN attendance_period.period_no IS
    'Tiết number (1..10 typical). DB allows broader values; service layer + Phase 1B CHECK constraint will enforce K-12 contract.';

COMMENT ON COLUMN attendance_period.date IS
    'Lesson date (separate from recorded_at to allow back-dated entry within audit window).';

COMMENT ON COLUMN attendance_period.recorded_at IS
    'Server-side timestamp at recording (used for ≤2 min SLA reporting AC-OPS-001).';
