-- Add theme_config_json column to branding table (GAP-065: made fresh-deploy safe)
-- This stores the complete AI-generated theme configuration JSON from KiteHub branding service
-- Format: { colors: {...}, typography: {...}, spacing: {...}, layout: {...} }
--
-- Note: branding table may not exist on fresh deploy (it was originally provisioned
-- by kitehub-branding service). V40 creates it IF NOT EXISTS, but since V25 runs
-- before V40, we guard with an existence check here.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'branding') THEN
        ALTER TABLE branding ADD COLUMN IF NOT EXISTS theme_config_json TEXT;
        COMMENT ON COLUMN branding.theme_config_json IS
            'Complete theme configuration JSON from AI branding (colors, typography, spacing, layout)';
    END IF;
END
$$;
