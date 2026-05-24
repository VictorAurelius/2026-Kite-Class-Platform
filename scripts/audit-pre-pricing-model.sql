-- ============================================================================
-- Pre-Migration Audit Script — Wave beta-readiness-4 Bucket C (GAP-292)
-- ============================================================================
-- Run BEFORE V67__add_pricing_model_to_courses.sql applies in production.
--
-- Purpose: Identify tenants có existing courses với legacy `price > 0` để
-- chuẩn bị email reclassification "Bạn muốn migrate sang PER_HOUR / MONTHLY /
-- COURSE_PACKAGE? Default V67 set PER_HOUR — review trước khi go-live."
--
-- Per Agent 3 Cell 12 mandate (cross-bucket Bucket D audit cell pattern):
-- pre-migration data state guides post-migration outreach.
--
-- Manual run instructions:
--   psql -U kite -d kitehub -f scripts/audit-pre-pricing-model.sql
--   OR via Docker exec:
--   docker exec kite-postgres psql -U kite -d kitehub -f /scripts/audit-pre-pricing-model.sql
--
-- Output: stdout report — copy/paste vào email runbook cho Phase 1 BETA owners.
-- ============================================================================

\echo '============================================'
\echo 'Audit: Courses with legacy price > 0 by tenant'
\echo '============================================'

SELECT
    instance_id AS tenant_id,
    COUNT(*) AS course_count,
    MIN(price) AS min_price,
    MAX(price) AS max_price,
    AVG(price)::NUMERIC(15,2) AS avg_price,
    STRING_AGG(DISTINCT level, ', ' ORDER BY level) AS levels
FROM courses
WHERE price IS NOT NULL
  AND price > 0
  AND deleted = FALSE
GROUP BY instance_id
ORDER BY course_count DESC;

\echo ''
\echo '============================================'
\echo 'Courses by name (sample for manual review)'
\echo '============================================'

SELECT
    instance_id AS tenant_id,
    id AS course_id,
    name,
    code,
    level,
    price AS legacy_price_vnd,
    total_sessions,
    duration_weeks,
    CASE
        WHEN level ILIKE '%IELTS%' OR level ILIKE '%TOEIC%' OR name ILIKE '%bundle%' THEN 'COURSE_PACKAGE'
        WHEN duration_weeks IS NOT NULL AND total_sessions IS NOT NULL THEN 'PER_HOUR'
        WHEN price < 2000000 THEN 'MONTHLY (small fee likely monthly)'
        ELSE 'PER_HOUR (default suggestion per V67)'
    END AS suggested_pricing_model
FROM courses
WHERE price IS NOT NULL
  AND price > 0
  AND deleted = FALSE
ORDER BY instance_id, id
LIMIT 50;

\echo ''
\echo '============================================'
\echo 'Total tenants requiring reclassification email'
\echo '============================================'

SELECT
    COUNT(DISTINCT instance_id) AS tenants_to_email,
    COUNT(*) AS total_courses_to_review
FROM courses
WHERE price IS NOT NULL
  AND price > 0
  AND deleted = FALSE;

\echo ''
\echo '============================================'
\echo 'NEXT STEPS:'
\echo '  1. Copy tenant_id list to email runbook'
\echo '  2. Send reclassification email cho Phase 1 BETA owners'
\echo '     Subject: [KiteClass] Vui lòng cập nhật hình thức tính học phí'
\echo '  3. Owners reply với pricing_model choice → admin update via PUT /api/v1/courses/{id}'
\echo '  4. After 7-day window, default PER_HOUR áp dụng cho non-responders'
\echo '============================================'
