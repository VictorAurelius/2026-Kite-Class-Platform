/**
 * BrandingThemeApplier Component Tests
 *
 * Verifies the dashboard applies the authenticated tenant's persisted branding
 * colours to the document root CSS variables (GAP-807) — not the globals.css
 * default blue.
 *
 * @since GAP-807
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render } from '@testing-library/react';
import { BrandingThemeApplier } from '../BrandingThemeApplier';

// Mock the authenticated branding hook — return an orange brand palette.
const mockUseBranding = vi.fn();
vi.mock('@/hooks/use-branding', () => ({
  useBranding: () => mockUseBranding(),
}));

const ORANGE = '#F97316'; // tailwind orange-500
const SECONDARY = '#8B5CF6';
const ACCENT = '#10B981';

// Globals.css default primary (Shadcn HSL) — what we must NOT see after apply.
const DEFAULT_PRIMARY_HSL = '221.2 83.2% 53.3%';

describe('BrandingThemeApplier', () => {
  beforeEach(() => {
    // Reset root inline styles between tests.
    document.documentElement.style.cssText = '';
    mockUseBranding.mockReset();
  });

  afterEach(() => {
    document.documentElement.style.cssText = '';
  });

  it('applies persisted brand colours to document root CSS vars on mount', () => {
    mockUseBranding.mockReturnValue({
      data: {
        id: 1,
        instanceId: 'tenant-1',
        displayName: 'Trung tâm Anh ngữ Sky Education',
        primaryColor: ORANGE,
        secondaryColor: SECONDARY,
        accentColor: ACCENT,
        createdAt: '2026-05-29T00:00:00Z',
        updatedAt: '2026-05-29T00:00:00Z',
      },
    });

    render(<BrandingThemeApplier />);

    const root = document.documentElement;

    // Raw-hex brand vars set verbatim.
    expect(root.style.getPropertyValue('--brand-primary')).toBe(ORANGE);
    expect(root.style.getPropertyValue('--brand-secondary')).toBe(SECONDARY);
    expect(root.style.getPropertyValue('--brand-accent')).toBe(ACCENT);

    // Shadcn HSL channel recoloured — orange, NOT the default blue.
    const primaryHsl = root.style.getPropertyValue('--primary');
    expect(primaryHsl).not.toBe('');
    expect(primaryHsl).not.toBe(DEFAULT_PRIMARY_HSL);
    // #F97316 → hue ~25 (orange range), clearly not 221 (blue).
    const hue = Number(primaryHsl.trim().split(' ')[0]);
    expect(hue).toBeGreaterThan(15);
    expect(hue).toBeLessThan(45);
  });

  it('is a no-op when branding has not loaded yet (no data)', () => {
    mockUseBranding.mockReturnValue({ data: undefined });

    render(<BrandingThemeApplier />);

    // No brand vars injected — dashboard keeps globals.css default until fetch resolves.
    expect(document.documentElement.style.getPropertyValue('--primary')).toBe('');
    expect(document.documentElement.style.getPropertyValue('--brand-primary')).toBe('');
  });
});
