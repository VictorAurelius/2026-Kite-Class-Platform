/**
 * Component tests for Sidebar.
 *
 * GAP-559 (Wave 79 Bucket D): Owner persona sees /onboarding nav entry;
 * admin variant does NOT show onboarding (admin nav is platform-admin scoped).
 *
 * @since Wave 79 Bucket D
 */

import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';

vi.mock('next/navigation', () => ({
  usePathname: () => '/dashboard',
}));

import { Sidebar } from '../Sidebar';

describe('Sidebar — GAP-559 onboarding entry-point discoverability', () => {
  afterEach(() => {
    cleanup();
  });

  describe('customer variant (Owner persona)', () => {
    it('renders the "Bắt đầu" onboarding nav entry linking to /onboarding', () => {
      render(<Sidebar variant="customer" />);
      const onboardingLink = screen.getByTestId('customer-nav-onboarding');
      expect(onboardingLink).toBeInTheDocument();
      expect(onboardingLink).toHaveAttribute('href', '/onboarding');
      expect(onboardingLink).toHaveTextContent('Bắt đầu');
    });

    it('renders Tổng quan, Bắt đầu, Thanh toán, AI Branding, Cài đặt in order', () => {
      render(<Sidebar variant="customer" />);
      // Expect 5 customer nav entries — onboarding inserted between Tổng quan and Thanh toán.
      const links = [
        screen.getByText('Tổng quan'),
        screen.getByText('Bắt đầu'),
        screen.getByText('Thanh toán'),
        screen.getByText('AI Branding'),
        screen.getByText('Cài đặt'),
      ];
      for (const link of links) {
        expect(link).toBeInTheDocument();
      }
    });
  });

  describe('admin variant (Staff persona / Platform admin)', () => {
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
