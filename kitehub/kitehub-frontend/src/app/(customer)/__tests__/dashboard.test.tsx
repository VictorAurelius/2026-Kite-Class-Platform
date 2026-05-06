/**
 * Dashboard Page Tests
 *
 * Tests for the main customer dashboard page.
 *
 * @since PR-Q4
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import { mockUser, mockInstances } from '@/__tests__/mocks/data';
import DashboardPage from '../dashboard/page';

// Mock the stores and hooks
vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/hooks/use-instances', () => ({
  useOwnerInstances: vi.fn(),
}));

vi.mock('@/components/onboarding/OnboardingWizard', () => ({
  OnboardingWizard: () => <div data-testid="onboarding-wizard" />,
  useOnboardingWizard: () => ({
    shouldShow: false,
    showWizard: vi.fn(),
    hideWizard: vi.fn(),
  }),
}));

// Mock Next.js Link
vi.mock('next/link', () => ({
  default: ({ children, href }: { children: React.ReactNode; href: string }) => (
    <a href={href}>{children}</a>
  ),
}));

// Import mocked modules
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Default mock: authenticated user
    (useAuthStore as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      user: mockUser,
    });
  });

  it('renders dashboard with loading state', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
      refetch: vi.fn(),
    });

    render(<DashboardPage />);

    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('renders dashboard with instances', async () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<DashboardPage />);

    // Check welcome message
    await waitFor(() => {
      expect(screen.getByText(/Chào buổi/)).toBeInTheDocument();
      expect(screen.getByText(mockUser.name, { exact: false })).toBeInTheDocument();
    });

    // Check instances are displayed
    expect(screen.getByText('Trung tâm của bạn')).toBeInTheDocument();
    expect(screen.getByText(mockInstances[0]!.name)).toBeInTheDocument();
    expect(screen.getByText(mockInstances[1]!.name)).toBeInTheDocument();
  });

  it('renders empty state when no instances', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<DashboardPage />);

    expect(screen.getByText('Chưa có trung tâm nào')).toBeInTheDocument();
    expect(screen.getByText('Tạo trung tâm KiteClass đầu tiên để bắt đầu quản lý')).toBeInTheDocument();
  });

  it('renders error state with retry button', () => {
    const mockRefetch = vi.fn();
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error('Network error'),
      refetch: mockRefetch,
    });

    render(<DashboardPage />);

    expect(screen.getByText('Không thể tải danh sách instance')).toBeInTheDocument();

    // Should have retry functionality
    const errorAlert = screen.getByTestId('error-alert');
    expect(errorAlert).toBeInTheDocument();
  });

  it('displays trial expiring warning when applicable', () => {
    const trialInstance = {
      ...mockInstances[0],
      isOnTrial: true,
      trialDaysLeft: 2,
      trialExpiresAt: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString(),
    };

    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: [trialInstance],
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<DashboardPage />);

    // Check for trial warning header
    expect(screen.getByText('Trial sắp hết hạn')).toBeInTheDocument();

    // Check for upgrade link
    expect(screen.getByRole('link', { name: /Nâng cấp ngay/i })).toBeInTheDocument();
  });

  it('does not display trial warning when trial has more than 3 days', () => {
    const trialInstance = {
      ...mockInstances[0],
      isOnTrial: true,
      trialDaysLeft: 7,
      trialExpiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
    };

    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: [trialInstance],
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<DashboardPage />);

    expect(screen.queryByText('Trial sắp hết hạn')).not.toBeInTheDocument();
  });

  it('renders header quick actions (Wave 31 Bucket A — search + brand CTA)', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<DashboardPage />);

    // Wave 31 replaces the old 4 quick-action chips with a kitehub-pro v2-
    // style ⌘K palette toggle + a single primary "Tạo brand mới" CTA.
    expect(screen.getByTestId('open-command-palette')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Tạo brand mới/i })).toBeInTheDocument();
    // Tier-status card surfaces the billing entry-points instead of separate chips.
    expect(screen.getByTestId('tier-status-card')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Hóa đơn/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Nâng cấp/i })).toBeInTheDocument();
  });

  it('renders Wave 31 KPI strip with 6 KPI cards', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<DashboardPage />);

    expect(screen.getByTestId('kpi-strip')).toBeInTheDocument();
    const cards = screen.getAllByTestId('kpi-card');
    expect(cards.length).toBe(6);
    expect(screen.getByText('Gói hiện tại')).toBeInTheDocument();
    expect(screen.getByText('Lượt gọi API · 7 ngày')).toBeInTheDocument();
  });

  it('renders setup checklist for new instances', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<DashboardPage />);

    expect(screen.getByText('Bắt đầu nhanh')).toBeInTheDocument();
    expect(screen.getByText(/hoàn thành/)).toBeInTheDocument();
  });

  it('renders promo banners', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<DashboardPage />);

    expect(screen.getByText('AI tạo website cho trung tâm')).toBeInTheDocument();
    // Wave 31 replaced "Mở rộng quy mô trung tâm" upgrade banner with a
    // help/onboarding-replay CTA — upgrade now lives in the tier-status card.
    expect(screen.getByText('Xem lại hướng dẫn nhanh')).toBeInTheDocument();
  });
});
