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
import { TeachersSection } from './TeachersSection';
import { CertificatesSection } from './CertificatesSection';
import { EnrollmentSection } from './EnrollmentSection';
import { PricingSection } from './PricingSection';
import { StatsSection } from './StatsSection';
import { TimelineSection } from './TimelineSection';
import { FaqSection } from './FaqSection';

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
  stats: 'Chỉ số nổi bật',
  about: 'Giới thiệu',
  courses: 'Khóa học',
  teachers: 'Đội ngũ giáo viên',
  certificates: 'Chứng chỉ',
  gallery: 'Thư viện ảnh',
  news: 'Tin tức',
  timeline: 'Lộ trình học tập',
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
    case 'stats':
      return <StatsSection slots={sectionSlots} />;
    case 'timeline':
      return <TimelineSection slots={sectionSlots} />;
    case 'about':
      return <AboutSection slots={sectionSlots} />;
    case 'courses':
      return <FeaturesSection slots={sectionSlots} />;
    case 'testimonials':
      return <TestimonialsSection slots={sectionSlots} />;
    case 'contact':
      return <ContactSection slots={sectionSlots} email={data.contactEmail} phone={data.contactPhone} address={data.address} />;
    case 'pricing':
      return <PricingSection slots={sectionSlots} />;
    case 'teachers':
      return <TeachersSection slots={sectionSlots} />;
    case 'certificates':
      return <CertificatesSection slots={sectionSlots} />;
    case 'gallery':
      return <PlaceholderSection title={SECTION_LABELS.gallery} description="Hình ảnh hoạt động." />;
    case 'news':
      return <PlaceholderSection title={SECTION_LABELS.news} description="Tin tức và sự kiện." />;
    case 'enrollment':
      return <EnrollmentSection slots={sectionSlots} />;
    case 'faq':
      return <FaqSection slots={sectionSlots} />;
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
