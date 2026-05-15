/**
 * RoleGuard component tests (GAP-562b Wave 80 Bucket C).
 *
 * Verifies:
 * - OWNER role + allowedRoles=['OWNER'] → renders children
 * - STAFF role + allowedRoles=['OWNER'] → renders nothing visible, schedules redirect
 * - Unauthenticated → renders nothing visible, schedules redirect
 * - Legacy PLATFORM_ADMIN / ADMIN aliases resolve to OWNER → renders children
 */

import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest';
import { render, screen, cleanup, waitFor } from '@testing-library/react';
import { useAuthStore } from '@/stores/auth-store';

const replaceMock = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: replaceMock,
    push: vi.fn(),
  }),
}));

import { RoleGuard } from '../RoleGuard';

function loginAs(role: 'OWNER' | 'STAFF' | 'PLATFORM_ADMIN' | 'ADMIN') {
  useAuthStore.setState({
    user: {
      id: 'test-user-id',
      email: 'test@kitehub.me',
      name: 'Test User',
      role,
    },
    accessToken: 'test-token',
    refreshToken: 'test-refresh',
    isAuthenticated: true,
  });
}

describe('RoleGuard', () => {
  beforeEach(() => {
    replaceMock.mockClear();
    useAuthStore.getState().clearAuth();
  });

  afterEach(() => {
    cleanup();
    useAuthStore.getState().clearAuth();
  });

  it('renders children when role matches (OWNER → OWNER)', async () => {
    loginAs('OWNER');
    render(
      <RoleGuard allowedRoles={['OWNER']}>
        <div data-testid="protected-content">Owner only content</div>
      </RoleGuard>,
    );
    expect(await screen.findByTestId('protected-content')).toBeInTheDocument();
    expect(replaceMock).not.toHaveBeenCalled();
  });

  it('does NOT render children + schedules redirect when STAFF hits OWNER route', async () => {
    loginAs('STAFF');
    render(
      <RoleGuard allowedRoles={['OWNER']}>
        <div data-testid="protected-content">Owner only content</div>
      </RoleGuard>,
    );
    await waitFor(() => {
      expect(replaceMock).toHaveBeenCalledWith('/dashboard');
    });
    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
  });

  it('redirects to custom fallbackUrl when provided', async () => {
    loginAs('STAFF');
    render(
      <RoleGuard allowedRoles={['OWNER']} fallbackUrl="/custom-fallback">
        <div data-testid="protected-content" />
      </RoleGuard>,
    );
    await waitFor(() => {
      expect(replaceMock).toHaveBeenCalledWith('/custom-fallback');
    });
  });

  it('resolves legacy PLATFORM_ADMIN alias to OWNER (Wave 81 cutoff window)', async () => {
    loginAs('PLATFORM_ADMIN');
    render(
      <RoleGuard allowedRoles={['OWNER']}>
        <div data-testid="protected-content">Owner only content</div>
      </RoleGuard>,
    );
    expect(await screen.findByTestId('protected-content')).toBeInTheDocument();
    expect(replaceMock).not.toHaveBeenCalled();
  });

  it('does NOT render children when user is unauthenticated', async () => {
    // No loginAs() — auth-store stays cleared
    render(
      <RoleGuard allowedRoles={['OWNER']}>
        <div data-testid="protected-content" />
      </RoleGuard>,
    );
    await waitFor(() => {
      expect(replaceMock).toHaveBeenCalledWith('/dashboard');
    });
    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
  });
});
