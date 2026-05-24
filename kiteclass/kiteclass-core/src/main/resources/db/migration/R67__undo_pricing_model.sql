-- ============================================================================
-- R67: ROLLBACK V67 — drop pricing_model + unit_price columns from courses
-- ============================================================================
-- MANUAL EXECUTION ONLY — NOT auto-applied by Flyway.
--
-- Run this script if Wave beta-readiness-4 Bucket C must be reverted post-deploy.
-- Pre-condition: NO code in production writes to courses.pricing_model/unit_price.
-- Post-rollback: Course entity will fail to start until @Deprecated price column is re-used.
--
-- Per .claude/rules/release-deploy-standard.md §4.4 rollback execution pattern.
-- Cross-reference: Wave plan §3.6 V67/V67b reserved; Agent 3 Cell 9 paired rollback mandate.
-- ============================================================================

-- Drop CHECK constraints first (PostgreSQL requires explicit drop before column drop)
ALTER TABLE courses DROP CONSTRAINT IF EXISTS chk_courses_free_zero_price;
ALTER TABLE courses DROP CONSTRAINT IF EXISTS chk_courses_unit_price_non_negative;
ALTER TABLE courses DROP CONSTRAINT IF EXISTS chk_courses_pricing_model;

ALTER TABLE courses DROP COLUMN IF EXISTS unit_price;
ALTER TABLE courses DROP COLUMN IF EXISTS pricing_model;
