-- ============================================================================
-- V85: Finance RLS re-assert + grades/courses legacy cleanup
-- ============================================================================
-- Wave 14 Bucket B2 (GAP-879 + GAP-904 + GAP-909).
--
-- DEPRECATE NOTICE — payments (V1) vs payment_records (V69):
--   `payment_records` (V69) is the CANONICAL Phase 1 BETA payment surface
--   (manual recording at trung tâm: CASH/BANK_TRANSFER/VIETQR/MOMO; flows
--   GAP-292b/GAP-705). The legacy `payments` table (V1, online-gateway oriented)
--   is DEPRECATED and is NOT dropped — entity drift + gateway flows are out of
--   B2 scope (see GAP-880). New finance code MUST target payment_records.
--
-- Part 1 (GAP-879): re-assert RLS on payment_records (already applied V78;
--   idempotent re-assert so V85 owns GAP-879 closure).
-- Part 2 (GAP-904): grades — add final_score CHECK (0-100, the entity-used
--   column), drop legacy chk_grades_score (on dropped legacy `score`), DROP 7
--   zero-usage legacy V1 columns. grade_type KEPT (entity-mapped + UK V74).
-- Part 3 (GAP-909): courses — DROP 3 zero-usage legacy V1 columns. cover_image_url
--   already present (V27) + entity-mapped → no change. price KEPT (@Deprecated,
--   still referenced by CourseMapper/IT fixtures).
--
-- Zero-usage verified 2026-06 via grep of kiteclass-core src (entity + repo +
-- native @Query + resources + test): no references to the dropped columns.
-- Forward-only, idempotent.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Part 1 — payment_records RLS (GAP-879), idempotent re-assert
-- ---------------------------------------------------------------------------
ALTER TABLE payment_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_records FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON payment_records;
CREATE POLICY tenant_isolation ON payment_records
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

COMMENT ON TABLE payments IS
    'DEPRECATED (Wave 14 B2 / GAP-879): legacy online-gateway payment table (V1). '
    'Canonical Phase 1 BETA payment surface = payment_records (V69). Not dropped — '
    'gateway entity reconcile is GAP-880 scope. New finance code MUST use payment_records.';

-- ---------------------------------------------------------------------------
-- Part 2 — grades cleanup (GAP-904)
-- ---------------------------------------------------------------------------
-- Replace legacy CHECK (on dropped `score`) with CHECK on entity-used final_score.
-- final_score is 0-100 scale (Grade.java javadoc + V64 comment + rules.md BR-GRD-005).
ALTER TABLE grades DROP CONSTRAINT IF EXISTS chk_grades_score;
ALTER TABLE grades DROP CONSTRAINT IF EXISTS chk_grades_final_score;
ALTER TABLE grades ADD CONSTRAINT chk_grades_final_score
    CHECK (final_score IS NULL OR (final_score >= 0 AND final_score <= 100));

-- DROP 7 zero-usage legacy V1 columns. grade_type KEPT (entity Grade.gradeType +
-- uk_grades_student_class_type V74).
ALTER TABLE grades DROP COLUMN IF EXISTS title;
ALTER TABLE grades DROP COLUMN IF EXISTS score;
ALTER TABLE grades DROP COLUMN IF EXISTS max_score;
ALTER TABLE grades DROP COLUMN IF EXISTS weight;
ALTER TABLE grades DROP COLUMN IF EXISTS feedback;
ALTER TABLE grades DROP COLUMN IF EXISTS graded_date;
ALTER TABLE grades DROP COLUMN IF EXISTS graded_by;

COMMENT ON CONSTRAINT chk_grades_final_score ON grades IS
    'final_score 0-100 (GAP-904 Wave 14 B2). Replaces legacy chk_grades_score on dropped score column.';

-- ---------------------------------------------------------------------------
-- Part 3 — courses cleanup (GAP-909)
-- ---------------------------------------------------------------------------
-- DROP 3 zero-usage legacy V1 columns. cover_image_url (V27) + price (@Deprecated)
-- KEPT — both still mapped/referenced.
ALTER TABLE courses DROP COLUMN IF EXISTS thumbnail_url;
ALTER TABLE courses DROP COLUMN IF EXISTS suggested_tuition;
ALTER TABLE courses DROP COLUMN IF EXISTS default_sessions;
