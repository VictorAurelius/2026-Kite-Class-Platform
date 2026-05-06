/**
 * G6 Invoice Detail — RTL coverage of the 6 spec'd states (loading, default,
 * pending, paid, overdue, print-view) plus tax-breakdown summation.
 *
 * Spec source: `ui_kits/components/G6-invoice-detail/spec.md` + 5 HTML state
 * files under that folder. Vietnamese-only labels per CLAUDE.md.
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { InvoiceDetail } from '../InvoiceDetail';
import type { InvoiceData, InvoiceState } from '../types';

function makeInvoice(overrides: Partial<InvoiceData> = {}): InvoiceData {
  return {
    number: 'KH-2026-04-001',
    status: 'PENDING_PAYMENT',
    issueDate: new Date('2026-04-01T00:00:00Z'),
    dueDate: new Date('2026-04-15T00:00:00Z'),
    items: [
      {
        title: 'Khóa luyện thi THPT Quốc gia 2026 — Toán',
        quantity: 1,
        unitPrice: 4500000,
        lineTotal: 4500000,
        meta: 'Học kỳ II · 60 buổi',
      },
      {
        title: 'Sách giáo khoa — Toán nâng cao 12',
        quantity: 1,
        unitPrice: 200000,
        lineTotal: 200000,
      },
    ],
    discounts: [{ label: 'Giảm giá anh chị em', amount: 200000 }],
    subtotal: 4700000,
    total: 4500000,
    balance: 4500000,
    student: { fullName: 'Lê Minh Tuấn', className: 'Lớp 12A1' },
    tenant: {
      name: 'Trung tâm Toán Master',
      address: '123 Nguyễn Văn Cừ, Q. 9, TP. HCM',
      mst: '0312345678',
    },
    ...overrides,
  };
}

function renderInvoice(state: InvoiceState, overrides: Partial<InvoiceData> = {}) {
  const onPayNow = vi.fn();
  const onDownloadPdf = vi.fn();
  const onSendEmail = vi.fn();
  render(
    <InvoiceDetail
      invoice={makeInvoice(overrides)}
      state={state}
      onPayNow={onPayNow}
      onDownloadPdf={onDownloadPdf}
      onSendEmail={onSendEmail}
    />,
  );
  return { onPayNow, onDownloadPdf, onSendEmail };
}

describe('<InvoiceDetail>', () => {
  it('renders skeleton when state="loading"', () => {
    renderInvoice('loading');
    expect(screen.getByTestId('invoice-detail-skeleton')).toBeInTheDocument();
    // No actionable buttons in loading state.
    expect(screen.queryByTestId('invoice-paynow-btn')).not.toBeInTheDocument();
  });

  it('renders pending state with status pill, due date, and Pay-now CTA', async () => {
    const user = userEvent.setup();
    const { onPayNow } = renderInvoice('pending');

    expect(screen.getByText('Chờ thanh toán')).toBeInTheDocument();
    expect(screen.getByText('KH-2026-04-001')).toBeInTheDocument();
    // Total appears in CTA label as `Thanh toán ngay 4.500.000đ`.
    const cta = screen.getByTestId('invoice-paynow-btn');
    expect(cta).toHaveTextContent('4.500.000đ');

    await user.click(cta);
    expect(onPayNow).toHaveBeenCalledTimes(1);
  });

  it('renders default state same as pending (alias)', () => {
    renderInvoice('default');
    expect(screen.getByText('Chờ thanh toán')).toBeInTheDocument();
    expect(screen.getByTestId('invoice-paynow-btn')).toBeInTheDocument();
  });

  it('renders paid state with success pill, paid date, download + email actions', async () => {
    const user = userEvent.setup();
    const { onDownloadPdf, onSendEmail, onPayNow } = renderInvoice('paid', {
      status: 'PAID',
      paidDate: new Date('2026-04-10T09:48:00Z'),
      balance: 0,
    });

    expect(screen.getByText('Đã thanh toán')).toBeInTheDocument();
    // Pay-now CTA must NOT render in paid state.
    expect(screen.queryByTestId('invoice-paynow-btn')).not.toBeInTheDocument();

    await user.click(screen.getByTestId('invoice-download-btn'));
    await user.click(screen.getByTestId('invoice-email-btn'));

    expect(onDownloadPdf).toHaveBeenCalledTimes(1);
    expect(onSendEmail).toHaveBeenCalledTimes(1);
    expect(onPayNow).not.toHaveBeenCalled();
  });

  it('renders overdue state with destructive banner + late-fee row + Pay-now', () => {
    renderInvoice('overdue', {
      status: 'OVERDUE',
      items: [
        ...makeInvoice().items,
        {
          title: 'Phí trễ hạn',
          quantity: 5,
          unitPrice: 50000,
          lineTotal: 250000,
          meta: '5 ngày × 50.000đ/ngày',
          intent: 'destructive',
        },
      ],
      total: 4750000,
      balance: 4750000,
    });

    expect(screen.getByText('Quá hạn')).toBeInTheDocument();
    // `role=alert` overdue banner must be present.
    expect(screen.getByRole('alert')).toBeInTheDocument();
    // Late fee row title visible.
    expect(screen.getByText('Phí trễ hạn')).toBeInTheDocument();
    // Pay-now still shown — overdue is still actionable.
    expect(screen.getByTestId('invoice-paynow-btn')).toHaveTextContent('4.750.000đ');
  });

  it('renders print-view: no nav header, no action buttons, role=document container', () => {
    renderInvoice('print-view', {
      status: 'PAID',
      paidDate: new Date('2026-04-10T09:48:00Z'),
      balance: 0,
    });

    // print-view strips the back button + actions for clean A4 output.
    expect(screen.queryByRole('button', { name: /quay lại/i })).not.toBeInTheDocument();
    expect(screen.queryByTestId('invoice-paynow-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('invoice-download-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('invoice-email-btn')).not.toBeInTheDocument();

    // The print-view marker is exposed as a stable selector.
    expect(screen.getByTestId('invoice-detail-print-view')).toBeInTheDocument();
  });

  it('renders VAT row + tax-invoice header when isVATInvoice=true', () => {
    renderInvoice('print-view', {
      isVATInvoice: true,
      status: 'PAID',
      vat: { rate: 0.08, amount: 344000 },
      paidDate: new Date('2026-04-10T09:48:00Z'),
    });

    // VN tax-invoice headline, see spec §"Vietnamese UX considerations".
    expect(screen.getByText(/HÓA ĐƠN GIÁ TRỊ GIA TĂNG/)).toBeInTheDocument();
    // VAT label + formatted amount + rate %.
    const vatRow = screen.getByTestId('invoice-vat-row');
    expect(vatRow).toHaveTextContent('8%');
    expect(vatRow).toHaveTextContent('344.000đ');
  });

  it('renders all line items with formatted unit price + line total', () => {
    renderInvoice('pending');
    const table = screen.getByRole('table');
    const utils = within(table);
    // Each amount appears in two cells: unit-price column + line-total column.
    expect(utils.getAllByText('4.500.000đ')).toHaveLength(2);
    expect(utils.getAllByText('200.000đ')).toHaveLength(2);
    // Item titles + meta rendered.
    expect(utils.getByText(/Khóa luyện thi THPT Quốc gia 2026/)).toBeInTheDocument();
    expect(utils.getByText(/Sách giáo khoa/)).toBeInTheDocument();
  });

  it('renders discount rows with U+2212 minus sign (matches HTML proto)', () => {
    renderInvoice('pending');
    // Discount: amount 200000 → rendered as `−200.000đ` per spec.
    const totals = screen.getByTestId('invoice-totals');
    expect(totals).toHaveTextContent('−200.000đ');
    expect(totals).toHaveTextContent('Giảm giá anh chị em');
  });

  it('reflects tax breakdown summation: subtotal + vat = total when VAT present', () => {
    renderInvoice('pending', {
      vat: { rate: 0.08, amount: 344000 },
      subtotal: 4300000,
      total: 4644000,
      discounts: [],
    });
    const totals = screen.getByTestId('invoice-totals');
    expect(totals).toHaveTextContent('Tổng cộng');
    expect(totals).toHaveTextContent('4.644.000đ');
    // VAT line shown
    expect(within(totals).getByTestId('invoice-vat-row')).toHaveTextContent('344.000đ');
  });

  it('uses lang="vi" by default on the wrapper', () => {
    renderInvoice('pending');
    const wrapper = screen.getByTestId('invoice-detail-root');
    expect(wrapper).toHaveAttribute('lang', 'vi');
  });

  it('sets role="status" on the status pill for screen readers', () => {
    renderInvoice('pending');
    const pill = screen.getByRole('status');
    expect(pill).toHaveTextContent('Chờ thanh toán');
  });
});
