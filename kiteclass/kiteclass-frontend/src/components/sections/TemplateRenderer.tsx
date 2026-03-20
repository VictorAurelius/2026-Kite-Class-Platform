/**
 * Dynamic template renderer.
 * Renders enabled sections in configured order based on TemplateConfig.
 *
 * @since PR-THEME-2
 */

import type { TemplateConfig, SectionId } from '@/lib/template/types';
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

interface TemplateRendererProps {
  template: TemplateConfig;
  data: LandingData;
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

function renderSection(sectionId: SectionId, data: LandingData) {
  switch (sectionId) {
    case 'hero':
      return <HeroSection title={data.heroTitle as string} subtitle={data.heroSubtitle as string} tagline={data.tagline as string} />;
    case 'about':
      return <AboutSection />;
    case 'courses':
      return <FeaturesSection />;
    case 'testimonials':
      return <TestimonialsSection />;
    case 'contact':
      return <ContactSection email={data.contactEmail} phone={data.contactPhone} address={data.address} />;
    // Sections with CTA inserted after courses
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

export function TemplateRenderer({ template, data }: TemplateRendererProps) {
  const sections = getEnabledSections(template);

  return (
    <div className="flex flex-col">
      {sections.map((section) => (
        <div key={section.id}>
          {renderSection(section.id, data)}
          {/* Insert CTA after courses section */}
          {section.id === 'courses' && <CTASection />}
        </div>
      ))}
    </div>
  );
}
