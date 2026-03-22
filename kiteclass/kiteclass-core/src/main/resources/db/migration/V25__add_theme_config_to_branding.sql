-- Add theme_config_json column to branding table
-- This stores the complete AI-generated theme configuration JSON from KiteHub branding service
-- Format: { colors: {...}, typography: {...}, spacing: {...}, layout: {...} }

ALTER TABLE branding
ADD COLUMN IF NOT EXISTS theme_config_json TEXT;

COMMENT ON COLUMN branding.theme_config_json IS 'Complete theme configuration JSON from AI branding (colors, typography, spacing, layout)';
