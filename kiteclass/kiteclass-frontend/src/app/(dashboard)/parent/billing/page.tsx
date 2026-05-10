/**
 * Parent billing list — outstanding banner + paid history.
 *
 * Wave 49 Bucket A (GAP-267). Ports `billing-list.html` from
 * `ui_kits/kiteclass-parent/screens/`. Click an invoice to drill in to
 * `/parent/billing/[invoiceId]/pay`.
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { AlertTriangle, ChevronRight, Receipt } from 'lucide-react';
import { ParentShell } from '@/components/parent/parent-shell';
import {
  MOCK_INVOICES,
  formatVN,
  type MockInvoice,
} from '@/components/parent/parent-mock-data';

const STATUS_BADGE: Record<MockInvoice['status'], { label: string; className: string }> = {
  PENDING_PAYMENT: {
    label: 'Chưa đóng',
    className: 'bg-amber-100 text-amber-700',
  },
  OVERDUE: {
    label: 'Quá hạn',
    className: 'bg-red-100 text-red-700',
  },
  PAID: {
    label: 'Đã đóng',
    className: 'bg-emerald-100 text-emerald-700',
  },
};

export default function ParentBillingListPage() {
  const outstanding = MOCK_INVOICES.filter((i) => i.status !== 'PAID');
  const paid = MOCK_INVOICES.filter((i) => i.status === 'PAID');
  const totalOutstanding = outstanding.reduce((s, i) => s + i.amount, 0);

  return (
    <ParentShell title="Học phí" subtitle="Danh sách hóa đơn">
      {totalOutstanding > 0 && (
        <section
          className="mx-4 mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4"
          role="region"
          aria-label="Học phí chưa đóng"
          data-testid="parent-billing-outstanding-banner"
        >
          <div className="flex items-start gap-3">
            <AlertTriangle
              className="h-5 w-5 flex-shrink-0 text-amber-600"
              aria-hidden
            />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-bold text-amber-900">
                Bạn còn {formatVN(totalOutstanding)} chưa đóng
              </p>
              <p className="mt-0.5 text-xs text-amber-800">
                {outstanding.length} hóa đơn — vui lòng đóng trước hạn để con
                không bị gián đoạn lớp.
              </p>
            </div>
          </div>
          <Link
            href={`/parent/billing/${outstanding[0]!.id}/pay`}
            className="mt-3 flex h-11 w-full items-center justify-center rounded-xl bg-amber-600 px-4 text-sm font-bold text-white shadow-sm transition-colors hover:bg-amber-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            data-testid="parent-billing-pay-cta"
          >
            Đóng học phí ngay
          </Link>
        </section>
      )}

      <section className="mx-4 mt-6">
        <h2 className="mb-2 text-sm font-bold">Hóa đơn của bạn</h2>
        <ul
          className="rounded-2xl border bg-card"
          data-testid="parent-billing-list"
        >
          {[...outstanding, ...paid].map((inv) => {
            const badge = STATUS_BADGE[inv.status];
            return (
              <li
                key={inv.id}
                className="border-b last:border-b-0"
                data-testid={`parent-invoice-${inv.id}`}
              >
                <Link
                  href={
                    inv.status === 'PAID'
                      ? `/parent/billing/${inv.id}/success`
                      : `/parent/billing/${inv.id}/pay`
                  }
                  className="flex items-center gap-3 p-4 transition-colors hover:bg-muted/40 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <span
                    className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary"
                    aria-hidden
                  >
                    <Receipt className="h-5 w-5" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold">
                      {inv.title}
                    </p>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                      {inv.childName} · {inv.termLabel}
                    </p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {inv.status === 'PAID'
                        ? `Đã đóng ${inv.paidAt}`
                        : `Hạn ${inv.dueDate}`}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    <span className="text-sm font-bold tracking-tight">
                      {formatVN(inv.amount)}
                    </span>
                    <span
                      className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${badge.className}`}
                    >
                      {badge.label}
                    </span>
                  </div>
                  <ChevronRight
                    className="h-4 w-4 flex-shrink-0 text-muted-foreground"
                    aria-hidden
                  />
                </Link>
              </li>
            );
          })}
        </ul>
      </section>
    </ParentShell>
  );
}
