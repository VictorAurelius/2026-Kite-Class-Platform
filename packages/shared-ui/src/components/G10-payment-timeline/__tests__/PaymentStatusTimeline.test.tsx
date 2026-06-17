/**
 * G10 Payment Status Timeline — RTL coverage of the 5 spec'd states (pending,
 * paid, partial-paid, overdue, refunded) + step ordering + the cross-component
 * re-use of `formatVNCurrency` from G6.
 *
 * Spec source: `ui_kits/components/G10-payment-timeline/README.md` + 5 HTML state
 * files under `states/`. Vietnamese-only labels per CLAUDE.md.
 *
 * NOTE — the cross-component `formatVNCurrency` import below is INTENTIONAL:
 * it is a smoke test verifying the proof-of-concept for sharing utilities
 * across G* components inside `@kite/shared-ui` works (relative import
 * resolves; identity preserved).
 */

import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { PaymentStatusTimeline } from '../PaymentStatusTimeline';
import { formatVNCurrency as g10ReExport } from '../PaymentStatusTimeline';
// Cross-component re-use proof: import the SAME helper directly from G6.
import { formatVNCurrency as g6Direct } from '../../G6-invoice-detail/utils';
import type { TimelineEvent, PaymentTimelineState } from '../types';

const ISSUED_AT = new Date('2026-10-01T09:15:00Z');
const PENDING_AT = new Date('2026-10-01T09:16:00Z');
const RECEIVED_AT = new Date('2026-10-14T19:42:00Z');
const CONFIRMED_AT = new Date('2026-10-14T19:43:00Z');
const COMPLETED_AT = new Date('2026-10-14T19:44:00Z');
const FAILED_AT = new Date('2026-10-15T10:00:00Z');
const REFUND_AT = new Date('2026-10-18T10:35:00Z');

function buildEvents(state: PaymentTimelineState): TimelineEvent[] {
  const base: TimelineEvent[] = [
    {
      step: 'CREATED',
      at: ISSUED_AT,
      actor: 'Hệ thống tự động',
      note: 'Email đã gửi tới phụ huynh',
    },
  ];
  switch (state) {
    case 'pending':
      return [
        ...base,
        {
          step: 'PAYMENT_PENDING',
          at: PENDING_AT,
          note: 'Đã gửi nhắc nhở qua Zalo OA (1 lần). Sẽ gửi tiếp nếu còn 3 ngày.',
        },
      ];
    case 'paid':
      return [
        ...base,
        { step: 'PAYMENT_PENDING', at: PENDING_AT },
        {
          step: 'PAYMENT_RECEIVED',
          at: RECEIVED_AT,
          actor: 'VNPay xác nhận',
          amount: 1_500_000,
        },
        { step: 'CONFIRMED', at: CONFIRMED_AT, actor: 'Hệ thống tự động' },
        { step: 'COMPLETED', at: COMPLETED_AT, amount: 1_500_000 },
      ];
    case 'partial-paid':
      return [
        ...base,
        { step: 'PAYMENT_PENDING', at: PENDING_AT },
        {
          step: 'PAYMENT_RECEIVED',
          at: RECEIVED_AT,
          actor: 'VNPay xác nhận (đợt 1/2)',
          amount: 900_000,
          note: 'Phụ huynh trả 60% học phí kỳ này.',
        },
      ];
    case 'overdue':
      return [
        ...base,
        { step: 'PAYMENT_PENDING', at: PENDING_AT },
        {
          step: 'FAILED',
          at: FAILED_AT,
          note: 'Quá hạn 8 ngày · phí trễ 5% áp dụng.',
        },
      ];
    case 'refunded':
      return [
        ...base,
        { step: 'PAYMENT_PENDING', at: PENDING_AT },
        {
          step: 'PAYMENT_RECEIVED',
          at: RECEIVED_AT,
          actor: 'VNPay xác nhận',
          amount: 1_500_000,
        },
        { step: 'CONFIRMED', at: CONFIRMED_AT },
        { step: 'COMPLETED', at: COMPLETED_AT, amount: 1_500_000 },
        {
          step: 'REFUNDED',
          at: REFUND_AT,
          actor: 'Cô Lê Thị Hà (Quản lý lớp)',
          amount: 1_500_000,
          note: 'Lý do: Trùng lịch học thêm Tiếng Anh · Mã: REF-2026-1018-B7E2D9F1A4C8',
        },
      ];
    /* istanbul ignore next */
    default: {
      const _exhaustive: never = state;
      throw new Error(`unhandled state ${_exhaustive as string}`);
    }
  }
}

function renderG10(state: PaymentTimelineState, totalAmount = 1_500_000) {
  render(
    <PaymentStatusTimeline
      invoiceNumber="KC-2026-10-0042"
      state={state}
      events={buildEvents(state)}
      totalAmount={totalAmount}
    />,
  );
}

describe('<PaymentStatusTimeline>', () => {
  it('renders pending state with warning pill + Chờ thanh toán + Zalo reminder', () => {
    renderG10('pending');

    // Top status pill — the "current state" chip.
    const pill = screen.getByRole('status', { name: /chờ thanh toán/i });
    expect(pill).toHaveTextContent('Chờ thanh toán');

    // Invoice number + total in header.
    expect(screen.getByText('KC-2026-10-0042')).toBeInTheDocument();
    expect(
      screen.getByTestId('payment-timeline-total'),
    ).toHaveTextContent('1.500.000đ');

    // Timeline contains the pending event with the Zalo reminder note.
    const timeline = screen.getByRole('list', {
      name: /timeline trạng thái thanh toán/i,
    });
    expect(within(timeline).getByText(/đã gửi nhắc nhở qua zalo oa/i)).toBeInTheDocument();
  });

  it('renders paid state with success pill + Đã thanh toán amount in event', () => {
    renderG10('paid');

    expect(
      screen.getByRole('status', { name: /đã thanh toán/i }),
    ).toBeInTheDocument();

    // The PAYMENT_RECEIVED step renders the formatted amount.
    const timeline = screen.getByRole('list', {
      name: /timeline trạng thái thanh toán/i,
    });
    // Amount appears at least once (PAYMENT_RECEIVED + COMPLETED both have it).
    expect(within(timeline).getAllByText(/1\.500\.000đ/).length).toBeGreaterThanOrEqual(1);
  });

  it('renders partial-paid state with info pill + amount paid so far', () => {
    renderG10('partial-paid');

    expect(
      screen.getByRole('status', { name: /trả góp|một phần/i }),
    ).toBeInTheDocument();

    const timeline = screen.getByRole('list', {
      name: /timeline trạng thái thanh toán/i,
    });
    expect(within(timeline).getByText(/đợt 1\/2/i)).toBeInTheDocument();
    expect(within(timeline).getByText('900.000đ')).toBeInTheDocument();
  });

  it('renders overdue state with destructive role=alert banner + Quá hạn pill', () => {
    renderG10('overdue');

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(
      screen.getByRole('status', { name: /quá hạn/i }),
    ).toBeInTheDocument();

    const timeline = screen.getByRole('list', {
      name: /timeline trạng thái thanh toán/i,
    });
    expect(within(timeline).getByText(/quá hạn 8 ngày/i)).toBeInTheDocument();
  });

  it('renders refunded state with warning pill + refund event with approver', () => {
    renderG10('refunded');

    expect(
      screen.getByRole('status', { name: /hoàn tiền/i }),
    ).toBeInTheDocument();

    const timeline = screen.getByRole('list', {
      name: /timeline trạng thái thanh toán/i,
    });
    expect(within(timeline).getByText(/cô lê thị hà/i)).toBeInTheDocument();
    // Refund mã giao dịch is shown in the note.
    expect(within(timeline).getByText(/REF-2026-1018-B7E2D9F1A4C8/)).toBeInTheDocument();
  });

  it('orders timeline events by canonical step lifecycle (CREATED first, REFUNDED last)', () => {
    renderG10('refunded');

    const items = screen.getAllByTestId(/^payment-timeline-event-/);
    // First event is CREATED; last is REFUNDED.
    expect(items[0]).toHaveAttribute('data-testid', 'payment-timeline-event-CREATED');
    expect(items[items.length - 1]).toHaveAttribute(
      'data-testid',
      'payment-timeline-event-REFUNDED',
    );
    // Sequence respects canonical order.
    const sequence = items.map((el) => el.getAttribute('data-testid'));
    expect(sequence).toEqual([
      'payment-timeline-event-CREATED',
      'payment-timeline-event-PAYMENT_PENDING',
      'payment-timeline-event-PAYMENT_RECEIVED',
      'payment-timeline-event-CONFIRMED',
      'payment-timeline-event-COMPLETED',
      'payment-timeline-event-REFUNDED',
    ]);
  });

  it('marks current step with aria-current="step" and others without it', () => {
    renderG10('pending');

    const current = screen.getByTestId('payment-timeline-event-PAYMENT_PENDING');
    expect(current.querySelector('[aria-current="step"]')).not.toBeNull();

    const past = screen.getByTestId('payment-timeline-event-CREATED');
    expect(past.querySelector('[aria-current="step"]')).toBeNull();
  });

  it('formats event timestamps as dd/MM/yyyy HH:mm (Vietnamese convention)', () => {
    renderG10('paid');

    const issuedRow = screen.getByTestId('payment-timeline-event-CREATED');
    // ISSUED_AT = 2026-10-01T09:15:00Z → "01/10/2026 09:15"
    expect(within(issuedRow).getByText(/01\/10\/2026 09:15/)).toBeInTheDocument();

    const receivedRow = screen.getByTestId('payment-timeline-event-PAYMENT_RECEIVED');
    // RECEIVED_AT = 2026-10-14T19:42:00Z → "14/10/2026 19:42"
    expect(within(receivedRow).getByText(/14\/10\/2026 19:42/)).toBeInTheDocument();
  });

  it('cross-component formatVNCurrency re-use: G10 amounts match G6 helper output', () => {
    // Sanity: the helper imported from G6 is itself functional.
    expect(g6Direct(1_500_000)).toBe('1.500.000đ');
    expect(g6Direct(0)).toBe('0đ');

    // Identity: the helper re-exported under G10 (PaymentStatusTimeline module)
    // must be the SAME function — proves the module-internal re-use works
    // and there's no copy-paste re-implementation drift.
    expect(g10ReExport).toBe(g6Direct);

    // Render proof: total in G10 panel uses the format produced by G6 helper.
    renderG10('paid', 1_500_000);
    const total = screen.getByTestId('payment-timeline-total');
    expect(total).toHaveTextContent(g6Direct(1_500_000));
  });

  it('uses lang="vi" by default on the wrapper', () => {
    renderG10('pending');
    const root = screen.getByTestId('payment-timeline-root');
    expect(root).toHaveAttribute('lang', 'vi');
  });

  it('standalone mode (default) keeps page-chrome (bg-muted/30 + min-h-full) on root', () => {
    renderG10('pending');
    const root = screen.getByTestId('payment-timeline-root');
    expect(root.className).toContain('bg-muted/30');
    expect(root.className).toContain('min-h-full');
  });

  it('embedded mode drops standalone page-chrome (bg-muted/30 + min-h-full + max-w-3xl)', () => {
    render(
      <PaymentStatusTimeline
        invoiceNumber="KC-2026-10-0042"
        state="pending"
        events={buildEvents('pending')}
        totalAmount={1_500_000}
        embedded
      />,
    );
    const root = screen.getByTestId('payment-timeline-root');
    expect(root.className).not.toContain('bg-muted/30');
    expect(root.className).not.toContain('min-h-full');

    // <main> must fill full width — no centering / max-width island.
    const main = root.querySelector('main');
    expect(main).not.toBeNull();
    expect(main?.className).not.toContain('max-w-3xl');
    expect(main?.className).not.toContain('mx-auto');
  });
});
