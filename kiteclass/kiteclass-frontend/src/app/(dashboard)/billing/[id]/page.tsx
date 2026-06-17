/**
 * Invoice detail page — KC pro v2 token-styled detail with G6 + G10 integration.
 *
 * Wave 30 Bucket D — wires `@kite/shared-ui` G6 `InvoiceDetail` (preview panel
 * mirrors VN tax-invoice format per Nghị định 123/2020/NĐ-CP) + G10
 * `PaymentStatusTimeline` (lifecycle steps from received payments). Preserves
 * the existing controls (Pay / Apply late fee / Cancel) that depend on the
 * authoritative KC `Invoice` type.
 *
 * @author KiteClass Team
 * @since 1.0.0 — G6/G10 integration Wave 30 (GAP-266)
 */

'use client';

import { useMemo, useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import dynamic from 'next/dynamic';
import { useQueryClient } from '@tanstack/react-query';
import { CreditCard, XCircle, AlertTriangle, AlertCircle, Receipt } from 'lucide-react';
import {
  InvoiceDetail,
  PaymentStatusTimeline,
  formatVNCurrency,
  type InvoiceData,
  type InvoiceState,
  type PaymentStatusTimelineProps,
  type TimelineEvent,
} from '@kite/shared-ui';

// Derive the state-bucket type from G10 props (G10's PaymentTimelineState is
// not directly exported via the package barrel; we reach it through Props).
type PaymentTimelineState = PaymentStatusTimelineProps['state'];
import { DashboardLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { InvoiceStatusBadge } from '@/components/billing/invoice-status-badge';
import { DynamicInvoiceDetailPanels } from '@/components/billing/dynamic-invoice-detail-panels';
import {
  useInvoice,
  useApplyLateFees,
  useCancelInvoice,
} from '@/hooks/use-invoices';
import { useInvoicePayments, useInvoicePaymentRecords } from '@/hooks/use-payments';
import { formatDate } from '@/lib/utils';
import type { Invoice, InvoiceItem } from '@/types/invoice';
import { InvoiceStatus as KCInvoiceStatus, InvoiceAdjustmentType } from '@/types/invoice';
import type { Payment } from '@/types/payment';
import { PaymentStatus } from '@/types/payment';
import type { PaymentRecord } from '@/types/payment-record';
import { PAYMENT_RECORD_METHOD_LABELS } from '@/types/payment-record';

// GAP-1431 — RecordPaymentModal (react-hook-form + zod + Dialog ~7.5KB) chỉ mở khi
// click "Ghi nhận thanh toán" → code-split khỏi First Load JS (giữ /billing/[id]
// dưới bundle budget 250KB; pattern GAP-236). ssr:false vì modal client-only.
const RecordPaymentModal = dynamic(
  () => import('@/components/billing/record-payment-modal').then((m) => m.RecordPaymentModal),
  { ssr: false },
);

/** Map KC `Invoice` (backend DTO) → shared-ui G6 `InvoiceData`. */
function toG6Invoice(invoice: Invoice): InvoiceData {
  const items = invoice.items.map((item: InvoiceItem) => ({
    title: item.description,
    quantity: item.quantity,
    unitPrice: item.unitPrice,
    lineTotal: item.amount,
  }));

  const discounts = invoice.adjustments
    .filter((adj) => adj.type === InvoiceAdjustmentType.DISCOUNT || adj.type === InvoiceAdjustmentType.WAIVER)
    .map((adj) => ({ label: adj.description, amount: Math.abs(adj.amount) }));

  // Late fees + penalties surface as destructive line items (per G6 spec).
  const lateFeeItems = invoice.adjustments
    .filter((adj) => adj.type === InvoiceAdjustmentType.LATE_FEE || adj.type === InvoiceAdjustmentType.PENALTY)
    .map((adj) => ({
      title: adj.description,
      quantity: 1,
      unitPrice: adj.amount,
      lineTotal: adj.amount,
      intent: 'destructive' as const,
    }));

  const status: InvoiceData['status'] =
    invoice.status === KCInvoiceStatus.PAID
      ? 'PAID'
      : invoice.status === KCInvoiceStatus.OVERDUE
      ? 'OVERDUE'
      : invoice.status === KCInvoiceStatus.CANCELLED
      ? 'VOID'
      : invoice.amountPaid > 0 && invoice.balanceDue > 0
      ? 'PARTIAL_PAID'
      : 'PENDING_PAYMENT';

  return {
    number: invoice.invoiceNumber,
    status,
    issueDate: new Date(invoice.issueDate),
    dueDate: new Date(invoice.dueDate),
    paidDate: invoice.paidAt ? new Date(invoice.paidAt) : undefined,
    items: [...items, ...lateFeeItems],
    discounts,
    subtotal: invoice.subtotal,
    total: invoice.total,
    balance: invoice.balanceDue,
    student: {
      fullName: `Học viên #${invoice.studentId}`,
      className: `Lớp #${invoice.classId}`,
    },
    tenant: {
      // Tenant identity comes from BrandingProvider context elsewhere; for the
      // detail page we render a neutral placeholder until §7 closure wires it.
      name: 'Trung tâm KiteClass',
      address: 'Địa chỉ tổ chức',
    },
  };
}

/**
 * Map KC payments → G10 timeline events + state derivation.
 *
 * GAP-1433 — merge cả hai nguồn thanh toán:
 *  - `payments`: gateway/SePay payment (`/payments/invoice/{id}`)
 *  - `paymentRecords`: phiếu thu thủ công (`/invoices/{id}/payment-records`, tạo qua record-payment)
 * Trước đây timeline chỉ đọc gateway payments → phiếu thu thủ công không hiện.
 */
function toG10Timeline(
  invoice: Invoice,
  payments: Payment[] | undefined,
  paymentRecords: PaymentRecord[] | undefined,
): {
  state: PaymentTimelineState;
  events: TimelineEvent[];
} {
  const events: TimelineEvent[] = [
    {
      step: 'CREATED',
      at: new Date(invoice.issueDate),
      note: 'Hóa đơn được tạo',
      actor: 'Hệ thống',
    },
    {
      step: 'PAYMENT_PENDING',
      at: new Date(invoice.issueDate),
      note: `Hạn thanh toán ${formatDate(invoice.dueDate)}`,
      actor: 'Hệ thống',
    },
  ];

  const sortedPayments = (payments ?? [])
    .slice()
    .sort((a, b) => new Date(a.initiatedAt).getTime() - new Date(b.initiatedAt).getTime());

  for (const p of sortedPayments) {
    if (p.paymentStatus === PaymentStatus.COMPLETED && p.completedAt) {
      events.push({
        step: 'PAYMENT_RECEIVED',
        at: new Date(p.completedAt),
        note: `Phiếu thu ${p.paymentNumber}`,
        actor: p.paymentMethod,
        amount: p.amount,
      });
    } else if (p.paymentStatus === PaymentStatus.FAILED) {
      events.push({
        step: 'FAILED',
        at: new Date(p.completedAt ?? p.initiatedAt),
        note: p.failureReason ?? 'Thanh toán thất bại',
        actor: p.paymentMethod,
        status: 'failed',
      });
    } else if (p.paymentStatus === PaymentStatus.REFUNDED) {
      events.push({
        step: 'REFUNDED',
        at: new Date(p.completedAt ?? p.initiatedAt),
        note: 'Đã hoàn tiền',
        amount: p.amount,
      });
    }
  }

  // GAP-1433 — phiếu thu thủ công (CASH/BANK_TRANSFER/VIETQR/MOMO) ghi qua
  // record-payment. Mỗi phiếu = 1 sự kiện PAYMENT_RECEIVED trên timeline.
  for (const r of paymentRecords ?? []) {
    events.push({
      step: 'PAYMENT_RECEIVED',
      at: new Date(r.paidAt),
      note: r.note || 'Phiếu thu thủ công',
      actor: PAYMENT_RECORD_METHOD_LABELS[r.method] ?? r.method,
      amount: r.amount,
    });
  }

  if (invoice.status === KCInvoiceStatus.PAID && invoice.paidAt) {
    events.push({
      step: 'COMPLETED',
      at: new Date(invoice.paidAt),
      note: 'Đã thanh toán đủ',
      actor: 'Hệ thống',
    });
  }

  // Sắp xếp toàn bộ sự kiện theo thời gian (gateway + thủ công xen kẽ đúng thứ tự).
  events.sort((a, b) => a.at.getTime() - b.at.getTime());

  let state: PaymentTimelineState = 'pending';
  if (invoice.status === KCInvoiceStatus.PAID) state = 'paid';
  else if (invoice.status === KCInvoiceStatus.OVERDUE) state = 'overdue';
  else if (invoice.amountPaid > 0 && invoice.balanceDue > 0) state = 'partial-paid';
  else if (sortedPayments.some((p) => p.paymentStatus === PaymentStatus.REFUNDED)) state = 'refunded';

  return { state, events };
}

export default function InvoiceDetailPage() {
  const params = useParams();
  const id = parseInt(params.id as string);
  const queryClient = useQueryClient();
  const { data: invoice, isLoading, error } = useInvoice(id);
  const { data: payments } = useInvoicePayments(id);
  // GAP-1433 — phiếu thu thủ công để merge vào timeline.
  const { data: paymentRecords } = useInvoicePaymentRecords(id);
  const applyLateFeesMutation = useApplyLateFees(id);
  const cancelMutation = useCancelInvoice(id);

  // GAP-1431 — modal "Ghi nhận thanh toán" (RecordPaymentModal) cho phiếu thu thủ công.
  const [recordModalOpen, setRecordModalOpen] = useState(false);

  /**
   * GAP-1431 — sau khi ghi nhận phiếu thu thành công, invalidate cả invoice
   * (status/balanceDue đổi sau khi BE reconcile) lẫn payment-records (timeline)
   * để UI tự cập nhật mà không cần reload trang.
   */
  const handleRecordSuccess = () => {
    queryClient.invalidateQueries({ queryKey: ['invoices', id] });
    queryClient.invalidateQueries({ queryKey: ['invoices'] });
    queryClient.invalidateQueries({ queryKey: ['payment-records', 'invoice', id] });
    queryClient.invalidateQueries({ queryKey: ['payments', 'invoice', id] });
  };

  // Map KC types → G6/G10 props (memo so empty arrays don't churn).
  const g6Invoice = useMemo(() => (invoice ? toG6Invoice(invoice) : null), [invoice]);
  const timeline = useMemo<Pick<PaymentStatusTimelineProps, 'state' | 'events'> | null>(
    () => (invoice ? toG10Timeline(invoice, payments, paymentRecords) : null),
    [invoice, payments, paymentRecords],
  );

  if (isLoading)
    return (
      <DashboardLayout>
        <div className="flex justify-center py-12">
          <div
            className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"
            aria-label="Đang tải"
          />
        </div>
      </DashboardLayout>
    );

  if (error || !invoice)
    return (
      <DashboardLayout>
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <AlertCircle className="mb-4 h-12 w-12 text-destructive" />
          <h2 className="mb-2 text-xl font-semibold">Không tìm thấy hóa đơn</h2>
          <p className="mb-4 text-muted-foreground">Hóa đơn không tồn tại hoặc không thể tải dữ liệu.</p>
          <Link href="/billing">
            <Button variant="outline">Quay lại danh sách hóa đơn</Button>
          </Link>
        </div>
      </DashboardLayout>
    );

  const g6State: InvoiceState =
    g6Invoice?.status === 'PAID'
      ? 'paid'
      : g6Invoice?.status === 'OVERDUE'
      ? 'overdue'
      : g6Invoice?.status === 'PENDING_PAYMENT' || g6Invoice?.status === 'PARTIAL_PAID'
      ? 'pending'
      : 'default';

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">
              Hóa đơn {invoice.invoiceNumber}
            </h1>
            <div className="mt-2">
              <InvoiceStatusBadge status={invoice.status} />
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            {invoice.status === KCInvoiceStatus.PENDING && invoice.balanceDue > 0 && (
              <Link href={`/billing/${id}/pay`}>
                <Button>
                  <CreditCard className="mr-2 h-4 w-4" />
                  Thanh toán
                </Button>
              </Link>
            )}
            {/*
              GAP-1431 — nút "Ghi nhận thanh toán" mở RecordPaymentModal để ghi
              phiếu thu thủ công (tiền mặt / chuyển khoản / VietQR / MoMo). Hiện
              khi hóa đơn còn dư nợ và chưa ở trạng thái kết thúc (PAID/CANCELLED/
              DRAFT) — bao gồm PENDING/OVERDUE và các trạng thái SENT/PARTIAL nếu
              được bổ sung (GAP-1432).
            */}
            {invoice.balanceDue > 0 &&
              invoice.status !== KCInvoiceStatus.PAID &&
              invoice.status !== KCInvoiceStatus.CANCELLED &&
              invoice.status !== KCInvoiceStatus.DRAFT && (
                <Button variant="outline" onClick={() => setRecordModalOpen(true)}>
                  <Receipt className="mr-2 h-4 w-4" />
                  Ghi nhận thanh toán
                </Button>
              )}
            {invoice.status === KCInvoiceStatus.PENDING && (
              <>
                <Button
                  variant="outline"
                  onClick={() => applyLateFeesMutation.mutate()}
                  disabled={applyLateFeesMutation.isPending}
                >
                  <AlertTriangle className="mr-2 h-4 w-4" />
                  Tính phí trễ
                </Button>
                <Button
                  variant="destructive"
                  onClick={() => cancelMutation.mutate()}
                  disabled={cancelMutation.isPending}
                >
                  <XCircle className="mr-2 h-4 w-4" />
                  Hủy hóa đơn
                </Button>
              </>
            )}
          </div>
        </div>

        {/* Owner-facing financial summary tiles (pro v2 token-style). */}
        <div className="grid gap-6 md:grid-cols-2">
          <Card className="rounded-xl shadow-sm">
            <CardHeader>
              <CardTitle>Thông tin hóa đơn</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Ngày phát hành:</span>
                <span>{formatDate(invoice.issueDate)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Hạn thanh toán:</span>
                <span>{formatDate(invoice.dueDate)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Kỳ học:</span>
                <span>
                  {formatDate(invoice.periodStart)} - {formatDate(invoice.periodEnd)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Học viên:</span>
                <span>#{invoice.studentId}</span>
              </div>
            </CardContent>
          </Card>

          <Card className="rounded-xl shadow-sm">
            <CardHeader>
              <CardTitle>Tổng quan thanh toán</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Tổng cộng:</span>
                <span className="font-medium">{formatVNCurrency(invoice.subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Giảm giá:</span>
                <span className="text-green-600">-{formatVNCurrency(invoice.discount)}</span>
              </div>
              <Separator />
              <div className="flex justify-between text-lg font-bold">
                <span>Tổng tiền:</span>
                <span>{formatVNCurrency(invoice.total)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Đã thanh toán:</span>
                <span>{formatVNCurrency(invoice.amountPaid)}</span>
              </div>
              <div className="flex justify-between text-lg font-bold text-destructive">
                <span>Còn lại:</span>
                <span>{formatVNCurrency(invoice.balanceDue)}</span>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* G10 PaymentStatusTimeline — lifecycle visualization */}
        {timeline && (
          <section data-testid="invoice-payment-timeline" aria-label="Lịch sử thanh toán">
            <PaymentStatusTimeline
              invoiceNumber={invoice.invoiceNumber}
              state={timeline.state}
              events={timeline.events}
              totalAmount={invoice.total}
              embedded
            />
          </section>
        )}

        {/* G6 InvoiceDetail — VN tax-invoice rendering (preview/print) */}
        {g6Invoice && (
          <section data-testid="invoice-detail-g6" aria-label="Chi tiết hóa đơn (định dạng in)">
            <InvoiceDetail invoice={g6Invoice} state={g6State} />
          </section>
        )}

        {/* Items + Adjustments + Payment History — lazy-loaded panels */}
        <DynamicInvoiceDetailPanels invoice={invoice} payments={payments} />
      </div>

      {/* GAP-1431 — modal ghi nhận phiếu thu thủ công, mặc định điền sẵn số dư còn lại.
          Render có điều kiện: dynamic chunk chỉ tải khi owner mở modal (giữ First Load JS thấp). */}
      {recordModalOpen && (
        <RecordPaymentModal
          invoiceId={id}
          defaultAmount={invoice.balanceDue}
          isOpen={recordModalOpen}
          onClose={() => setRecordModalOpen(false)}
          onSuccess={handleRecordSuccess}
        />
      )}
    </DashboardLayout>
  );
}
