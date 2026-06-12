-- GAP-1213 — Cross-service AI-branding deploy propagation: track the last applied
-- branding deploy version per tenant landing page so the branding.deployed consumer
-- (BrandingDeployedEventConsumer) is idempotent against duplicate / stale broker
-- redeliveries (apply only when the incoming version is newer). NOT NULL DEFAULT 0
-- so existing landings are treated as "no AI deploy applied yet" and the first real
-- branding.deployed (version >= 1) always lands.
ALTER TABLE landing_pages ADD COLUMN IF NOT EXISTS branding_version INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN landing_pages.branding_version IS
    'GAP-1213: last applied AI-branding deploy version from the cross-service branding.deployed event; drives consumer idempotency (apply only when incoming version > this).';
