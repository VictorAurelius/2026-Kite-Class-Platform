/**
 * Tests for AttendanceFormList empty states (GAP-1474 Part B).
 *
 * Verifies the roster is no longer silently empty when students exist but are
 * PENDING_PAYMENT (excluded from attendance per BR-ATTEND-001): the empty-state
 * explains the reason and the pending count, instead of "no students".
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import { AttendanceFormList } from '../attendance-form-list';

describe('AttendanceFormList empty states (GAP-1474)', () => {
  const noop = vi.fn();

  it('explains the empty roster when students are awaiting payment', () => {
    render(
      <AttendanceFormList
        rows={[]}
        onStatusChange={noop}
        onNotesChange={noop}
        pendingPaymentCount={3}
      />
    );

    expect(
      screen.getByText('Chưa có học sinh nào đã kích hoạt để điểm danh')
    ).toBeInTheDocument();

    const explanation = screen.getByText(/đang chờ xác nhận thanh toán/i);
    expect(explanation).toHaveTextContent(
      '3 học sinh đang chờ xác nhận thanh toán'
    );

    // The misleading silent message must NOT be shown in this case.
    expect(
      screen.queryByText('Không có học viên nào trong lớp này')
    ).not.toBeInTheDocument();
  });

  it('shows the generic empty state when there are no pending enrollments', () => {
    render(
      <AttendanceFormList
        rows={[]}
        onStatusChange={noop}
        onNotesChange={noop}
        pendingPaymentCount={0}
      />
    );

    expect(
      screen.getByText('Không có học viên nào trong lớp này')
    ).toBeInTheDocument();
    expect(
      screen.queryByText('Chưa có học sinh nào đã kích hoạt để điểm danh')
    ).not.toBeInTheDocument();
  });

  it('defaults to the generic empty state when pendingPaymentCount is omitted', () => {
    render(
      <AttendanceFormList rows={[]} onStatusChange={noop} onNotesChange={noop} />
    );

    expect(
      screen.getByText('Không có học viên nào trong lớp này')
    ).toBeInTheDocument();
  });
});
