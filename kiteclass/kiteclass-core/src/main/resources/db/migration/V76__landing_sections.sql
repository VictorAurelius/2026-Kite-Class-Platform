-- wave-thesis-4: data-driven landing sections.
--
-- Adds 7 nullable columns to landing_pages so the 6 marketing sections (About,
-- Teachers, Programs, Pricing, Testimonials, FAQ, Stats) read per-tenant content
-- from DB instead of hardcoded FE copy. JSONB columns are bound by Hibernate via
-- @JdbcTypeCode(SqlTypes.JSON) on List<Map<String,Object>> entity fields
-- (GAP-220 pattern). All nullable — getOrCreateDefault leaves them null until the
-- tenant customises landing content; FE falls back to defaults when null.

ALTER TABLE landing_pages
    ADD COLUMN IF NOT EXISTS about_text     TEXT,
    ADD COLUMN IF NOT EXISTS teachers       JSONB,
    ADD COLUMN IF NOT EXISTS programs       JSONB,
    ADD COLUMN IF NOT EXISTS pricing_tiers  JSONB,
    ADD COLUMN IF NOT EXISTS testimonials   JSONB,
    ADD COLUMN IF NOT EXISTS faqs           JSONB,
    ADD COLUMN IF NOT EXISTS stats          JSONB;
