-- V19__seed_default_landing_page.sql
-- Seed default landing page for Demo School tenant
-- This provides initial content that will be replaced by AI Branding (KiteHub) later
-- @since 3.4

-- =====================================================
-- Default Landing Page for Demo School
-- =====================================================
INSERT INTO landing_pages (
    instance_id,
    hero_title,
    hero_subtitle,
    hero_image_url,
    teacher_bio,
    logo_url,
    tagline,
    primary_color,
    secondary_color,
    contact_email,
    contact_phone,
    address,
    facebook_url,
    youtube_url,
    instagram_url,
    created_at,
    updated_at,
    created_by,
    deleted
)
VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid, -- Demo School instance
    'Quản lý Trung tâm Tiếng Anh Chuyên nghiệp & Hiệu quả', -- hero_title
    'Nền tảng quản lý toàn diện giúp tối ưu hóa vận hành trung tâm tiếng Anh với LMS, quản lý học viên, điểm danh tự động và thanh toán online.', -- hero_subtitle
    NULL, -- hero_image_url (will be set by KiteHub AI Branding)
    'Đội ngũ giảng viên giàu kinh nghiệm, tận tâm với phương pháp giảng dạy hiện đại. Chúng tôi cam kết mang đến chất lượng giáo dục tốt nhất cho học viên.', -- teacher_bio
    NULL, -- logo_url (will be set by KiteHub)
    'Nâng tầm giáo dục, tối ưu quản lý', -- tagline
    '#3B82F6', -- primary_color (blue)
    '#8B5CF6', -- secondary_color (purple)
    'support@kiteclass.com', -- contact_email
    '1900 xxxx', -- contact_phone
    'Hà Nội, Việt Nam', -- address
    NULL, -- facebook_url
    NULL, -- youtube_url
    NULL, -- instagram_url
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'SYSTEM',
    false
);

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE landing_pages IS 'This seed data provides default landing page content for Demo School. AI Branding (KiteHub) will generate personalized content based on teacher inputs.';
