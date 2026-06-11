-- GAP-826 — Landing multi-banner carousel: store an ordered list of hero banner
-- image URLs per tenant landing page. JSONB array of URL strings (slide order =
-- array order). Nullable for backward-compat: when empty/null the frontend falls
-- back to the existing single hero_image_url column (single-banner behaviour
-- unchanged). Mirrors the V95 JSONB landing-section columns (problem_solution, ...).
ALTER TABLE landing_pages ADD COLUMN hero_images JSONB;

COMMENT ON COLUMN landing_pages.hero_images IS
    'GAP-826: ordered hero banner carousel image URLs (JSONB array of strings); slide order = array order; null/empty falls back to hero_image_url (single banner).';
