/**
 * Parent billing success — payment confirmation screen.
 *
 * Wave 49 Bucket A (GAP-267). Ports `billing-success.html`.
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { CheckCircle2, Download, Home } from 'lucide-react';
import { ParentShell } from '@/components/parent/parent-shell';
import {
  MOCK_INVOICES,
  formatVN,
} from '@/components/parent/parent-mock-data';

export default function ParentBillingSuccessPage() {
  const params = useParams<{ invoiceId: string }>();
  const invoiceId = params?.invoiceId;
  const invoice =
    MOCK_INVOICES.find((i) => i.id === invoiceId) ?? MOCK_INVOICES[0]!;

  return (
    <ParentShell title="Thanh toán thành công" subtitle="Cảm ơn quý phụ huynh">
      <section
        className="mx-4 mt-6 rounded-2xl border bg-card p-6 text-center shadow-sm"
        data-testid="parent-billing-success-card"
      >
        <span
          className="mx-auto mb-3 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-emerald-700"
          aria-hidden
        >
          <CheckCircle2 className="h-8 w-8" />
        </span>
        <p className="text-base font-bold">Đã đóng {formatVN(invoice.amount)}</p>
        <p className="mt-1 text-xs text-muted-foreground">
          {invoice.title}
        </p>
        <dl className="mt-4 space-y-1 text-left text-sm">
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Mã giao dịch</dt>
            <dd className="font-mono text-xs">TXN-{invoice.id.toUpperCase()}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Học sinh</dt>
            <dd className="font-medium">{invoice.childName}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Thời gian</dt>
            <dd className="font-medium">
              {new Date().toLocaleString('vi-VN')}
            </dd>
          </div>
        </dl>
      </section>

      <section className="mx-4 mt-4 space-y-2">
        <button
          type="button"
          className="flex h-12 w-full items-center justify-center gap-2 rounded-2xl border bg-card text-sm font-semibold transition-colors hover:bg-muted/40"
          data-testid="parent-billing-download-receipt"
        >
          <Download className="h-4 w-4" aria-hidden />
          Tải biên lai (PDF)
        </button>
        <Link
          href="/parent"
          className="flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-primary text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <Home className="h-4 w-4" aria-hidden />
          Về trang chủ
        </Link>
      </section>
    </ParentShell>
  );
}
