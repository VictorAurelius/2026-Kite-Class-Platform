/**
 * Branding Hub Page Tests — Wave 31 Bucket C
 *
 * Tests for the production branding hub page ported from `kitehub-pro-v2`
 * kit (`branding-hub-{default,dark,loading,quota-empty}.html`). Integrates
 * G11 ThemePreview from `@kite/shared-ui` (Wave 29 Bucket C output).
 *
 * Wizard internals (6-step Direction C, Enterprise Advanced, Quality Gate)
 * are deferred to Wave 32 — only the wizard CTA placeholder is tested here.
 *
 * @since Wave 31 Bucket C
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import { mockUser, mockInstances } from '@/__tests__/mocks/data';
import BrandingDashboardPage from '../page';

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/hooks/use-instances', () => ({
  useOwnerInstances: vi.fn(),
}));

vi.mock('@/hooks/use-branding', () => ({
  useAssets: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  useSearchParams: () => ({
    get: () => null,
  }),
  useRouter: () => ({
    push: vi.fn(),
  }),
}));

// Mock G11 ThemePreview — integration smoke test ensures the prop contract
// is honoured; the component itself is unit-tested in `@kite/shared-ui`.
vi.mock('@kite/shared-ui', () => ({
  ThemePreview: ({ brandColors }: { brandColors: { primary: string } }) => (
    <div data-testid="g11-theme-preview" data-primary={brandColors.primary}>
      ThemePreview mock
    </div>
  ),
}));

vi.mock('next/dynamic', () => ({
  default: () => {
    const Mock = () => <div data-testid="recent-assets-grid">RecentAssetsGrid</div>;
    return Mock;
  },
}));

import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useAssets } from '@/hooks/use-branding';

describe('BrandingDashboardPage (Wave 31 Bucket C)', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    (useAuthStore as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      user: mockUser,
    });

    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    });

    (useAssets as ReturnType<typeof vi.fn>).mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
    });
  });

  it('renders branding hub default state with header + quota counter', () => {
    render(<BrandingDashboardPage />);

    // Hero header
    expect(screen.getByRole('heading', { level: 1, name: /Thương hiệu AI/i })).toBeInTheDocument();
    // Quota widget — kit-pro-v2 spec: visible regenerate counter (per ai-branding-guidelines.md §4.3)
    expect(screen.getByText(/Quota làm lại brand/i)).toBeInTheDocument();
  });

  it('integrates G11 ThemePreview from @kite/shared-ui with brand colors', () => {
    render(<BrandingDashboardPage />);

    const preview = screen.getByTestId('g11-theme-preview');
    expect(preview).toBeInTheDocument();
    // Prop contract: brandColors.primary must be a hex value string
    expect(preview.getAttribute('data-primary')).toMatch(/^#[0-9a-f]{6}$/i);
  });

  it('exposes wizard CTA as placeholder link to /branding/wizard (Wave 32)', () => {
    render(<BrandingDashboardPage />);

    // Per Wave 31 Bucket C constraint: wizard route NOT created here, only CTA.
    const ctas = screen.getAllByRole('link', { name: /Bắt đầu|Tạo lại brand|Tạo Branding/i });
    expect(ctas.length).toBeGreaterThan(0);
    // At least one CTA must point at /branding/wizard placeholder
    const wizardCta = ctas.find((el) => el.getAttribute('href') === '/branding/wizard');
    expect(wizardCta).toBeDefined();
  });
});
