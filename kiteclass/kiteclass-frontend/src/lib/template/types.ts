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
  /** Admin-panel section name (section list / toggles) — NOT the rendered title. */
  label: string;
  enabled: boolean;
  order: number;
  /**
   * Rendered section title override. When set, flows into the section component's
   * `heading` prop so the on-page <h2> matches the template voice (e.g. personal
   * vs organization). When omitted, the component keeps its own default heading.
   * (GAP-1208: personal template read as a center because `label` never flowed
   * into the rendered heading.)
   */
  heading?: string;
  /** Rendered sub-heading / lead paragraph override; falls back to component default. */
  subheading?: string;
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
