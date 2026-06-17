/**
 * Billing Page Tests — Wave 31 Bucket B (GAP-273) + Wave flow-kh3 (GAP-1472).
 *
 * Verifies the kitehub-pro-v2 token-styled billing page. Covers the
 * trial / subscribed / loading / error views, the KPI summary tiles backed by
 * `formatVNCurrency`, and — per GAP-1472 — the "Lịch sử hóa đơn" list now wired
 * to REAL payment data via `usePaymentHistory` (no more `MOCK_INVOICES`):
 * real rows render, the real-payment detail panel renders the selected payment,
 * and the empty state shows when there are no payments.
 *
 * @since PR-Q4 — refreshed Wave 31 Bucket B; Wave flow-kh3 GAP-1472 real-data wiring
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import { mockUser, mockInstances, mockSubscription } from '@/__tests__/mocks/data';
import type { Payment } from '@/types/payment';
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

// GAP-1472 — real payment-history hook (replaces MOCK_INVOICES wiring).
vi.mock('@/hooks/use-payments', () => ({
  usePaymentHistory: vi.fn(),
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

// Mock heavy peripherals — keep G5 from `@kite/shared-ui` real so we
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
import { usePaymentHistory } from '@/hooks/use-payments';

// Two REAL payments matching the BE `PaymentResponse` shape — one COMPLETED, one
// PENDING. Amounts use real PREMIUM pricing (1.500.000đ), NOT the old fake 499k.
const mockPayments: Payment[] = [
  {
    id: 'pay-1111aaaa-2222-3333-4444-555566667777',
    subscriptionId: 'sub-123',
    amountVnd: 1500000,
    currency: 'VND',
    paymentMethod: 'VIETQR',
    status: 'COMPLETED',
    qrCodeUrl: null,
    transactionId: 'TXN-2026-06-001',
    bankCode: 'VCB',
    accountNumber: '1234567890',
    accountName: 'CONG TY KITEHUB',
    paymentContent: 'KITEHUB PREMIUM SUB',
    txnRef: 'KH26ABCXYZ01',
    paidAt: '2026-06-10T03:05:00Z',
    expiresAt: null,
    createdAt: '2026-06-10T03:00:00Z',
    updatedAt: '2026-06-10T03:05:00Z',
  },
  {
    id: 'pay-8888bbbb-9999-0000-1111-222233334444',
    subscriptionId: 'sub-123',
    amountVnd: 1500000,
    currency: 'VND',
    paymentMethod: 'VIETQR',
    status: 'PENDING',
    qrCodeUrl: 'https://img.vietqr.io/mock.png',
    transactionId: null,
    bankCode: 'VCB',
    accountNumber: '1234567890',
    accountName: 'CONG TY KITEHUB',
    paymentContent: 'KITEHUB PREMIUM SUB',
    txnRef: 'KH26DEFUVW02',
    paidAt: null,
    expiresAt: '2026-06-17T03:00:00Z',
    createdAt: '2026-06-16T03:00:00Z',
    updatedAt: '2026-06-16T03:00:00Z',
  },
];

describe('BillingPage (Wave 31 Bucket B port + GAP-1472 real payment data)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    pushMock.mockReset();

    (useAuthStore as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
      user: mockUser,
    });

    // Default: no payments. Individual tests override as needed.
    (usePaymentHistory as ReturnType<typeof vi.fn>).mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
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

  it('renders subscribed view with KPI tiles + REAL payment rows using formatVNCurrency', () => {
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
    (usePaymentHistory as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockPayments,
      isLoading: false,
      error: null,
    });

    render(<BillingPage />);

    expect(screen.getByText('Hóa đơn & Thanh toán')).toBeInTheDocument();
    expect(screen.getByTestId('current-plan-card')).toBeInTheDocument();
    expect(screen.getByTestId('billing-summary')).toBeInTheDocument();
    // KPI tiles render formatVNCurrency output (suffix lowercase Latin "đ").
    expect(screen.getByTestId('billing-summary').textContent ?? '').toMatch(/đ/);
    // Real PREMIUM amount (1.500.000đ) appears in KPI cards + payment rows.
    expect(screen.getAllByText('1.500.000đ').length).toBeGreaterThan(0);
    // Invoice list rendered with REAL payment rows (keyed by real payment id).
    expect(screen.getByTestId('invoice-list')).toBeInTheDocument();
    expect(
      screen.getByTestId('invoice-row-pay-1111aaaa-2222-3333-4444-555566667777'),
    ).toBeInTheDocument();
    // Real gateway reference (txnRef) is the row label — NOT a fake KHB- number.
    // (Appears in the row AND the default-selected detail panel.)
    expect(screen.getAllByText('KH26ABCXYZ01').length).toBeGreaterThan(0);
    expect(screen.queryByText(/KHB-2026/)).not.toBeInTheDocument();
  });

  it('renders the real-payment detail panel for the selected payment', () => {
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
    (usePaymentHistory as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockPayments,
      isLoading: false,
      error: null,
    });

    render(<BillingPage />);

    const detailPanel = screen.getByTestId('invoice-detail-panel');
    expect(detailPanel).toBeInTheDocument();
    // First payment selected by default → real-payment detail renders real fields.
    expect(screen.getByTestId('real-payment-detail')).toBeInTheDocument();
    expect(screen.getByText('Mã giao dịch')).toBeInTheDocument();
    expect(screen.getByText('TXN-2026-06-001')).toBeInTheDocument();
  });

  it('shows the pending-payment CTA when a PENDING payment is selected', () => {
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
    (usePaymentHistory as ReturnType<typeof vi.fn>).mockReturnValue({
      data: mockPayments,
      isLoading: false,
      error: null,
    });

    render(<BillingPage />);

    // Select the PENDING payment row → CTA appears → routes to the payment page.
    fireEvent.click(
      screen.getByTestId('invoice-row-pay-8888bbbb-9999-0000-1111-222233334444'),
    );
    const cta = screen.getByTestId('continue-payment-cta');
    expect(cta).toBeInTheDocument();
    fireEvent.click(cta);
    expect(pushMock).toHaveBeenCalledWith(
      '/billing/payment/pay-8888bbbb-9999-0000-1111-222233334444',
    );
  });

  it('renders the friendly empty state when there are no payments', () => {
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
    (usePaymentHistory as ReturnType<typeof vi.fn>).mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
    });

    render(<BillingPage />);

    expect(screen.getByTestId('payment-empty-state')).toBeInTheDocument();
    expect(screen.getByText('Chưa có hóa đơn nào')).toBeInTheDocument();
    // No mock rows, no detail panel content.
    expect(screen.queryByTestId('real-payment-detail')).not.toBeInTheDocument();
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
