/**
 * Component tests for AdminLayout.
 *
 * GAP-518 (FE role-guard unification): accepts both PLATFORM_ADMIN (canonical
 * backend role) and legacy ADMIN; rejects OWNER + unauthenticated.
 * GAP-519 (admin sidebar nav): renders sidebar with 4 testid'd links.
 *
 * @since Wave 72a Bucket C
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { useAuthStore } from '@/stores/auth-store';

const mockReplace = vi.fn();
const mockPush = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
  usePathname: () => '/admin',
}));

import { AdminLayout } from '../AdminLayout';

describe('AdminLayout — role-guard (GAP-518)', () => {
  beforeEach(() => {
    useAuthStore.getState().clearAuth();
    mockReplace.mockClear();
    mockPush.mockClear();
  });

  afterEach(() => {
    cleanup();
  });

  it('accepts PLATFORM_ADMIN role (canonical backend role) and renders children', () => {
    useAuthStore.getState().setAuth(
      {
        id: 'platform-admin-uuid',
        email: 'admin@kitehub.me',
        name: 'Platform Admin',
        role: 'PLATFORM_ADMIN',
      },
      'access-token',
      'refresh-token',
    );

    render(
      <AdminLayout>
        <div data-testid="admin-child-content">Admin Page Content</div>
      </AdminLayout>,
    );

    expect(screen.getByTestId('admin-child-content')).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('accepts legacy ADMIN role for backward compatibility', () => {
    useAuthStore.getState().setAuth(
      {
        id: 'legacy-admin-uuid',
        email: 'legacy@example.com',
        name: 'Legacy Admin',
        role: 'ADMIN',
      },
      'access-token',
      'refresh-token',
    );

    render(
      <AdminLayout>
        <div data-testid="admin-child-content">Admin Page Content</div>
      </AdminLayout>,
    );

    expect(screen.getByTestId('admin-child-content')).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('rejects OWNER role and redirects to /login', () => {
    useAuthStore.getState().setAuth(
      {
        id: 'owner-uuid',
        email: 'owner@example.com',
        name: 'Owner User',
        role: 'OWNER',
      },
      'access-token',
      'refresh-token',
    );

    render(
      <AdminLayout>
        <div data-testid="admin-child-content">Admin Page Content</div>
      </AdminLayout>,
    );

    // OWNER does not get admin chrome — should NOT render children
    expect(screen.queryByTestId('admin-child-content')).not.toBeInTheDocument();
    expect(mockReplace).toHaveBeenCalledWith('/login');
  });

  it('redirects unauthenticated visitors to /login', () => {
    // No setAuth call — store is in cleared state
    render(
      <AdminLayout>
        <div data-testid="admin-child-content">Admin Page Content</div>
      </AdminLayout>,
    );

    expect(screen.queryByTestId('admin-child-content')).not.toBeInTheDocument();
    expect(mockReplace).toHaveBeenCalledWith('/login');
  });
});

describe('AdminLayout — sidebar nav (GAP-519)', () => {
  beforeEach(() => {
    useAuthStore.getState().clearAuth();
    mockReplace.mockClear();
    mockPush.mockClear();

    useAuthStore.getState().setAuth(
      {
        id: 'platform-admin-uuid',
        email: 'admin@kitehub.me',
        name: 'Platform Admin',
        role: 'PLATFORM_ADMIN',
      },
      'access-token',
      'refresh-token',
    );
  });

  afterEach(() => {
    cleanup();
  });

  it('renders all 4 admin nav links with data-testid attributes', () => {
    render(
      <AdminLayout>
        <div>content</div>
      </AdminLayout>,
    );

    expect(screen.getByTestId('admin-nav-beta-requests')).toBeInTheDocument();
    expect(screen.getByTestId('admin-nav-instances')).toBeInTheDocument();
    expect(screen.getByTestId('admin-nav-payments')).toBeInTheDocument();
    expect(screen.getByTestId('admin-nav-revenue')).toBeInTheDocument();
  });

  it('admin nav links point to expected /admin/* routes', () => {
    render(
      <AdminLayout>
        <div>content</div>
      </AdminLayout>,
    );

    expect(screen.getByTestId('admin-nav-beta-requests')).toHaveAttribute(
      'href',
      '/admin/beta-requests',
    );
    expect(screen.getByTestId('admin-nav-instances')).toHaveAttribute('href', '/admin/instances');
    expect(screen.getByTestId('admin-nav-payments')).toHaveAttribute('href', '/admin/payments');
    expect(screen.getByTestId('admin-nav-revenue')).toHaveAttribute('href', '/admin/revenue');
  });
});
