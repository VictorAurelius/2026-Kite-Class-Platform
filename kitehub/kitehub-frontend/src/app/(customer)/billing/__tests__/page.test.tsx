/**
 * Billing Page Tests — Wave 31 Bucket B (GAP-273)
 *
 * Verifies the kitehub-pro-v2 token-styled billing page with G5
 * (`PaymentMethodSelector`) + G6 (`InvoiceDetail`) `@kite/shared-ui`
 * integration. Tests cover the trial / subscribed / loading / error views,
 * the KPI summary tiles backed by `formatVNCurrency`, the G6 invoice detail
 * render path, and the G5 payment-method tier-upgrade smoke flow.
 *
 * @since PR-Q4 — refreshed Wave 31 Bucket B
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
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
  // GAP-1257-FE — pending-payment-status hook (code-to-contract). Default null
  // = no pending payment so the awaiting-confirmation banner doesn't render.
  usePendingPaymentStatus: vi.fn(() => ({ data: null })),
}));

const pushMock = vi.fn();

// Mock Next.js navigation
vi.mock('next/navigation', () => ({
  useSearchParams: () => ({
    get: () => null,
  }),
  useRouter: () => ({
    push: pushMock,
  }),
}));

// Mock heavy peripherals — keep G5/G6 from `@kite/shared-ui` real so we
// validate true integration.
vi.mock('@/components/billing/CurrentPlanCard', () => ({
  CurrentPlanCard: ({ subscription }: { subscription: typeof mockSubscription }) => (
    <div data-testid="current-plan-card">Current Plan: {subscription.tier}</div>
  ),
}));

vi.mock('@/components/billing/PlanComparison', () => ({
  PlanComparison: ({ currentTier }: { currentTier: string | null }) => (
    <div data-testid="plan-comparison">
      {currentTier ? `Comparing plans (current: ${currentTier})` : 'All plans available'}
    </div>
  ),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

// Import mocks
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useActiveSubscription } from '@/hooks/use-subscriptions';

describe('BillingPage (Wave 31 Bucket B port)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    pushMock.mockReset();

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

  it('renders trial / empty state when no subscription', async () => {
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

    // GAP-1079-FE: empty-state copy (404 no-active-sub handled gracefully).
    expect(screen.getByText('Chưa có gói trả phí')).toBeInTheDocument();
    expect(
      screen.getByText(
        'Bạn đang dùng gói Trial/Miễn phí. Chọn gói phù hợp để mở khóa đầy đủ tính năng.',
      ),
    ).toBeInTheDocument();
    // PlanComparison loaded via `next/dynamic` — wait for it.
    expect(await screen.findByTestId('plan-comparison')).toHaveTextContent(
      'All plans available',
    );
  });

  it('renders default subscribed view with KPI tiles + invoice list using formatVNCurrency', async () => {
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

    expect(screen.getByText('Hóa đơn & Thanh toán')).toBeInTheDocument();
    expect(screen.getByTestId('current-plan-card')).toBeInTheDocument();
    expect(screen.getByTestId('billing-summary')).toBeInTheDocument();
    // KPI tiles render formatVNCurrency output — check VND symbol presence on
    // any of the values (locale 'vi-VN' uses non-breaking space + ₫).
    expect(screen.getByTestId('billing-summary').textContent ?? '').toMatch(/đ/);
    // Invoice list rendered with mock subscription invoices.
    expect(screen.getByTestId('invoice-list')).toBeInTheDocument();
    expect(screen.getByTestId('invoice-row-inv-2026-04')).toBeInTheDocument();
  });

  it('renders G6 InvoiceDetail panel when an invoice is selected', async () => {
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

    const detailPanel = await screen.findByTestId('invoice-detail-panel');
    expect(detailPanel).toBeInTheDocument();
    // First mock invoice is selected by default. Wait for dynamic G6 to mount.
    // The invoice number appears in BOTH the list row AND the G6 detail panel
    // — we expect at least 2 hits to confirm G6 rendered.
    const matches = await screen.findAllByText('KHB-2026-04-001');
    expect(matches.length).toBeGreaterThanOrEqual(2);
  });

  it('disables tier-upgrade CTA until a payment method is selected', () => {
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

    const cta = screen.getByTestId('tier-upgrade-cta');
    expect(cta).toBeDisabled();
  });

  it('routes to /billing/upgrade with selected payment method via G5 selector', () => {
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

    // G5 PaymentMethodSelector renders radio buttons for each method.
    const momoRadio = screen.getByRole('radio', { name: /MoMo/i });
    fireEvent.click(momoRadio);

    const cta = screen.getByTestId('tier-upgrade-cta');
    expect(cta).not.toBeDisabled();
    fireEvent.click(cta);

    expect(pushMock).toHaveBeenCalledWith('/billing/upgrade?method=MOMO');
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

    expect(
      screen.getByText('Không thể tải thông tin thanh toán. Vui lòng thử lại.'),
    ).toBeInTheDocument();
  });
});
