/**
 * Template system types.
 * Defines template configs, section configs, and rendering types.
 *
 * @since PR-THEME-2
 */

export type TemplateType = 'personal' | 'organization';

export type SectionId =
  | 'hero'
  | 'stats'
  | 'problemSolution'
  | 'about'
  | 'howItWorks'
  | 'courses'
  | 'teachers'
  | 'certificates'
  | 'gallery'
  | 'news'
  | 'timeline'
  | 'enrollment'
  | 'trustStrip'
  | 'pricing'
  | 'testimonials'
  | 'faq'
  | 'parents'
  | 'contact';

export interface SectionConfig {
  id: SectionId;
  label: string;
  enabled: boolean;
  order: number;
}

export interface TemplateConfig {
  type: TemplateType;
  name: string;
  description: string;
  sections: SectionConfig[];
}

export interface SectionProps {
  data: Record<string, unknown>;
}

/**
 * Get enabled sections sorted by order.
 */
export function getEnabledSections(config: TemplateConfig): SectionConfig[] {
  return config.sections
    .filter(s => s.enabled)
    .sort((a, b) => a.order - b.order);
}
