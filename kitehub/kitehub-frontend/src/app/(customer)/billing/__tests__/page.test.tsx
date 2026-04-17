/**
 * Billing Page Tests
 *
 * Tests for billing/subscription management page.
 *
 * @since PR-Q4
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import { mockUser, mockInstances, mockSubscription } from '@/__tests__/mocks/data';
import BillingPage from '../page';

// Mock stores and hooks
vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/hooks/use-instances', () => ({
  useOwnerInstances: vi.fn(),
}));

vi.mock('@/hooks/use-subscriptions', () => ({
  useActiveSubscription: vi.fn(),
}));

// Mock Next.js navigation
vi.mock('next/navigation', () => ({
  useSearchParams: () => ({
    get: () => null,
  }),
  useRouter: () => ({
    push: vi.fn(),
  }),
}));

// Mock components
vi.mock('@/components/billing/CurrentPlanCard', () => ({
  CurrentPlanCard: ({ subscription }: { subscription: typeof mockSubscription }) => (
    <div data-testid="current-plan-card">
      Current Plan: {subscription.tier}
    </div>
  ),
}));

vi.mock('@/components/billing/PlanComparison', () => ({
  PlanComparison: ({ currentTier }: { currentTier: string | null }) => (
    <div data-testid="plan-comparison">
      {currentTier ? `Comparing plans (current: ${currentTier})` : 'All plans available'}
    </div>
  ),
}));

// Mock toast
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
  },
}));

// Import mocks
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useActiveSubscription } from '@/hooks/use-subscriptions';

describe('BillingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    (useAuthStore as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      user: mockUser,
    });
  });

  it('renders loading state', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    });

    (useActiveSubscription as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    });

    render(<BillingPage />);

    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('renders error state when instances fail to load', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error('Network error'),
    });

    (useActiveSubscription as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: null,
    });

    render(<BillingPage />);

    expect(screen.getByText('Không thể tải thông tin thanh toán. Vui lòng thử lại.')).toBeInTheDocument();
  });

  it('renders trial user view (no subscription)', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
    });

    (useActiveSubscription as ReturnType<typeof vi.fn>).mockReturnValue({
      data: null,
      isLoading: false,
      error: null,
    });

    render(<BillingPage />);

    expect(screen.getByText('Chưa có gói đăng ký')).toBeInTheDocument();
    expect(screen.getByText('Bạn đang trong giai đoạn dùng thử. Chọn gói phù hợp với nhu cầu của bạn.')).toBeInTheDocument();
    expect(screen.getByTestId('plan-comparison')).toHaveTextContent('All plans available');
  });

  it('renders active subscription view', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
    });

    (useActiveSubscription as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockSubscription,
      isLoading: false,
      error: null,
    });

    render(<BillingPage />);

    expect(screen.getByText('Thanh toán & Đăng ký')).toBeInTheDocument();
    expect(screen.getByText('Quản lý gói đăng ký và lịch sử thanh toán của bạn')).toBeInTheDocument();
    expect(screen.getByTestId('current-plan-card')).toBeInTheDocument();
    expect(screen.getByTestId('plan-comparison')).toBeInTheDocument();
  });

  it('renders action buttons for subscribed users', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
    });

    (useActiveSubscription as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockSubscription,
      isLoading: false,
      error: null,
    });

    render(<BillingPage />);

    expect(screen.getByRole('button', { name: /Lịch sử/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Nâng cấp/i })).toBeInTheDocument();
  });

  it('handles subscription API error (shows trial view)', () => {
    (useOwnerInstances as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockInstances,
      isLoading: false,
      error: null,
    });

    (useActiveSubscription as ReturnType<typeof vi.fn>).mockReturnValue({
      data: null,
      isLoading: false,
      error: { status: 400, message: 'No subscription found' },
    });

    render(<BillingPage />);

    // Should show trial user view (no subscription)
    expect(screen.getByText('Chưa có gói đăng ký')).toBeInTheDocument();
  });
});
