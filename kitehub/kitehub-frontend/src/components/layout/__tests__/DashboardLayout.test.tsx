/**
 * DashboardLayout mount tests — GAP-1443.
 *
 * Regression guard: the SupportMenu (`?` floating feedback/support entry) and
 * beta banner are mounted via OnboardingCoordinator inside the authenticated
 * customer layout. Before GAP-1443 the coordinator was orphaned (never mounted)
 * so owners had NO support/feedback affordance. This asserts the trigger is in
 * the rendered tree.
 *
 * @since Wave flow-fix-1 Phase-3 — GAP-1443
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { DashboardLayout } from '../DashboardLayout';

// Authenticated owner so the layout renders its body (not the login redirect).
// Selector-aware: components call useAuthStore() (whole store) AND
// useAuthStore(s => s.user?.role) (selector) — support both forms.
vi.mock('@/stores/auth-store', () => {
  const state = {
    isAuthenticated: true,
    user: { email: 'owner@test.kitehub.me', role: 'OWNER' },
    clearAuth: vi.fn(),
  };
  return {
    useAuthStore: (selector?: (s: typeof state) => unknown) =>
      typeof selector === 'function' ? selector(state) : state,
  };
});

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

// OnboardingCoordinator reads the onboarding phase; stub to a steady phase so
// SupportMenu renders (it hides only while the onboarding modal is open).
vi.mock('@/hooks/useOnboardingPhase', () => ({
  useOnboardingPhase: () => ({ phase: 'steady' }),
}));

// Sidebar pulls auth/navigation internals not under test — stub it out.
vi.mock('../Sidebar', () => ({
  Sidebar: () => <nav data-testid="sidebar-stub" />,
}));

describe('DashboardLayout — support/feedback affordance mounted (GAP-1443)', () => {
  it('renders the SupportMenu floating trigger for authenticated owners', () => {
    render(
      <DashboardLayout>
        <p>child content</p>
      </DashboardLayout>,
    );

    expect(screen.getByTestId('support-menu-trigger')).toBeInTheDocument();
  });

  it('still renders page children alongside the support entry', () => {
    render(
      <DashboardLayout>
        <p>child content</p>
      </DashboardLayout>,
    );

    expect(screen.getByText('child content')).toBeInTheDocument();
  });
});
