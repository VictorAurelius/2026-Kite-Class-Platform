/**
 * BatchInvoiceDrawer tests — GAP-297 (Wave p0-ux-1 Bucket D).
 *
 * Verify: preview render (count + revenue + line items), empty state, error state
 * surfaces backend reason (not generic), confirm calls batch-confirm + closes.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@/test/utils';
import { BatchInvoiceDrawer } from '@/components/billing/batch-invoice-drawer';

vi.mock('@/lib/api/invoices', () => ({
  invoicesApi: {
    batchGenerate: vi.fn(),
    batchConfirm: vi.fn(),
  },
}));

import { invoicesApi } from '@/lib/api/invoices';

const samplePreview = {
  month: '2026-05',
  invoiceCount: 2,
  totalRevenue: 2500000,
  invoices: [
    {
      enrollmentId: 1,
      studentId: 101,
      classId: 11,
      classNameVi: 'Lớp Toán 6A',
      tuitionAmount: 1500000,
      discountPercent: 0,
      proratedTuition: 1500000,
      discountAmount: 0,
      total: 1500000,
      prorated: false,
      billableDays: 31,
      daysInMonth: 31,
    },
    {
      enrollmentId: 2,
      studentId: 102,
      classId: 12,
      classNameVi: 'Lớp Anh ngữ 5A1',
      tuitionAmount: 1000000,
      discountPercent: 0,
      proratedTuition: 1000000,
      discountAmount: 0,
      total: 1000000,
      prorated: false,
      billableDays: 31,
      daysInMonth: 31,
    },
  ],
};

describe('BatchInvoiceDrawer (GAP-297)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('auto-previews on open and renders count + revenue + line items', async () => {
    vi.mocked(invoicesApi.batchGenerate).mockResolvedValue(samplePreview);

    render(<BatchInvoiceDrawer open onOpenChange={vi.fn()} />);

    expect(
      screen.getByRole('heading', { name: 'Tạo hóa đơn tháng' }),
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId('batch-summary')).toHaveTextContent('Số hóa đơn');
    });

    const lineItems = screen.getByTestId('batch-line-items');
    expect(lineItems).toHaveTextContent('Lớp Toán 6A');
    expect(lineItems).toHaveTextContent('Lớp Anh ngữ 5A1');
    expect(invoicesApi.batchGenerate).toHaveBeenCalled();
  });

  it('shows friendly empty state when 0 active enrollments', async () => {
    vi.mocked(invoicesApi.batchGenerate).mockResolvedValue({
      month: '2026-05',
      invoiceCount: 0,
      totalRevenue: 0,
      invoices: [],
    });

    render(<BatchInvoiceDrawer open onOpenChange={vi.fn()} />);

    await waitFor(() => {
      expect(
        screen.getByText(/Không có học viên nào đang học/),
      ).toBeInTheDocument();
    });
    // Confirm disabled when empty
    expect(
      screen.getByRole('button', { name: /Xác nhận tạo/ }),
    ).toBeDisabled();
  });

  it('surfaces the actual backend error reason (not a generic message)', async () => {
    vi.mocked(invoicesApi.batchGenerate).mockRejectedValue({
      response: {
        data: { error: { code: 'INVALID_MONTH_FORMAT', message: 'Tháng không hợp lệ' } },
      },
    });

    render(<BatchInvoiceDrawer open onOpenChange={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByText('Tháng không hợp lệ')).toBeInTheDocument();
    });
  });

  it('confirms batch → calls batch-confirm and closes drawer', async () => {
    vi.mocked(invoicesApi.batchGenerate).mockResolvedValue(samplePreview);
    vi.mocked(invoicesApi.batchConfirm).mockResolvedValue({
      month: '2026-05',
      createdCount: 2,
      skippedCount: 0,
      totalRevenue: 2500000,
      createdInvoiceIds: [101, 102],
    });
    const onOpenChange = vi.fn();

    render(<BatchInvoiceDrawer open onOpenChange={onOpenChange} />);

    const confirmBtn = await screen.findByRole('button', {
      name: /Xác nhận tạo/,
    });
    await waitFor(() => expect(confirmBtn).not.toBeDisabled());

    fireEvent.click(confirmBtn);

    const expectedMonth = new Date().toISOString().slice(0, 7);
    await waitFor(() => {
      expect(invoicesApi.batchConfirm).toHaveBeenCalledWith(expectedMonth);
    });
    await waitFor(() => {
      expect(onOpenChange).toHaveBeenCalledWith(false);
    });
  });
});
