-- V15: Alter branding_templates.theme_config from JSONB to TEXT
-- Reason: BrandingTemplate entity maps themeConfig as String (columnDefinition="text"),
-- but V13 created the column as JSONB. Hibernate validate mode fails on type mismatch.
-- JSONB casts to TEXT automatically (no data loss).

ALTER TABLE branding_templates
    ALTER COLUMN theme_config TYPE TEXT USING theme_config::text;
