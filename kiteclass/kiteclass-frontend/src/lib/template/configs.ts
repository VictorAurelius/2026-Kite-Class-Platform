/**
 * Pre-defined template configurations.
 *
 * Personal: 7 sections (for individual tutors / small centers)
 * Organization: 13 sections (for large centers with full features)
 *
 * @since PR-THEME-2
 */

import type { TemplateConfig } from './types';

// Personal template voice (GAP-1208): headings/sub-headings speak as ONE independent
// teacher to parents/students — NOT a center's organizational voice. The `heading`/
// `subheading` overrides below flow into each section's <h2>; `label` (admin section
// name) is kept in sync for the editor. Sections without an override keep the
// component default (which reads correctly for a solo teacher already).
export const PERSONAL_TEMPLATE: TemplateConfig = {
  type: 'personal',
  name: 'Cá nhân',
  description: 'Dành cho gia sư, giáo viên tự do, trung tâm nhỏ',
  sections: [
    { id: 'hero', label: 'Giới thiệu chính', enabled: true, order: 0 },
    { id: 'stats', label: 'Chỉ số nổi bật', enabled: true, order: 1 },
    { id: 'problemSolution', label: 'Vấn đề & Giải pháp', enabled: true, order: 2 },
    { id: 'about', label: 'Về giáo viên', enabled: true, order: 3, heading: 'Về giáo viên' },
    // 'teachers' (TeachersSection) enabled cho GV độc lập (GAP-1194): hiển thị thẻ giáo viên
    // khi tenant có data teachers; tự ẩn khi rỗng (anti-fabrication GAP-958). Trước đây section
    // này chỉ có ở organization template → teachers JSONB của tenant personal không render.
    // GAP-1208: heading "Giáo viên đồng hành" (giọng cá nhân) thay "Đội ngũ giáo viên" (giọng trung tâm).
    {
      id: 'teachers',
      label: 'Giáo viên đồng hành',
      enabled: true,
      order: 4,
      heading: 'Giáo viên đồng hành',
      subheading: 'Người trực tiếp giảng dạy và theo sát từng học viên',
    },
    { id: 'howItWorks', label: 'Cách hoạt động', enabled: true, order: 5 },
    // 'courses' (FeaturesSection generic platform-feature: LMS/thanh toán) ẩn cho GV độc lập —
    // chương trình/khóa học thật hiển thị qua section 'certificates' (programs data). wave-thesis-4.
    { id: 'courses', label: 'Khóa học', enabled: false, order: 6 },
    { id: 'timeline', label: 'Lộ trình học tập', enabled: true, order: 7 },
    { id: 'certificates', label: 'Chương trình giảng dạy', enabled: true, order: 8 },
    { id: 'trustStrip', label: 'Tin cậy & minh bạch', enabled: true, order: 9 },
    // GAP-1208: "Học phí" (giọng cá nhân) thay "Bảng giá" (giọng trung tâm).
    {
      id: 'pricing',
      label: 'Học phí',
      enabled: true,
      order: 10,
      heading: 'Học phí',
      subheading: 'Học phí rõ ràng — chọn hình thức học phù hợp với mục tiêu của con',
    },
    {
      id: 'testimonials',
      label: 'Phụ huynh & học viên nói gì',
      enabled: true,
      order: 11,
      heading: 'Phụ huynh & học viên nói gì',
    },
    { id: 'faq', label: 'Câu hỏi thường gặp', enabled: true, order: 12 },
    { id: 'contact', label: 'Liên hệ', enabled: true, order: 13 },
  ],
};

export const ORGANIZATION_TEMPLATE: TemplateConfig = {
  type: 'organization',
  name: 'Tổ chức',
  description: 'Dành cho trung tâm giáo dục, trường học, doanh nghiệp đào tạo',
  sections: [
    { id: 'hero', label: 'Giới thiệu chính', enabled: true, order: 0 },
    { id: 'problemSolution', label: 'Vấn đề & Giải pháp', enabled: true, order: 1 },
    { id: 'about', label: 'Giới thiệu', enabled: true, order: 2 },
    { id: 'courses', label: 'Khóa học', enabled: true, order: 3 },
    { id: 'howItWorks', label: 'Cách hoạt động', enabled: true, order: 4 },
    { id: 'teachers', label: 'Đội ngũ giáo viên', enabled: true, order: 5 },
    { id: 'certificates', label: 'Chứng chỉ', enabled: true, order: 6 },
    { id: 'gallery', label: 'Thư viện ảnh', enabled: false, order: 7 },
    { id: 'news', label: 'Tin tức', enabled: false, order: 8 },
    { id: 'enrollment', label: 'Tuyển sinh', enabled: true, order: 9 },
    { id: 'trustStrip', label: 'Tin cậy & minh bạch', enabled: true, order: 10 },
    { id: 'pricing', label: 'Bảng giá', enabled: true, order: 11 },
    { id: 'testimonials', label: 'Đánh giá', enabled: true, order: 12 },
    { id: 'faq', label: 'Câu hỏi thường gặp', enabled: true, order: 13 },
    { id: 'parents', label: 'Phụ huynh', enabled: false, order: 14 },
    { id: 'contact', label: 'Liên hệ', enabled: true, order: 15 },
  ],
};

export const TEMPLATES: Record<string, TemplateConfig> = {
  personal: PERSONAL_TEMPLATE,
  organization: ORGANIZATION_TEMPLATE,
};

/**
 * Get template by type, defaults to organization.
 */
export function getTemplate(type?: string): TemplateConfig {
  return TEMPLATES[type ?? 'organization'] ?? ORGANIZATION_TEMPLATE;
}
