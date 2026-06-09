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
  useBrandingDeployStatus: vi.fn(),
}));

vi.mock('@/hooks/use-branding-tier', () => ({
  useBrandingTier: vi.fn(),
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
import { useAssets, useBrandingDeployStatus } from '@/hooks/use-branding';
import { useBrandingTier } from '@/hooks/use-branding-tier';

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

    // GAP-1091a: real tier hook — mock so the page renders past LoadingSpinner.
    (useBrandingTier as ReturnType<typeof vi.fn>).mockReturnValue({
      tier: 'FREE',
      regenerateQuota: 3,
      isLoading: false,
    });

    // GAP-1108: default = not deployed → deploy-success card hidden.
    (useBrandingDeployStatus as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
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

  // ---- GAP-1108: post-deploy success card -------------------------------

  it('hides the deploy-success card when instance is not deployed', () => {
    render(<BrandingDashboardPage />);
    expect(screen.queryByText(/Trang web của bạn đã sẵn sàng/i)).not.toBeInTheDocument();
  });

  it('shows the deploy-success card + landing link when instance is DEPLOYED', () => {
    (useBrandingDeployStatus as ReturnType<typeof vi.fn>).mockReturnValue({
      data: {
        instanceId: mockInstances[0]!.id,
        state: 'DEPLOYED',
        deployed: true,
        frontendUrl: 'https://toan-master.kiteclass.vn',
        templateId: 'sky-wave',
        slug: 'toan-master',
        brandingVersion: 1,
        deployedAt: '2026-06-09T09:57:59',
      },
      isLoading: false,
      error: null,
    });

    render(<BrandingDashboardPage />);

    expect(screen.getByText(/Trang web của bạn đã sẵn sàng/i)).toBeInTheDocument();
    const landingLink = screen.getByRole('link', { name: /Xem.*landing|landing.*triển khai/i });
    expect(landingLink.getAttribute('href')).toBe('https://toan-master.kiteclass.vn');
    expect(landingLink.getAttribute('target')).toBe('_blank');
  });
});
