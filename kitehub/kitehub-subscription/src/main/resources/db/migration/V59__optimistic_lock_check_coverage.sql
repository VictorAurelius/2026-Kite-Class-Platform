-- V59: Wave 14 Bucket C-KH — optimistic-lock version coverage + CHECK constraint coverage.
--
-- GAP-895: subscription-cluster tables (subscriptions, payments) lack @Version optimistic
--   lock. Race risk: auto-renew cron vs admin manual extend overwrite without lock
--   (especially pending_payment_id during admin upgrade vs cron expire). Add
--   `version BIGINT NOT NULL DEFAULT 0`; entity gets `@Version Long version` (field-level
--   on Subscription + Payment, NOT BaseEntity — instances table is out of this bucket's
--   scope (D-KH), so adding @Version to BaseEntity would require an instances column too →
--   schema-drift FAIL. Minimal-change: per-entity field matches the 2 columns added here).
--
-- GAP-899: CHECK constraint coverage inconsistent across branding cluster. State-check
--   (V4/V16/V29/V30) confirms missing:
--     * backup_records.status        — no CHECK (V16)
--     * branding_regenerate_usage.tier — no CHECK (V29)
--     * branding_regenerate_usage window order (window_end > window_start) — no semantic CHECK
--   branding_jobs/branding_instance_state/branding_lifecycle_events already have CHECKs (V4/V30).
--
-- Forward-only + idempotent (IF NOT EXISTS guards on columns; DO-block guards on constraints).
-- No money-field changes (Long amount_vnd / price_vnd untouched — that is Bucket D-KH).

-- ===== GAP-895: optimistic-lock version column =====

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN subscriptions.version IS
    'JPA @Version optimistic lock (GAP-895, Wave 14 C-KH). Guards auto-renew cron vs admin '
    'manual extend race on pending_payment_id / status.';

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN payments.version IS
    'JPA @Version optimistic lock (GAP-895, Wave 14 C-KH). Guards concurrent payment status '
    'transitions (PENDING -> COMPLETED vs admin REFUNDED).';

-- ===== GAP-899: missing CHECK constraints (branding cluster) =====

-- backup_records.status: values per BackupRecord lifecycle (V16 default 'IN_PROGRESS').
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND constraint_name = 'chk_backup_records_status'
    ) THEN
        ALTER TABLE backup_records
            ADD CONSTRAINT chk_backup_records_status
            CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'RESTORED'));
    END IF;
END $$;

-- branding_regenerate_usage.tier: tier caps live in code (FREE/PRO/PREMIUM/ENTERPRISE per V29
-- comment). CHECK keeps DB enum-trace consistent with subscription tier vocabulary.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND constraint_name = 'chk_branding_regen_tier'
    ) THEN
        ALTER TABLE branding_regenerate_usage
            ADD CONSTRAINT chk_branding_regen_tier
            CHECK (tier IN ('FREE', 'PRO', 'PREMIUM', 'ENTERPRISE'));
    END IF;
END $$;

-- branding_regenerate_usage window order: window_end must be strictly after window_start
-- (daily UTC window per V29). Semantic CHECK prevents inverted/zero-length windows.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND constraint_name = 'chk_branding_regen_window_order'
    ) THEN
        ALTER TABLE branding_regenerate_usage
            ADD CONSTRAINT chk_branding_regen_window_order
            CHECK (window_end > window_start);
    END IF;
END $$;
