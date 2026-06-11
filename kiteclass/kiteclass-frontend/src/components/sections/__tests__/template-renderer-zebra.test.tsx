/**
 * TemplateRenderer zebra-rhythm tests (GAP-1226).
 *
 * Root cause fixed: the zebra band index used to advance for EVERY non-hero section,
 * including hide-when-empty sections that render nothing (no slot data). A tenant
 * missing data (e.g. no testimonials/faq/stats) therefore left phantom bands → two
 * adjacent VISIBLE sections could share the same background, drifting from the kit.
 *
 * Fix: the band only advances for sections that actually render content
 * (`sectionHasContent`), so the visible sections keep strict zebra alternation.
 */

import { describe, it, expect } from 'vitest';
import { render } from '@/test/utils';
import { TemplateRenderer, sectionHasContent, type SectionSlotMap } from '../TemplateRenderer';
import { PERSONAL_TEMPLATE } from '@/lib/template/configs';
import type { SlotItem } from '@/lib/template/slots';

const teachers: SlotItem[] = [
  { title: 'Cô Hà', description: 'Giáo viên Toán', items: ['6 năm kinh nghiệm'] },
];
const plans: SlotItem[] = [
  { title: 'Lớp Toán cô Hà', description: 'Miễn phí / học thử', items: ['Lớp nhỏ 6 học viên'] },
];

/**
 * Collect the visible (content-bearing) non-hero section wrappers in render order,
 * then map each to whether it carries the striped `bg-muted/40` band class.
 */
function visibleBandFlags(container: HTMLElement): boolean[] {
  return Array.from(container.querySelectorAll('[data-section]'))
    .filter(
      (el) =>
        (el as HTMLElement).dataset.section !== 'hero' &&
        ((el as HTMLElement).textContent ?? '').trim().length > 0,
    )
    .map((el) => el.classList.contains('bg-muted/40'));
}

describe('sectionHasContent (GAP-1226)', () => {
  const empty: SectionSlotMap = {};

  it('returns false for hide-when-empty sections with no slot data', () => {
    expect(sectionHasContent('testimonials', empty)).toBe(false);
    expect(sectionHasContent('faq', empty)).toBe(false);
    expect(sectionHasContent('stats', empty)).toBe(false);
    expect(sectionHasContent('teachers', empty)).toBe(false);
    expect(sectionHasContent('certificates', empty)).toBe(false);
  });

  it('returns true for hide-when-empty sections once their slot is present', () => {
    const slots: SectionSlotMap = { teachers: { teachers }, pricing: { plans } };
    expect(sectionHasContent('teachers', slots)).toBe(true);
    expect(sectionHasContent('pricing', slots)).toBe(true);
  });

  it('returns true for fallback sections regardless of slot data', () => {
    expect(sectionHasContent('hero', empty)).toBe(true);
    expect(sectionHasContent('about', empty)).toBe(true);
    expect(sectionHasContent('timeline', empty)).toBe(true);
    expect(sectionHasContent('contact', empty)).toBe(true);
  });
});

describe('TemplateRenderer — zebra skips empty sections (GAP-1226)', () => {
  it('keeps strict zebra alternation across visible sections when a tenant lacks data', () => {
    // Tenant has teacher + pricing only; testimonials/faq/stats/etc. collapse (empty).
    const { container } = render(
      <TemplateRenderer
        template={PERSONAL_TEMPLATE}
        data={{ heroTitle: 'Lớp Toán cô Hà' }}
        slots={{ teachers: { teachers }, pricing: { plans } }}
      />,
    );

    const flags = visibleBandFlags(container as HTMLElement);

    // Sanity: more than two visible content sections (about/teachers/timeline/pricing/contact).
    expect(flags.length).toBeGreaterThan(2);
    // Strict alternation: no two adjacent visible sections share the band state.
    for (let i = 1; i < flags.length; i++) {
      expect(flags[i]).not.toBe(flags[i - 1]);
    }
  });
});
