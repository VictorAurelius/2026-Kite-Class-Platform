-- GAP-535 (Wave 77 Bucket D): Tenant slug normalize for VN diacritics + smart quotes + collision recovery.
--
-- Wave 77 outside-in failure-mode matrix F2 (P0): P2 Center owner pastes
-- 'Trường Mầm Non "Hoa Mai"' (with smart quotes from Word/Zalo) into signup
-- form → without normalization the slug pipeline mishandles diacritics +
-- smart-quote codepoints (U+201C/U+201D), producing empty slug, mismatch
-- with subdomain validation, or DB constraint violation.
--
-- The existing instances table uses `subdomain` as the public slug (column
-- present since V1). This migration adds a separate `slug` column to hold
-- the normalized form alongside the original `organization_name` (preserved
-- with diacritics for display) — same row, two surfaces:
--   * organization_name: display name with diacritics + smart quotes intact
--   * subdomain: legacy column (kept for backward-compat); equal to slug for
--                new rows; may differ for legacy rows
--   * slug: normalized form (NFC + stripAccents + lowercase + dash-collapse)
--
-- Collision recovery is service-side (TenantSlugNormalizer) — append `-1`/`-2`
-- suffix until unique, cap retries 10.

ALTER TABLE instances
    ADD COLUMN slug VARCHAR(120) NULL;

-- Unique partial index: enforce slug uniqueness only when set (NULL allowed
-- for grandfathered rows pre-Wave-77 that retain only subdomain).
CREATE UNIQUE INDEX idx_instances_slug_unique
    ON instances(slug)
    WHERE slug IS NOT NULL;

-- Backfill: for existing rows, copy subdomain → slug so the columns align.
-- subdomain is constrained NOT NULL since V1, so this is safe.
UPDATE instances SET slug = subdomain WHERE slug IS NULL;

COMMENT ON COLUMN instances.slug IS
    'GAP-535 Wave 77 — normalized slug for URL/subdomain routing. Pipeline: NFC normalize → strip smart quotes (U+2018-U+201D) → Apache Commons stripAccents (flatten VN diacritics) → lowercase → non-alphanumeric → ''-'' → trim/collapse. Collision recovery service-side via -N suffix. Original organization_name preserved with diacritics for display.';
