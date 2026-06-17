/**
 * Component tests for PendingPaymentBanner — GAP-1471 cancel affordance.
 *
 * Verifies the "Hủy yêu cầu thanh toán" CTA renders and that confirming the
 * AlertDialog invokes `useCancelPendingPayment` with the subscription id.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@/test/test-utils';
import { PendingPaymentBanner } from '../PendingPaymentBanner';
import type { PendingPaymentStatus } from '@/types/subscription';

const mutateMock = vi.fn();

vi.mock('@/hooks/use-subscriptions', () => ({
  useCancelPendingPayment: () => ({ mutate: mutateMock, isPending: false }),
}));

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
    prefetch: vi.fn(),
  }),
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

const pendingFixture: PendingPaymentStatus = {
  subscriptionId: 'sub-123',
  pendingPaymentId: 'pay-1',
  amountVnd: 500_000,
  status: 'PENDING',
  expiresAt: null,
  adminConfirmSla: 'trong vòng 24 giờ làm việc',
};

describe('PendingPaymentBanner — GAP-1471 cancel affordance', () => {
  beforeEach(() => {
    mutateMock.mockClear();
  });

  it('renders the cancel CTA when a payment is PENDING', () => {
    render(<PendingPaymentBanner pending={pendingFixture} />);
    expect(screen.getByTestId('pending-payment-cancel-cta')).toBeInTheDocument();
    // the existing "view transfer info" CTA is still present
    expect(screen.getByTestId('pending-payment-view-cta')).toBeInTheDocument();
  });

  it('invokes the cancel mutation with the subscription id after confirming the dialog', async () => {
    render(<PendingPaymentBanner pending={pendingFixture} />);

    // open the confirm dialog
    fireEvent.click(screen.getByTestId('pending-payment-cancel-cta'));

    // confirm the cancel
    const confirm = await screen.findByTestId('pending-payment-cancel-confirm');
    fireEvent.click(confirm);

    expect(mutateMock).toHaveBeenCalledTimes(1);
    expect(mutateMock).toHaveBeenCalledWith(
      'sub-123',
      expect.objectContaining({
        onSuccess: expect.any(Function),
        onError: expect.any(Function),
      })
    );
  });

  it('renders nothing when the payment is not PENDING', () => {
    const { container } = render(
      <PendingPaymentBanner pending={{ ...pendingFixture, status: 'COMPLETED' }} />
    );
    expect(container).toBeEmptyDOMElement();
  });
});
