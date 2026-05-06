'use client';

/**
 * G6 — Invoice Detail Panel.
 *
 * Replaces production KH `/billing/payment/[id]` (33/128 baseline) per
 * `dossier/04-component-gaps.md` §G6 + `ui_kits/components/G6-invoice-detail/spec.md`.
 *
 * State machine:
 *   loading → default | pending | paid | overdue | print-view
 *   ('default' is an alias for 'pending' for ergonomics — both render the
 *    pending-payment view; the spec lists both.)
 *
 * Print-friendly: no fixed pixel widths in the main layout; uses
 * Tailwind utility classes that resolve to `rem` / `%` / responsive breakpoints.
 * `@media print` rules in `print.css` (loaded by host app) hide nav + actions
 * and switch to A4 portrait — the component itself just exposes a stable
 * `data-testid="invoice-detail-print-view"` marker so host CSS / Playwright
 * can target it.
 *
 * Accessibility (WCAG AA):
 *  - Status pill uses `role="status"` so screen readers announce status changes.
 *  - Line-items rendered as a `<table>` with `<th scope="col">` headers.
 *  - Currency cells right-aligned + `tabular-nums` for visual scan.
 *  - Overdue banner uses `role="alert"`.
 *  - Contrast ratios are documented in the kit HTML protos
 *    (Body on bg-card 17.9:1 AAA, Status pills ≥4.7:1 AA — see proto comments).
 *
 * No new deps — relies on Tailwind utility classes already shipped with the
 * shared-ui package consumer's app theme.
 */

import type React from 'react';
import { useMemo } from 'react';
import type { InvoiceData, InvoiceDetailProps, InvoiceStatus } from './types';
import { formatVNCurrency, formatVNTax } from './utils';

const COPY = {
  heading: 'Chi tiết hóa đơn',
  invoiceLabel: 'Hóa đơn',
  issueDate: 'Ngày phát hành',
  dueDate: 'Hạn thanh toán',
  paidDate: 'Đã thanh toán',
  issuer: 'Bên phát hành',
  student: 'Học sinh',
  mst: 'MST',
  itemsHeading: 'Khoản mục thanh toán',
  colDescription: 'Mô tả',
  colQuantity: 'SL',
  colUnitPrice: 'Đơn giá',
  colLineTotal: 'Thành tiền',
  subtotal: 'Tạm tính',
  vat: 'Thuế GTGT',
  total: 'Tổng cộng',
  paid: 'Đã thanh toán',
  payNow: 'Thanh toán ngay',
  downloadPdf: 'Tải PDF',
  downloadReceipt: 'Tải biên lai',
  sendEmail: 'Gửi qua email',
  back: 'Quay lại',
  vatInvoiceHeader: 'HÓA ĐƠN GIÁ TRỊ GIA TĂNG',
  vatTemplateLabel: 'Mẫu số',
  vatSerialLabel: 'Ký hiệu',
  vatTemplateValue: '01GTKT0/001',
  vatSerialValue: 'KH/26E',
  status: {
    PENDING_PAYMENT: 'Chờ thanh toán',
    PARTIAL_PAID: 'Trả góp',
    PAID: 'Đã thanh toán',
    OVERDUE: 'Quá hạn',
    VOID: 'Đã hủy',
  } as const satisfies Record<InvoiceStatus, string>,
  overdueBannerTitle: 'Hóa đơn quá hạn',
  overdueBannerHint:
    'Vui lòng thanh toán sớm để tránh phát sinh thêm phí và đảm bảo quyền học của con.',
};

function formatVNDate(date: Date | undefined): string {
  if (!date) return '';
  // Use UTC accessors so test fixtures + production both emit dd/MM/yyyy
  // regardless of the running machine's timezone.
  const dd = String(date.getUTCDate()).padStart(2, '0');
  const mm = String(date.getUTCMonth() + 1).padStart(2, '0');
  const yyyy = date.getUTCFullYear();
  return `${dd}/${mm}/${yyyy}`;
}

function statusPillClasses(status: InvoiceStatus): string {
  // Tokens: success / warning / destructive / muted from the shared theme.
  switch (status) {
    case 'PAID':
      return 'bg-success/10 text-[hsl(var(--success))]';
    case 'OVERDUE':
      return 'bg-destructive/10 text-destructive';
    case 'VOID':
      return 'bg-muted text-muted-foreground';
    case 'PARTIAL_PAID':
      return 'bg-info/10 text-[hsl(var(--info))]';
    case 'PENDING_PAYMENT':
    default:
      return 'bg-warning/10 text-[hsl(var(--warning))]';
  }
}

export function InvoiceDetail(props: InvoiceDetailProps): React.JSX.Element {
  const { invoice, state, onPayNow, onDownloadPdf, onSendEmail, lang = 'vi' } = props;

  const isLoading = state === 'loading';
  const isPrint = state === 'print-view';
  const isPaid = state === 'paid' || invoice.status === 'PAID';
  const isOverdue = state === 'overdue' || invoice.status === 'OVERDUE';
  // 'default' aliases 'pending' (both show pending-payment treatment).
  const isPending = state === 'pending' || state === 'default';

  // Status used for pill rendering — derive from `state` first so 'paid' state
  // wins even if caller forgets to update `invoice.status`.
  const renderedStatus: InvoiceStatus = useMemo(() => {
    if (state === 'paid') return 'PAID';
    if (state === 'overdue') return 'OVERDUE';
    return invoice.status;
  }, [state, invoice.status]);

  if (isLoading) {
    return (
      <div
        data-testid="invoice-detail-skeleton"
        lang={lang}
        className="mx-auto w-full max-w-4xl space-y-4 px-4 py-6"
      >
        <div className="h-24 animate-pulse rounded-2xl border bg-muted/40" />
        <div className="space-y-2 rounded-2xl border bg-card p-6 shadow-soft">
          <div className="h-4 w-1/3 animate-pulse rounded bg-muted/60" />
          <div className="h-4 w-1/2 animate-pulse rounded bg-muted/60" />
          <div className="h-4 w-1/4 animate-pulse rounded bg-muted/60" />
        </div>
        <div className="h-12 animate-pulse rounded-xl bg-muted/60" />
      </div>
    );
  }

  const showActions = !isPrint;
  const showPayNow = showActions && (isPending || isOverdue) && !isPaid;
  const showPaidActions = showActions && isPaid;

  return (
    <div
      data-testid="invoice-detail-root"
      lang={lang}
      data-print-view={isPrint ? 'true' : undefined}
      className="min-h-screen bg-muted/30 text-foreground"
    >
      {!isPrint && (
        <header className="border-b bg-card">
          <div className="mx-auto flex max-w-4xl items-center gap-3 px-4 py-3">
            <button
              type="button"
              aria-label={COPY.back}
              className="rounded-lg p-2 hover:bg-muted focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
            >
              <span aria-hidden="true">←</span>
            </button>
            <h1 className="text-base font-semibold md:text-lg">{COPY.heading}</h1>
          </div>
        </header>
      )}

      <main
        data-testid={isPrint ? 'invoice-detail-print-view' : undefined}
        className="mx-auto w-full max-w-4xl space-y-5 px-4 py-6 sm:px-6"
      >
        {invoice.isVATInvoice && (
          <section
            data-testid="invoice-vat-tax-header"
            className="rounded-xl border bg-card p-5 text-center shadow-soft"
          >
            <p className="text-lg font-bold uppercase text-destructive sm:text-xl">
              {COPY.vatInvoiceHeader}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              {COPY.vatTemplateLabel}: {COPY.vatTemplateValue} · {COPY.vatSerialLabel}:{' '}
              {COPY.vatSerialValue}
            </p>
          </section>
        )}

        {isOverdue && !isPrint && <OverdueBanner invoice={invoice} />}

        <InvoiceHeaderSection invoice={invoice} status={renderedStatus} />

        <LineItemsSection invoice={invoice} />

        {!isPrint && (
          <ActionsSection
            showPayNow={showPayNow}
            showPaidActions={showPaidActions}
            isOverdue={isOverdue}
            invoice={invoice}
            onPayNow={onPayNow}
            onDownloadPdf={onDownloadPdf}
            onSendEmail={onSendEmail}
          />
        )}
      </main>
    </div>
  );
}

function OverdueBanner({ invoice }: { invoice: InvoiceData }): React.JSX.Element {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 rounded-xl border-2 border-destructive/30 bg-destructive/5 p-4"
    >
      <span aria-hidden="true" className="mt-0.5 text-destructive">
        ⚠
      </span>
      <div className="flex-1 text-sm">
        <p>
          <strong className="text-destructive">{COPY.overdueBannerTitle}.</strong>{' '}
          {COPY.subtotal}: {formatVNCurrency(invoice.subtotal)}.
        </p>
        <p className="mt-0.5 text-xs text-muted-foreground">{COPY.overdueBannerHint}</p>
      </div>
    </div>
  );
}

function InvoiceHeaderSection({
  invoice,
  status,
}: {
  invoice: InvoiceData;
  status: InvoiceStatus;
}): React.JSX.Element {
  return (
    <section className="rounded-2xl border bg-card p-6 shadow-soft">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            {COPY.invoiceLabel}
          </p>
          <p className="mt-1 font-mono text-2xl font-bold md:text-3xl">{invoice.number}</p>
          <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1.5 text-sm text-muted-foreground">
            <span>
              {COPY.issueDate}:{' '}
              <strong className="text-foreground">{formatVNDate(invoice.issueDate)}</strong>
            </span>
            {invoice.paidDate ? (
              <span className="text-[hsl(var(--success))]">
                {COPY.paidDate}:{' '}
                <strong>{formatVNDate(invoice.paidDate)}</strong>
              </span>
            ) : (
              <span>
                {COPY.dueDate}:{' '}
                <strong className="text-foreground">{formatVNDate(invoice.dueDate)}</strong>
              </span>
            )}
          </div>
        </div>
        <span
          role="status"
          className={`inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-semibold ${statusPillClasses(status)}`}
        >
          <span>{COPY.status[status]}</span>
        </span>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <div className="rounded-lg border bg-muted/30 p-4">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            {COPY.issuer}
          </p>
          <p className="font-semibold">{invoice.tenant.name}</p>
          <p className="mt-0.5 text-sm text-muted-foreground">{invoice.tenant.address}</p>
          {invoice.tenant.mst && (
            <p className="mt-1 text-xs text-muted-foreground">
              {COPY.mst}: <span className="font-mono">{invoice.tenant.mst}</span>
            </p>
          )}
        </div>
        <div className="rounded-lg border bg-muted/30 p-4">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            {COPY.student}
          </p>
          <p className="font-semibold">{invoice.student.fullName}</p>
          <p className="mt-0.5 text-sm text-muted-foreground">{invoice.student.className}</p>
        </div>
      </div>
    </section>
  );
}

function LineItemsSection({ invoice }: { invoice: InvoiceData }): React.JSX.Element {
  return (
    <section className="overflow-hidden rounded-2xl border bg-card shadow-soft">
      <div className="border-b px-6 py-4">
        <h2 className="font-semibold">{COPY.itemsHeading}</h2>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-muted/40">
            <tr>
              <th scope="col" className="px-6 py-2.5 text-left font-semibold">
                {COPY.colDescription}
              </th>
              <th scope="col" className="w-16 px-2 py-2.5 text-right font-semibold">
                {COPY.colQuantity}
              </th>
              <th scope="col" className="w-32 px-2 py-2.5 text-right font-semibold">
                {COPY.colUnitPrice}
              </th>
              <th scope="col" className="w-32 px-6 py-2.5 text-right font-semibold">
                {COPY.colLineTotal}
              </th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {invoice.items.map((item, idx) => {
              const destructive = item.intent === 'destructive';
              return (
                <tr
                  key={`${item.title}-${idx}`}
                  className={destructive ? 'bg-destructive/5' : undefined}
                >
                  <td className="px-6 py-3">
                    <p
                      className={`font-medium ${destructive ? 'text-destructive' : ''}`}
                    >
                      {item.title}
                    </p>
                    {item.meta && (
                      <p className="mt-0.5 text-xs text-muted-foreground">{item.meta}</p>
                    )}
                  </td>
                  <td className="px-2 py-3 text-right tabular-nums">{item.quantity}</td>
                  <td className="px-2 py-3 text-right font-mono tabular-nums">
                    {formatVNCurrency(item.unitPrice)}
                  </td>
                  <td
                    className={`px-6 py-3 text-right font-mono font-semibold tabular-nums ${
                      destructive ? 'text-destructive' : ''
                    }`}
                  >
                    {formatVNCurrency(item.lineTotal)}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div data-testid="invoice-totals" className="border-t bg-muted/30 px-6 py-4">
        <dl className="ml-auto max-w-sm space-y-1.5 text-sm">
          <div className="flex justify-between">
            <dt className="text-muted-foreground">{COPY.subtotal}</dt>
            <dd className="font-mono tabular-nums">
              {formatVNCurrency(invoice.subtotal)}
            </dd>
          </div>
          {invoice.discounts.map((discount) => (
            <div
              key={discount.label}
              className="flex justify-between text-[hsl(var(--success))]"
            >
              <dt>{discount.label}</dt>
              <dd className="font-mono tabular-nums">
                {formatVNCurrency(-Math.abs(discount.amount))}
              </dd>
            </div>
          ))}
          {invoice.vat && (
            <div data-testid="invoice-vat-row" className="flex justify-between">
              <dt className="text-muted-foreground">
                {COPY.vat} ({formatVNTax(invoice.vat.rate)})
              </dt>
              <dd className="font-mono tabular-nums">
                {formatVNCurrency(invoice.vat.amount)}
              </dd>
            </div>
          )}
          <div className="mt-2 flex justify-between border-t pt-2 text-base">
            <dt className="font-semibold">{COPY.total}</dt>
            <dd className="font-mono text-lg font-bold tabular-nums">
              {formatVNCurrency(invoice.total)}
            </dd>
          </div>
        </dl>
      </div>
    </section>
  );
}

function ActionsSection({
  showPayNow,
  showPaidActions,
  isOverdue,
  invoice,
  onPayNow,
  onDownloadPdf,
  onSendEmail,
}: {
  showPayNow: boolean;
  showPaidActions: boolean;
  isOverdue: boolean;
  invoice: InvoiceData;
  onPayNow?: () => void;
  onDownloadPdf?: () => void;
  onSendEmail?: () => void;
}): React.JSX.Element | null {
  if (!showPayNow && !showPaidActions) return null;
  return (
    <div className="flex flex-col gap-2 sm:flex-row">
      {showPayNow && (
        <button
          type="button"
          data-testid="invoice-paynow-btn"
          onClick={onPayNow}
          className={`inline-flex flex-1 items-center justify-center gap-2 rounded-xl px-5 py-3 text-base font-semibold shadow-soft hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 ${
            isOverdue
              ? 'bg-destructive text-destructive-foreground'
              : 'bg-primary text-primary-foreground'
          }`}
        >
          <span>
            {COPY.payNow} {formatVNCurrency(invoice.total)}
          </span>
        </button>
      )}
      {showPaidActions && (
        <>
          <button
            type="button"
            data-testid="invoice-download-btn"
            onClick={onDownloadPdf}
            className="inline-flex flex-1 items-center justify-center gap-2 rounded-xl bg-primary px-5 py-3 text-base font-semibold text-primary-foreground shadow-soft hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          >
            <span>{COPY.downloadReceipt}</span>
          </button>
          <button
            type="button"
            data-testid="invoice-email-btn"
            onClick={onSendEmail}
            className="inline-flex items-center justify-center gap-2 rounded-xl border bg-card px-4 py-3 text-sm font-medium hover:bg-muted focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          >
            <span>{COPY.sendEmail}</span>
          </button>
        </>
      )}
      {showPayNow && !showPaidActions && (
        <>
          <button
            type="button"
            data-testid="invoice-download-btn"
            onClick={onDownloadPdf}
            className="inline-flex items-center justify-center gap-2 rounded-xl border bg-card px-4 py-3 text-sm font-medium hover:bg-muted focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          >
            <span>{COPY.downloadPdf}</span>
          </button>
          <button
            type="button"
            data-testid="invoice-email-btn"
            onClick={onSendEmail}
            className="inline-flex items-center justify-center gap-2 rounded-xl border bg-card px-4 py-3 text-sm font-medium hover:bg-muted focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
          >
            <span>{COPY.sendEmail}</span>
          </button>
        </>
      )}
    </div>
  );
}

export default InvoiceDetail;
