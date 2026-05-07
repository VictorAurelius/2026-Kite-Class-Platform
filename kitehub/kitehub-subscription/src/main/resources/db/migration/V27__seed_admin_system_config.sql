-- GAP-376 (Wave 33 Bucket A): Production data seed scaffolding.
--
-- Creates the `system_config` flat key/value store consumed by
-- `ProductionSeedRunner` + future infrastructure callers, and idempotently
-- seeds platform-level system_config defaults so the runner can be skipped
-- on environments that prefer SQL-only bootstrap.
--
-- The platform admin user is NOT inserted here — that is the runner's job
-- (it BCrypts a password sourced from the secrets manager, per GAP-379, and
-- never persists plaintext to migration scripts).
--
-- Idempotency: every INSERT uses ON CONFLICT DO NOTHING so re-running this
-- migration on a database that already contains the rows is a no-op. The
-- runner relies on the same uniqueness contract via SystemConfigSeedDao.
--
-- See: documents/04-quality/gaps/GAP-376-production-data-seed.md
--      documents/03-planning/waves/wave-2026-05-06-33-beta-deploy-cluster.md §3 Bucket A

CREATE TABLE IF NOT EXISTS system_config (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    description  VARCHAR(500),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_system_config_key ON system_config(config_key);

-- Default platform system config (FREE tier, VND, vi locale).
-- ProductionSeedRunner will upsert the same keys when it runs; this migration
-- exists so SQL-only environments (e.g. read-replica bootstrap) still get the
-- baseline rows without invoking the Spring runner.
INSERT INTO system_config (config_key, config_value, description) VALUES
    ('default_tier', 'FREE', 'Default pricing tier for new instances'),
    ('currency', 'VND', 'Platform default currency code (ISO-4217)'),
    ('locale', 'vi', 'Platform default locale (BCP-47)'),
    ('platform_tenant_id', '0', 'Reserved tenant id for platform-level resources')
ON CONFLICT (config_key) DO NOTHING;
