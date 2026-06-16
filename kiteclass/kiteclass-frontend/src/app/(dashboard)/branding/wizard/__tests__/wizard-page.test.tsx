/**
 * Branding wizard route tests — GAP-1214 (UNIFY) + GAP-1447 (no auto-bounce).
 *
 * The AI Branding generation wizard is a KiteHub platform capability (KH-6).
 * This KiteClass route hands off to the canonical KiteHub wizard instead of
 * rendering the divergent XState orphan (`about:blank` preview).
 *
 * GAP-1447: the old auto-`window.location.assign` bounced owners onto the
 * KiteHub login form with no shared session — a dead-end (no cross-product SSO
 * yet). The route now renders an EXPLICIT hand-off card (no auto-bounce); the
 * manual link opens in a new tab so the KiteClass session is preserved, and the
 * card warns a separate KiteHub login may be required. Verifies:
 *   - authenticated owner → hand-off card + manual link to KiteHub wizard
 *     (`:3001 /branding/wizard`), opens in new tab, NO auto window.location.assign
 *   - login-required notice present (no silent dead-end)
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

  it('hands off an authenticated owner to the KiteHub wizard WITHOUT auto-bounce (GAP-1447)', () => {
    useAuthStore.getState().setAuth(
      { id: 1 as unknown as number, email: 'owner.test@test.vn', name: 'Chủ TT', userType: 'CENTER_OWNER' as never, referenceId: undefined } as never,
      'access-token',
      'refresh-token',
      TEST_TENANT,
    );

    render(<BrandingWizardPage />);

    // Manual hand-off link points at the KiteHub wizard route + opens in a new tab
    // so the KiteClass session is preserved (no dead-end on the KiteHub login form).
    const link = screen.getByTestId('kc-wizard-redirect-link') as HTMLAnchorElement;
    expect(link).toBeInTheDocument();
    expect(link.getAttribute('href')).toMatch(/\/branding\/wizard$/);
    expect(link.getAttribute('target')).toBe('_blank');

    // GAP-1447: NO silent auto-redirect — owner is not bounced onto the KiteHub login.
    expect(assignSpy).not.toHaveBeenCalled();

    // The card warns a separate KiteHub login may be required (no silent dead-end).
    expect(screen.getByText(/chưa có đăng nhập dùng chung/i)).toBeInTheDocument();
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
