/**
 * Admin Dashboard Page Tests.
 *
 * Tests dashboard statistics, revenue chart, and quick actions.
 *
 * @author KiteHub Team
 * @since PR 5.8
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@/test/test-utils';
import AdminDashboardPage from './page';
import { mockDashboardStats } from '@/test/mocks/admin-data';

// Mock the hooks
const mockUseAdminDashboard = vi.fn();
const mockUseAdminRevenue = vi.fn();
const mockUseAdminPendingPayments = vi.fn();

vi.mock('@/hooks/use-admin', () => ({
  useAdminDashboard: () => mockUseAdminDashboard(),
  useAdminRevenue: () => mockUseAdminRevenue(),
  useAdminPendingPayments: () => mockUseAdminPendingPayments(),
}));

describe('AdminDashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Default mock returns
    mockUseAdminDashboard.mockReturnValue({
      data: mockDashboardStats,
      isLoading: false,
      error: null,
    });

    mockUseAdminRevenue.mockReturnValue({
      data: null,
      isLoading: false,
      error: null,
    });

    // GAP-1440: dashboard derives the pending-payments KPI from this list.
    // 8 entries → "Thanh toán chờ xác nhận" shows 8.
    mockUseAdminPendingPayments.mockReturnValue({
      data: new Array(8).fill(null),
      isLoading: false,
      error: null,
    });
  });

  describe('Rendering', () => {
    it('renders dashboard title', () => {
      render(<AdminDashboardPage />);

      expect(screen.getByText('Dashboard')).toBeInTheDocument();
    });

    it('renders stats cards', () => {
      render(<AdminDashboardPage />);

      // Check all stat labels
      expect(screen.getByText('Tổng Instance')).toBeInTheDocument();
      expect(screen.getByText('Đang hoạt động')).toBeInTheDocument();
      expect(screen.getByText('Đang dùng thử')).toBeInTheDocument();
      expect(screen.getByText('Tạm ngưng')).toBeInTheDocument();
    });

    it('displays correct stats values', () => {
      render(<AdminDashboardPage />);

      // Check stats values from mock
      expect(screen.getByText('150')).toBeInTheDocument(); // totalInstances
      expect(screen.getByText('120')).toBeInTheDocument(); // activeInstances
      expect(screen.getByText('25')).toBeInTheDocument(); // trialInstances
      expect(screen.getByText('5')).toBeInTheDocument(); // suspendedInstances
    });

    it('displays revenue stats', () => {
      render(<AdminDashboardPage />);

      // Revenue section
      expect(screen.getByText('Doanh thu')).toBeInTheDocument();
      expect(screen.getByText('Tổng doanh thu')).toBeInTheDocument();
    });

    it('displays pending payments count', () => {
      render(<AdminDashboardPage />);

      // Pending payments indicator
      expect(screen.getByText(/chờ xác nhận/i)).toBeInTheDocument();
      expect(screen.getByText('8')).toBeInTheDocument(); // pendingPayments
    });
  });

  describe('Loading State', () => {
    it('shows loading skeleton while fetching', () => {
      mockUseAdminDashboard.mockReturnValue({
        data: undefined,
        isLoading: true,
        error: null,
      });

      render(<AdminDashboardPage />);

      // Should show skeleton elements
      const skeletons = document.querySelectorAll('[class*="animate-pulse"]');
      expect(skeletons.length).toBeGreaterThan(0);
    });
  });

  describe('Error State', () => {
    it('shows error message on failure', () => {
      mockUseAdminDashboard.mockReturnValue({
        data: undefined,
        isLoading: false,
        error: new Error('Failed to load dashboard'),
      });

      render(<AdminDashboardPage />);

      expect(screen.getByText(/lỗi/i)).toBeInTheDocument();
    });
  });

  describe('Quick Actions', () => {
    it('renders link to instances page', () => {
      render(<AdminDashboardPage />);

      const instancesLink = screen.getByRole('link', { name: /quản lý instance/i });
      expect(instancesLink).toHaveAttribute('href', '/admin/instances');
    });

    it('renders link to payments page', () => {
      render(<AdminDashboardPage />);

      const paymentsLink = screen.getByRole('link', { name: /thanh toán/i });
      expect(paymentsLink).toHaveAttribute('href', '/admin/payments');
    });
  });

  describe('New Instances This Month', () => {
    it('displays new instances count', () => {
      render(<AdminDashboardPage />);

      expect(screen.getByText(/mới trong tháng/i)).toBeInTheDocument();
      expect(screen.getByText('12')).toBeInTheDocument(); // newInstancesThisMonth
    });
  });

  describe('Empty Data', () => {
    it('handles zero stats gracefully', () => {
      mockUseAdminDashboard.mockReturnValue({
        data: {
          totalInstances: 0,
          activeInstances: 0,
          trialInstances: 0,
          suspendedInstances: 0,
          totalRevenue: 0,
          monthlyRevenue: 0,
          pendingPayments: 0,
          newInstancesThisMonth: 0,
        },
        isLoading: false,
        error: null,
      });

      render(<AdminDashboardPage />);

      // All zeros should be displayed correctly
      const zeros = screen.getAllByText('0');
      expect(zeros.length).toBeGreaterThan(0);
    });
  });
});
