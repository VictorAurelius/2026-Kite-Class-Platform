/**
 * Dynamic template renderer.
 * Renders enabled sections in configured order, passing slot data to each.
 *
 * @since PR-THEME-2, updated PR-THEME-3
 */

import type { TemplateConfig, SectionId } from '@/lib/template/types';
import type { SlotData } from '@/lib/template/slots';
import { getEnabledSections } from '@/lib/template/types';
import { HeroSection } from './HeroSection';
import { AboutSection } from './AboutSection';
import { FeaturesSection } from './FeaturesSection';
import { TestimonialsSection } from './TestimonialsSection';
import { CTASection } from './CTASection';
import { ContactSection } from './ContactSection';
import { PlaceholderSection } from './PlaceholderSection';

interface LandingData {
  heroTitle?: string;
  heroSubtitle?: string;
  tagline?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  [key: string]: unknown;
}

/**
 * Per-section slot data from backend or CMS.
 */
export type SectionSlotMap = Partial<Record<SectionId, SlotData>>;

interface TemplateRendererProps {
  template: TemplateConfig;
  data: LandingData;
  slots?: SectionSlotMap;
}

const SECTION_LABELS: Record<SectionId, string> = {
  hero: 'Giới thiệu chính',
  about: 'Giới thiệu',
  courses: 'Khóa học',
  teachers: 'Đội ngũ giáo viên',
  certificates: 'Chứng chỉ',
  gallery: 'Thư viện ảnh',
  news: 'Tin tức',
  enrollment: 'Tuyển sinh',
  pricing: 'Bảng giá',
  testimonials: 'Đánh giá',
  faq: 'Câu hỏi thường gặp',
  parents: 'Dành cho phụ huynh',
  contact: 'Liên hệ',
};

function renderSection(sectionId: SectionId, data: LandingData, sectionSlots?: SlotData) {
  switch (sectionId) {
    case 'hero':
      return <HeroSection slots={sectionSlots} title={data.heroTitle as string} subtitle={data.heroSubtitle as string} tagline={data.tagline as string} />;
    case 'about':
      return <AboutSection slots={sectionSlots} />;
    case 'courses':
      return <FeaturesSection slots={sectionSlots} />;
    case 'testimonials':
      return <TestimonialsSection slots={sectionSlots} />;
    case 'contact':
      return <ContactSection slots={sectionSlots} email={data.contactEmail} phone={data.contactPhone} address={data.address} />;
    case 'pricing':
      return <PlaceholderSection title={SECTION_LABELS.pricing} description="Thông tin bảng giá sẽ được cập nhật sớm." />;
    case 'teachers':
      return <PlaceholderSection title={SECTION_LABELS.teachers} description="Đội ngũ giáo viên giàu kinh nghiệm." />;
    case 'certificates':
      return <PlaceholderSection title={SECTION_LABELS.certificates} description="Chứng chỉ được công nhận." />;
    case 'gallery':
      return <PlaceholderSection title={SECTION_LABELS.gallery} description="Hình ảnh hoạt động." />;
    case 'news':
      return <PlaceholderSection title={SECTION_LABELS.news} description="Tin tức và sự kiện." />;
    case 'enrollment':
      return <PlaceholderSection title={SECTION_LABELS.enrollment} description="Thông tin tuyển sinh." />;
    case 'faq':
      return <PlaceholderSection title={SECTION_LABELS.faq} description="Câu hỏi thường gặp." />;
    case 'parents':
      return <PlaceholderSection title={SECTION_LABELS.parents} description="Thông tin dành cho phụ huynh." />;
    default:
      return null;
  }
}

export function TemplateRenderer({ template, data, slots = {} }: TemplateRendererProps) {
  const sections = getEnabledSections(template);

  return (
    <div className="flex flex-col">
      {sections.map((section) => (
        <div key={section.id}>
          {renderSection(section.id, data, slots[section.id])}
          {section.id === 'courses' && <CTASection />}
        </div>
      ))}
    </div>
  );
}
