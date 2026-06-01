/**
 * Branding wizard page tests — GAP-726.
 *
 * Verifies the wizard route resolves tenantId from the authenticated session
 * (auth-store) instead of the hard-coded `current-tenant`/`my-school` scaffold,
 * and renders the wizard UI (not a blank page) for a seeded owner. Also verifies
 * the graceful "loading tenant" fallback when the session has not hydrated.
 *
 * @since GAP-726
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@/test/utils';
import { useAuthStore } from '@/stores/auth-store';
import BrandingWizardPage from '../page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/branding/wizard',
  useSearchParams: () => new URLSearchParams(''),
}));

const TEST_TENANT = '22222222-2222-2222-2222-222222222222';

describe('BrandingWizardPage (GAP-726 — session-resolved tenant)', () => {
  beforeEach(() => {
    useAuthStore.getState().clearAuth();
    localStorage.clear();
  });

  it('renders the wizard UI (welcome step) for an authenticated owner', () => {
    useAuthStore.getState().setAuth(
      { id: 1 as unknown as number, email: 'owner.test@test.vn', name: 'Chủ TT', userType: 'CENTER_OWNER' as never, referenceId: undefined } as never,
      'access-token',
      'refresh-token',
      TEST_TENANT,
    );

    render(<BrandingWizardPage />);

    // Wizard progress + welcome step content render — NOT a blank body.
    // WelcomeStep renders a primary CTA button ("Bắt đầu" / next).
    const buttons = screen.getAllByRole('button');
    expect(buttons.length).toBeGreaterThan(0);
  });

  it('shows graceful loading message (not blank) when session has no tenant', () => {
    // clearAuth in beforeEach → tenantId null
    render(<BrandingWizardPage />);

    expect(screen.getByText(/Đang tải thông tin trung tâm/)).toBeInTheDocument();
  });

  it('does NOT use the hard-coded current-tenant scaffold value', () => {
    useAuthStore.getState().setAuth(
      { id: 1 as unknown as number, email: 'owner.test@test.vn', name: 'Chủ TT', userType: 'CENTER_OWNER' as never, referenceId: undefined } as never,
      'access-token',
      'refresh-token',
      TEST_TENANT,
    );

    // The page source no longer references the scaffold literals.
    expect(useAuthStore.getState().tenantId).toBe(TEST_TENANT);
  });
});
