/**
 * Parent billing success — payment confirmation screen.
 *
 * Wave 49 Bucket A (GAP-267). Ports `billing-success.html`.
 */

'use client';

export const dynamic = 'force-dynamic';

import { useCallback, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { CheckCircle2, Download, Home, Loader2 } from 'lucide-react';
import { ParentShell } from '@/components/parent/parent-shell';
import {
  MOCK_INVOICES,
  formatVN,
} from '@/components/parent/parent-mock-data';
import { documentsApi } from '@/lib/api/documents';
import { toast } from '@/hooks/use-toast';

export default function ParentBillingSuccessPage() {
  const params = useParams<{ invoiceId: string }>();
  const invoiceId = params?.invoiceId;
  const invoice =
    MOCK_INVOICES.find((i) => i.id === invoiceId) ?? MOCK_INVOICES[0]!;

  const [downloading, setDownloading] = useState(false);

  // GAP-1434: wire the receipt button to the real document-gen API
  // (POST /api/v1/documents/pdf/download, template `invoice`) → fetch the
  // branded PDF blob → trigger a browser download. Branding + tenant are
  // resolved server-side; we only map the receipt fields into the template
  // data shape (invoiceNumber/issueDate/buyer/items/totals).
  const handleDownloadReceipt = useCallback(async () => {
    setDownloading(true);
    try {
      const blob = await documentsApi.download('pdf', {
        templateId: 'invoice',
        data: {
          invoiceNumber: invoice.id.toUpperCase(),
          issueDate: new Date().toLocaleDateString('vi-VN'),
          buyerName: invoice.childName,
          subtotal: invoice.amount,
          vatRate: 0,
          vatAmount: 0,
          total: invoice.amount,
          items: [
            {
              description: invoice.title,
              qty: 1,
              unitPrice: invoice.amount,
              lineTotal: invoice.amount,
            },
          ],
        },
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `bien-lai-${invoice.id}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch {
      toast({
        title: 'Lỗi tải biên lai',
        description: 'Không thể tải biên lai PDF. Vui lòng thử lại sau.',
        variant: 'destructive',
      });
    } finally {
      setDownloading(false);
    }
  }, [invoice]);

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
          onClick={handleDownloadReceipt}
          disabled={downloading}
          className="flex h-12 w-full items-center justify-center gap-2 rounded-2xl border bg-card text-sm font-semibold transition-colors hover:bg-muted/40 disabled:cursor-not-allowed disabled:opacity-60"
          data-testid="parent-billing-download-receipt"
        >
          {downloading ? (
            <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
          ) : (
            <Download className="h-4 w-4" aria-hidden />
          )}
          {downloading ? 'Đang tải biên lai…' : 'Tải biên lai (PDF)'}
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
