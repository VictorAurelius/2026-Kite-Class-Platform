/**
 * CMS Slot system types and definitions.
 *
 * Slots allow per-instance content customization.
 * Each section has defined slots (text, image, list).
 * Content comes from backend API or AI branding.
 *
 * @since PR-THEME-3
 */

import type { SectionId } from './types';

export type SlotType = 'text' | 'richtext' | 'image' | 'list' | 'items';

export interface SlotDefinition {
  id: string;
  type: SlotType;
  label: string;
  required: boolean;
  maxLength?: number;
  maxItems?: number;
  placeholder?: string;
}

export interface SlotData {
  [slotId: string]: string | string[] | SlotItem[] | undefined;
}

export interface SlotItem {
  title: string;
  description?: string;
  icon?: string;
  image?: string;
  items?: string[];
}

/**
 * Slot definitions per section.
 * Defines what content each section accepts.
 */
export const SECTION_SLOTS: Record<SectionId, SlotDefinition[]> = {
  hero: [
    { id: 'title', type: 'text', label: 'Tiêu đề chính', required: true, maxLength: 100 },
    { id: 'subtitle', type: 'text', label: 'Mô tả ngắn', required: true, maxLength: 200 },
    { id: 'tagline', type: 'text', label: 'Slogan', required: false, maxLength: 50 },
    { id: 'image', type: 'image', label: 'Ảnh hero', required: false },
    { id: 'urgency', type: 'text', label: 'Thông điệp khẩn (khai giảng/ưu đãi)', required: false, maxLength: 120 },
    { id: 'ctaPrimaryLabel', type: 'text', label: 'Nút CTA chính (nhãn)', required: false, maxLength: 40 },
    { id: 'ctaPrimaryHref', type: 'text', label: 'Nút CTA chính (liên kết)', required: false, maxLength: 200 },
    { id: 'ctaSecondaryLabel', type: 'text', label: 'Nút CTA phụ (nhãn)', required: false, maxLength: 40 },
    { id: 'ctaSecondaryHref', type: 'text', label: 'Nút CTA phụ (liên kết)', required: false, maxLength: 200 },
  ],
  stats: [
    { id: 'stats', type: 'items', label: 'Chỉ số nổi bật', required: false, maxItems: 4 },
  ],
  problemSolution: [
    { id: 'items', type: 'items', label: 'Vấn đề & Giải pháp', required: false, maxItems: 6 },
  ],
  howItWorks: [
    { id: 'steps', type: 'items', label: 'Cách hoạt động (3 bước)', required: false, maxItems: 5 },
  ],
  trustStrip: [
    { id: 'signals', type: 'items', label: 'Tín hiệu tin cậy', required: false, maxItems: 6 },
  ],
  timeline: [
    { id: 'steps', type: 'items', label: 'Lộ trình học tập', required: false, maxItems: 6 },
  ],
  about: [
    { id: 'content', type: 'richtext', label: 'Nội dung giới thiệu', required: true },
    { id: 'mission', type: 'text', label: 'Sứ mệnh', required: false, maxLength: 200 },
    { id: 'vision', type: 'text', label: 'Tầm nhìn', required: false, maxLength: 200 },
  ],
  courses: [
    { id: 'features', type: 'items', label: 'Tính năng nổi bật', required: true, maxItems: 6 },
  ],
  teachers: [
    { id: 'teachers', type: 'items', label: 'Đội ngũ giáo viên', required: true, maxItems: 12 },
  ],
  certificates: [
    { id: 'certificates', type: 'items', label: 'Chứng chỉ', required: true, maxItems: 8 },
  ],
  gallery: [
    { id: 'images', type: 'list', label: 'Hình ảnh', required: true },
  ],
  news: [
    { id: 'articles', type: 'items', label: 'Bài viết', required: true, maxItems: 6 },
  ],
  enrollment: [
    { id: 'content', type: 'richtext', label: 'Thông tin tuyển sinh', required: true },
  ],
  pricing: [
    { id: 'plans', type: 'items', label: 'Gói dịch vụ', required: true, maxItems: 4 },
  ],
  testimonials: [
    { id: 'testimonials', type: 'items', label: 'Đánh giá', required: true, maxItems: 6 },
  ],
  faq: [
    { id: 'questions', type: 'items', label: 'Câu hỏi', required: true, maxItems: 10 },
  ],
  parents: [
    { id: 'content', type: 'richtext', label: 'Thông tin phụ huynh', required: true },
  ],
  contact: [
    { id: 'email', type: 'text', label: 'Email', required: false },
    { id: 'phone', type: 'text', label: 'Điện thoại', required: false },
    { id: 'address', type: 'text', label: 'Địa chỉ', required: false },
  ],
};

/**
 * Get slot definitions for a section.
 */
export function getSectionSlots(sectionId: SectionId): SlotDefinition[] {
  return SECTION_SLOTS[sectionId] || [];
}
