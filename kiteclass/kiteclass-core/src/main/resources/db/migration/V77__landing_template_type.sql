-- wave-thesis-4: per-tenant landing template type (personal | organization).
-- GV độc lập → 'personal' (7 section: Về tôi/khóa học/lộ trình...); trung tâm lớn → 'organization'.
-- Default 'organization' giữ backward-compat cho tenant cũ.
ALTER TABLE landing_pages ADD COLUMN IF NOT EXISTS template_type VARCHAR(20) NOT NULL DEFAULT 'organization';
