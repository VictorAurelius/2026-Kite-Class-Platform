/**
 * Settings page tests — Wave 30 Bucket D.
 *
 * Verifies tabs render, branding tab default, and G11 ThemePreview integration
 * on the new "Theme preview" tab.
 *
 * @since Wave 30 (GAP-266)
 */

import { describe, it, expect, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import userEvent from '@testing-library/user-event';
import { render, screen } from '@/test/utils';
import { server } from '@/mocks/server';
import SettingsPage from '../page';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/settings',
}));

// ResizeObserver / matchMedia stubs — required by Radix tabs + G11 dark-morph media query.
beforeAll(() => {
  (global as unknown as { ResizeObserver: unknown }).ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  if (!window.matchMedia) {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        addListener: vi.fn(),
        removeListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });
  }
});

beforeEach(() => {
  // Branding fetch — empty default
  server.use(
    http.get(`${BASE_URL}/api/v1/branding/me`, () =>
      HttpResponse.json({
        displayName: 'Trung tâm Test',
        tagline: '',
        primaryColor: '#0EA5E9',
        secondaryColor: '#F97316',
        accentColor: '#10B981',
        contactEmail: '',
        contactPhone: '',
        address: '',
        facebookUrl: '',
        zaloUrl: '',
        websiteUrl: '',
      }),
    ),
  );
});

describe('SettingsPage (Wave 30 — G11 ThemePreview integration)', () => {
  it('renders heading and three tabs (Branding, Theme preview, Tùy chọn)', () => {
    render(<SettingsPage />);

    expect(screen.getByRole('heading', { level: 1, name: 'Cài đặt' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Branding' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Theme preview' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Tùy chọn' })).toBeInTheDocument();
  });

  it('mounts G11 ThemePreview when "Theme preview" tab is activated', async () => {
    const user = userEvent.setup();
    render(<SettingsPage />);

    await user.click(screen.getByRole('tab', { name: 'Theme preview' }));

    expect(screen.getByTestId('settings-theme-preview')).toBeInTheDocument();
    // CTA link to AI Branding wizard surface
    expect(
      screen.getByRole('link', { name: /Áp dụng full theme/ }),
    ).toHaveAttribute('href', '/branding');
  });
});
