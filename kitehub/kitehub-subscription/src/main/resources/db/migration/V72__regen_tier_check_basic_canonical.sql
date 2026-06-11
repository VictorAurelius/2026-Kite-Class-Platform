-- GAP-1228: tier-name unify PRO → BASIC (canonical per PricingTier.java).
--
-- Root cause: V59 CHECK chk_branding_regen_tier chỉ nhận ('FREE','PRO','PREMIUM','ENTERPRISE')
-- trong khi JWT tier claim phát Enum.name() của PricingTier = 'BASIC' → insert snapshot
-- tier='BASIC' vi phạm CHECK. RegenerateQuotaService.canonicalTier() từ nay map alias
-- cũ 'PRO' → 'BASIC' trước khi persist, nên:
--   1. Migrate rows cũ 'PRO' → 'BASIC' (value-preserving — cùng cap 10/ngày).
--   2. CHECK mới = canonical 4 tier, không còn 'PRO'.

UPDATE branding_regenerate_usage SET tier = 'BASIC' WHERE tier = 'PRO';

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND constraint_name = 'chk_branding_regen_tier'
    ) THEN
        ALTER TABLE branding_regenerate_usage
            DROP CONSTRAINT chk_branding_regen_tier;
    END IF;

    ALTER TABLE branding_regenerate_usage
        ADD CONSTRAINT chk_branding_regen_tier
        CHECK (tier IN ('FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'));
END $$;
