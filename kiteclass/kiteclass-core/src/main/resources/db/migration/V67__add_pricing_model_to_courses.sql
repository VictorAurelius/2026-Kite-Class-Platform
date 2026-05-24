-- ============================================================================
-- V67: Add pricing_model + unit_price columns to courses table (GAP-292)
-- ============================================================================
-- Wave beta-readiness-4 Bucket C — Pricing PER_HOUR taxonomy
--
-- Adds 2 columns to courses:
--   - pricing_model VARCHAR(30) — PricingModel enum: PER_HOUR | MONTHLY | COURSE_PACKAGE | FREE
--   - unit_price NUMERIC(19,2) — interpreted per pricing_model (per giờ / per tháng / per khoá / 0 đ)
--
-- Default pricing_model = 'PER_HOUR' (VN TT Anh ngữ market dominant pattern per ADR-027 +
-- Apollo 257-344k/giờ, ILA 195-368k/giờ market benchmark).
--
-- Default unit_price = 0 — Trung tâm chủ MUST update sau khi migrate, KHÔNG tự động charge.
-- Pre-migration audit script: scripts/audit-pre-pricing-model.sql guides reclassification.
--
-- Business Rules:
--   BR-COURSE-PRICING-001: pricing_model NOT NULL, immutable post first active enrollment
--   BR-COURSE-PRICING-002: unit_price semantics depend on pricing_model
--   BR-COURSE-PRICING-003: pricing_model set at course creation
--
-- Legacy column 'price' (NUMERIC(15,2)) PRESERVED for backward-compatibility (Course.java @Deprecated);
-- new code MUST NOT write to it. Future V-migration sẽ mark NOT USED.
--
-- Rollback path: R67__undo_pricing_model.sql (manual execution).
-- ============================================================================

ALTER TABLE courses
    ADD COLUMN pricing_model VARCHAR(30) NOT NULL DEFAULT 'PER_HOUR';

ALTER TABLE courses
    ADD COLUMN unit_price NUMERIC(19, 2) NOT NULL DEFAULT 0;

-- Add CHECK constraint enforcing pricing_model whitelist (defense-in-depth beyond JPA EnumType.STRING)
ALTER TABLE courses
    ADD CONSTRAINT chk_courses_pricing_model
    CHECK (pricing_model IN ('PER_HOUR', 'MONTHLY', 'COURSE_PACKAGE', 'FREE'));

-- Add CHECK constraint: unit_price MUST be >= 0 (no negative pricing)
ALTER TABLE courses
    ADD CONSTRAINT chk_courses_unit_price_non_negative
    CHECK (unit_price >= 0);

-- Add CHECK constraint: FREE pricing model MUST have unit_price = 0
ALTER TABLE courses
    ADD CONSTRAINT chk_courses_free_zero_price
    CHECK (pricing_model != 'FREE' OR unit_price = 0);

COMMENT ON COLUMN courses.pricing_model IS
    'PricingModel enum: PER_HOUR | MONTHLY | COURSE_PACKAGE | FREE (Wave beta-readiness-4 Bucket C / ADR-027)';

COMMENT ON COLUMN courses.unit_price IS
    'Unit price in VND. Semantics depend on pricing_model: PER_HOUR=per giờ, MONTHLY=per tháng, COURSE_PACKAGE=per khoá, FREE=must be 0';
