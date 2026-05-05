-- =============================================================================
-- V55: Extend subject_grades for TT 22/2021 — Tổ trưởng workflow + assessment typing
-- Wave 19 Bucket B — GAP-323c Phase 1C v1
-- =============================================================================
--
-- Additive only. Existing rows are backfilled to backward-compat defaults:
--   - type = 'TX' (regular continuous assessment — only kind tracked pre-Phase-1C)
--   - weight = 1.0 (matches TX weight per BR-GRADEBOOK-004)
--   - status = 'DRAFT' (pre-Phase-1C rows treated as not-yet-reviewed)
--   - reviewed_by, published_at = NULL
--
-- BR-GRADEBOOK-001..005 (TT 22/2021/TT-BGDĐT Đ.7) defined in
--   documents/01-business/kiteclass/multi-subject-gradebook/rules.md
-- =============================================================================

-- 1. Add new columns. Default values cover existing rows; new code
--    paths set them explicitly.
ALTER TABLE subject_grades
    ADD COLUMN type VARCHAR(8) DEFAULT 'TX' NOT NULL,
    ADD COLUMN weight DECIMAL(4, 2) DEFAULT 1.0 NOT NULL,
    ADD COLUMN status VARCHAR(16) DEFAULT 'DRAFT' NOT NULL,
    ADD COLUMN reviewed_by BIGINT,
    ADD COLUMN published_at TIMESTAMP;

-- 2. Enum-domain CHECK constraints (mirrors Java enums to prevent drift).
ALTER TABLE subject_grades
    ADD CONSTRAINT chk_sg_type CHECK (type IN ('TX', 'GK', 'CK'));

ALTER TABLE subject_grades
    ADD CONSTRAINT chk_sg_status CHECK (status IN ('DRAFT', 'REVIEWED', 'PUBLISHED'));

-- 3. Weight bounds (TX=1.0, GK=2.0, CK=3.0 per BR-GRADEBOOK-004; allow
--    0.0..10.0 envelope for future TT amendments).
ALTER TABLE subject_grades
    ADD CONSTRAINT chk_sg_weight CHECK (weight >= 0 AND weight <= 10);

-- 4. Indexes for Tổ trưởng / Hiệu trưởng review queue + formula service queries.
CREATE INDEX idx_sg_status ON subject_grades(status) WHERE deleted = FALSE;
CREATE INDEX idx_sg_subject_section_status ON subject_grades(subject_section_id, status)
    WHERE deleted = FALSE;
CREATE INDEX idx_sg_student_section_semester_type
    ON subject_grades(student_id, subject_section_id, semester_id, type)
    WHERE deleted = FALSE;
