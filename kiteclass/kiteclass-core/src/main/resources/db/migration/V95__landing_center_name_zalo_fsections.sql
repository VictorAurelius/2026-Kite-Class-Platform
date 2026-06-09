-- GAP-1083 (wave-landing-100 follow-up): per-tenant centerName + Zalo CTA + F-section data.
--
-- Wave landing-100 shipped the FE for these (nav/footer/JsonLd prefer centerName over the
-- heroTitle slogan; FloatingCTA Zalo deep-link; ProblemSolution/HowItWorks/TrustStrip sections)
-- but the LandingPageResponse lacked the backing columns, so every tenant fell back to default
-- copy. These 5 nullable columns close the data-binding gap so each tenant renders its own data.
--
-- JSONB columns follow the V76 pattern — bound by Hibernate @JdbcTypeCode(SqlTypes.JSON) on
-- JsonNode entity fields (GAP-220 pattern). All nullable; getOrCreateDefault leaves them null
-- until the tenant customises, and the FE falls back to each section's VN default when null.

ALTER TABLE landing_pages
    ADD COLUMN IF NOT EXISTS center_name       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS landing_zalo_url  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS problem_solution  JSONB,
    ADD COLUMN IF NOT EXISTS how_it_works      JSONB,
    ADD COLUMN IF NOT EXISTS trust_strip       JSONB;
