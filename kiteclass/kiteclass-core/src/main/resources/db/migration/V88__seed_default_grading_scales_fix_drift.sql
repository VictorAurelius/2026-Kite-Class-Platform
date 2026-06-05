-- V88: Seed grading scales per-tenant + fix grading_scales schema drift (GAP-998, Wave flow-kc6)
--
-- Two coupled issues surfaced by KC-6 G1 pre-walk (production-equivalent):
--   1. grading_scales is EMPTY → every calculate/finalize throws 404
--      GRADING_SCALE_NOT_FOUND at the letter-grade mapping step
--      (GradeServiceImpl.mapGradeToLetterAndGpa).
--   2. Schema↔entity drift (KC-5 class): legacy columns grade/min_percentage/
--      max_percentage/gpa are NOT NULL no-default and the GradingScale entity
--      does not map them (it maps the V79 columns). GAP-875 (DONE) only ADDed
--      the new columns — never reconciled the legacy NOT-NULL ones. Invisible to
--      IT (ddl-auto=create-drop generates schema from entity).
--
-- Design note (→ GAP-1002): GradingScale extends BaseEntity → instance_id NOT NULL
-- + tenantFilter (instance_id=:tenantId) + RLS tenant_isolation. The code's
-- `findDefaultGradingScales() WHERE instance_id IS NULL` fallback is therefore
-- UNREACHABLE at request time (NULL never matches the tenant filter/RLS), and
-- instance_id can't be NULL anyway. So scales MUST be seeded per-tenant
-- (instance_id = the tenant) — which `findByInstanceIdAndScoreRange` resolves.
-- New-tenant provisioning seed + the dead NULL-default path are tracked in GAP-1002.

-- 1. Drop NOT NULL on legacy columns the entity no longer maps (avoid 23502 on
--    any future entity-driven scale save, e.g. Phase 1.5 tenant custom scales).
ALTER TABLE grading_scales ALTER COLUMN grade DROP NOT NULL;
ALTER TABLE grading_scales ALTER COLUMN min_percentage DROP NOT NULL;
ALTER TABLE grading_scales ALTER COLUMN max_percentage DROP NOT NULL;
ALTER TABLE grading_scales ALTER COLUMN gpa DROP NOT NULL;

-- 2. Seed the 8 default scales (per BR-GRD-005 letter bands + BR-GRD-006 GPA) for
--    EVERY existing tenant that has classes (= every tenant that can have grades).
--    Bands use .99 upper bounds so a 2-decimal final_score maps to exactly one band
--    under the repository query (score >= min_score AND score <= max_score).
--    is_passing=false only for F (pass/fail at grade level uses grade.pass-threshold).
--    Migration runs as the Flyway role (bypasses RLS) so per-tenant rows insert cleanly.
INSERT INTO grading_scales
    (instance_id, scale_name, letter_grade, min_score, max_score, gpa_value,
     is_default, is_passing, deleted, version, created_at, updated_at)
SELECT t.instance_id, b.scale_name, b.letter_grade, b.min_score, b.max_score,
       b.gpa_value, true, b.is_passing, false, 0, now(), now()
FROM (SELECT DISTINCT instance_id FROM classes WHERE instance_id IS NOT NULL) t
CROSS JOIN (VALUES
    ('Default', 'A+', 95.00, 100.00, 4.00, true),
    ('Default', 'A',  90.00,  94.99, 4.00, true),
    ('Default', 'B+', 85.00,  89.99, 3.30, true),
    ('Default', 'B',  80.00,  84.99, 3.00, true),
    ('Default', 'C+', 75.00,  79.99, 2.30, true),
    ('Default', 'C',  70.00,  74.99, 2.00, true),
    ('Default', 'D',  60.00,  69.99, 1.00, true),
    ('Default', 'F',   0.00,  59.99, 0.00, false)
) AS b(scale_name, letter_grade, min_score, max_score, gpa_value, is_passing);
