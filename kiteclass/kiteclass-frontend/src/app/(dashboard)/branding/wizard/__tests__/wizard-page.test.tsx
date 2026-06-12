/**
 * Branding wizard route tests — GAP-1214 (UNIFY).
 *
 * The AI Branding generation wizard is a KiteHub platform capability (KH-6).
 * This KiteClass route now REDIRECTS to the canonical KiteHub wizard instead of
 * rendering the divergent XState orphan (`about:blank` preview). Verifies:
 *   - authenticated owner → redirect surface + manual link to the KiteHub wizard
 *     (`:3001 /branding/wizard`), auto-redirect attempted via window.location.assign
 *   - unauthenticated/no-tenant → graceful loading message (not blank)
 *
 * @since GAP-1214 — supersedes the GAP-726 "render wizard UI" assertions.
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen } from '@/test/utils';
import { useAuthStore } from '@/stores/auth-store';
import BrandingWizardPage from '../page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/branding/wizard',
  useSearchParams: () => new URLSearchParams(''),
}));

const TEST_TENANT = '22222222-2222-2222-2222-222222222222';

describe('BrandingWizardPage (GAP-1214 — unify → KiteHub canonical)', () => {
  let assignSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    useAuthStore.getState().clearAuth();
    localStorage.clear();
    assignSpy = vi.fn();
    // jsdom does not implement navigation; stub assign to observe the redirect.
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...window.location, assign: assignSpy },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('redirects an authenticated owner to the canonical KiteHub wizard', () => {
    useAuthStore.getState().setAuth(
      { id: 1 as unknown as number, email: 'owner.test@test.vn', name: 'Chủ TT', userType: 'CENTER_OWNER' as never, referenceId: undefined } as never,
      'access-token',
      'refresh-token',
      TEST_TENANT,
    );

    render(<BrandingWizardPage />);

    // Manual fallback link points at the KiteHub wizard route.
    const link = screen.getByTestId('kc-wizard-redirect-link') as HTMLAnchorElement;
    expect(link).toBeInTheDocument();
    expect(link.getAttribute('href')).toMatch(/\/branding\/wizard$/);

    // Auto-redirect attempted.
    expect(assignSpy).toHaveBeenCalledWith(expect.stringMatching(/\/branding\/wizard$/));
  });

  it('does NOT render the divergent KiteClass XState wizard orphan', () => {
    useAuthStore.getState().setAuth(
      { id: 1 as unknown as number, email: 'owner.test@test.vn', name: 'Chủ TT', userType: 'CENTER_OWNER' as never, referenceId: undefined } as never,
      'access-token',
      'refresh-token',
      TEST_TENANT,
    );

    render(<BrandingWizardPage />);

    expect(screen.getByTestId('kc-wizard-redirect')).toBeInTheDocument();
  });

  it('shows graceful loading message (not blank) when session has no tenant', () => {
    render(<BrandingWizardPage />);
    expect(screen.getByText(/Đang tải thông tin trung tâm/)).toBeInTheDocument();
  });
});
