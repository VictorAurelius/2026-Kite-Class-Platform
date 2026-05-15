/**
 * Component tests for Sidebar.
 *
 * GAP-559 (Wave 79 Bucket D): Owner persona sees /onboarding nav entry;
 * admin variant does NOT show onboarding (admin nav is platform-admin scoped).
 *
 * GAP-562b (Wave 80 Bucket C): customerNav Owner-only items (billing / branding
 * / settings) hide for STAFF role; admin nav unchanged.
 *
 * @since Wave 79 Bucket D; extended Wave 80 Bucket C
 */

import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { useAuthStore } from '@/stores/auth-store';

vi.mock('next/navigation', () => ({
  usePathname: () => '/dashboard',
}));

import { Sidebar } from '../Sidebar';

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

describe('Sidebar — nav entries by role', () => {
  beforeEach(() => {
    useAuthStore.getState().clearAuth();
  });

  afterEach(() => {
    cleanup();
    useAuthStore.getState().clearAuth();
  });

  describe('customer variant — OWNER role (full nav)', () => {
    beforeEach(() => {
      loginAs('OWNER');
    });

    it('renders the "Bắt đầu" onboarding nav entry linking to /onboarding', async () => {
      render(<Sidebar variant="customer" />);
      const onboardingLink = await screen.findByTestId('customer-nav-onboarding');
      expect(onboardingLink).toBeInTheDocument();
      expect(onboardingLink).toHaveAttribute('href', '/onboarding');
      expect(onboardingLink).toHaveTextContent('Bắt đầu');
    });

    it('renders all 5 customer nav entries (GAP-559 + GAP-562b OWNER full access)', async () => {
      render(<Sidebar variant="customer" />);
      expect(await screen.findByText('Tổng quan')).toBeInTheDocument();
      expect(await screen.findByText('Bắt đầu')).toBeInTheDocument();
      expect(await screen.findByText('Thanh toán')).toBeInTheDocument();
      expect(await screen.findByText('AI Branding')).toBeInTheDocument();
      expect(await screen.findByText('Cài đặt')).toBeInTheDocument();
    });
  });

  describe('customer variant — STAFF role (GAP-562b Owner-only hidden)', () => {
    beforeEach(() => {
      loginAs('STAFF');
    });

    it('renders Tổng quan + Bắt đầu (non-role-gated)', async () => {
      render(<Sidebar variant="customer" />);
      expect(await screen.findByText('Tổng quan')).toBeInTheDocument();
      expect(await screen.findByText('Bắt đầu')).toBeInTheDocument();
    });

    it('HIDES Thanh toán / AI Branding / Cài đặt (Owner-only)', async () => {
      render(<Sidebar variant="customer" />);
      // Wait for hydration tick by querying for a known visible item first.
      await screen.findByText('Tổng quan');
      expect(screen.queryByTestId('customer-nav-billing')).not.toBeInTheDocument();
      expect(screen.queryByTestId('customer-nav-branding')).not.toBeInTheDocument();
      expect(screen.queryByTestId('customer-nav-settings')).not.toBeInTheDocument();
      expect(screen.queryByText('Thanh toán')).not.toBeInTheDocument();
      expect(screen.queryByText('AI Branding')).not.toBeInTheDocument();
      expect(screen.queryByText('Cài đặt')).not.toBeInTheDocument();
    });
  });

  describe('admin variant (PLATFORM_ADMIN persona)', () => {
    beforeEach(() => {
      loginAs('PLATFORM_ADMIN');
    });

    it('does NOT render the customer onboarding nav entry', () => {
      render(<Sidebar variant="admin" />);
      expect(screen.queryByTestId('customer-nav-onboarding')).not.toBeInTheDocument();
      expect(screen.queryByText('Bắt đầu')).not.toBeInTheDocument();
    });

    it('renders the 4 platform-admin nav entries (GAP-519)', () => {
      render(<Sidebar variant="admin" />);
      expect(screen.getByTestId('admin-nav-beta-requests')).toBeInTheDocument();
      expect(screen.getByTestId('admin-nav-instances')).toBeInTheDocument();
      expect(screen.getByTestId('admin-nav-payments')).toBeInTheDocument();
      expect(screen.getByTestId('admin-nav-revenue')).toBeInTheDocument();
    });
  });
});
