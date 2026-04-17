-- V13: Create branding_templates table for template gallery (SAAS-8)
CREATE TABLE IF NOT EXISTS branding_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    thumbnail_url VARCHAR(500),
    theme_config JSONB NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Seed 5 templates for instant branding
INSERT INTO branding_templates (name, category, theme_config) VALUES
('Modern Education', 'education', '{"colors":{"primary":"#3B82F6","secondary":"#1E40AF","accent":"#F59E0B"},"fonts":{"heading":"Inter","body":"Inter"},"style":"modern"}'),
('Classic Academy', 'education', '{"colors":{"primary":"#059669","secondary":"#047857","accent":"#D97706"},"fonts":{"heading":"Merriweather","body":"Open Sans"},"style":"classic"}'),
('Playful Learning', 'education', '{"colors":{"primary":"#8B5CF6","secondary":"#7C3AED","accent":"#EC4899"},"fonts":{"heading":"Poppins","body":"Poppins"},"style":"playful"}'),
('Professional Training', 'business', '{"colors":{"primary":"#1F2937","secondary":"#374151","accent":"#3B82F6"},"fonts":{"heading":"Roboto","body":"Roboto"},"style":"professional"}'),
('Minimal Clean', 'general', '{"colors":{"primary":"#0F172A","secondary":"#334155","accent":"#0EA5E9"},"fonts":{"heading":"DM Sans","body":"DM Sans"},"style":"minimal"}');
