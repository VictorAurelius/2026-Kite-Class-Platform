/**
 * Invoice detail page with payment history.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useParams } from 'next/navigation';
import { DashboardLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { InvoiceStatusBadge } from '@/components/billing/invoice-status-badge';
import { PaymentStatusBadge } from '@/components/billing/payment-status-badge';
import {
  useInvoice,
  useApplyLateFees,
  useCancelInvoice,
} from '@/hooks/use-invoices';
import { useInvoicePayments } from '@/hooks/use-payments';
import { formatCurrency, formatDate } from '@/lib/utils';
import { CreditCard, XCircle, AlertTriangle, AlertCircle } from 'lucide-react';
import Link from 'next/link';

export default function InvoiceDetailPage() {
  const params = useParams();
  const id = parseInt(params.id as string);
  const { data: invoice, isLoading, error } = useInvoice(id);
  const { data: payments } = useInvoicePayments(id);
  const applyLateFeesMutation = useApplyLateFees(id);
  const cancelMutation = useCancelInvoice(id);

  if (isLoading) return (
    <DashboardLayout>
      <div className="flex justify-center py-12">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" aria-label="Đang tải" />
      </div>
    </DashboardLayout>
  );

  if (error || !invoice) return (
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

  return (
    <DashboardLayout>
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">
            Hóa đơn {invoice.invoiceNumber}
          </h1>
          <div className="mt-2">
            <InvoiceStatusBadge status={invoice.status} />
          </div>
        </div>
        <div className="flex gap-2">
          {invoice.status === 'PENDING' && invoice.balanceDue > 0 && (
            <Link href={`/billing/${id}/pay`}>
              <Button>
                <CreditCard className="mr-2 h-4 w-4" />
                Thanh toán
              </Button>
            </Link>
          )}
          {invoice.status === 'PENDING' && (
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

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
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

        <Card>
          <CardHeader>
            <CardTitle>Tổng quan thanh toán</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Tổng cộng:</span>
              <span className="font-medium">
                {formatCurrency(invoice.subtotal)}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Giảm giá:</span>
              <span className="text-green-600">
                -{formatCurrency(invoice.discount)}
              </span>
            </div>
            <Separator />
            <div className="flex justify-between text-lg font-bold">
              <span>Tổng tiền:</span>
              <span>{formatCurrency(invoice.total)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Đã thanh toán:</span>
              <span>{formatCurrency(invoice.amountPaid)}</span>
            </div>
            <div className="flex justify-between text-lg font-bold text-destructive">
              <span>Còn lại:</span>
              <span>{formatCurrency(invoice.balanceDue)}</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Invoice Items */}
      <Card>
        <CardHeader>
          <CardTitle>Chi tiết</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            {invoice.items.map((item) => (
              <div key={item.id} className="flex justify-between">
                <span>
                  {item.description} (x{item.quantity})
                </span>
                <span>{formatCurrency(item.amount)}</span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Adjustments */}
      {invoice.adjustments && invoice.adjustments.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Điều chỉnh</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {invoice.adjustments.map((adjustment) => (
                <div key={adjustment.id} className="flex justify-between border-b pb-2">
                  <div>
                    <p className="font-medium">{adjustment.description}</p>
                    <p className="text-sm text-muted-foreground">
                      {adjustment.type}
                    </p>
                  </div>
                  <span>{formatCurrency(adjustment.amount)}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Payment History */}
      {payments && payments.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Lịch sử thanh toán</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {payments.map((payment) => (
                <div key={payment.id} className="flex justify-between border-b pb-2">
                  <div>
                    <p className="font-medium">{payment.paymentNumber}</p>
                    <p className="text-sm text-muted-foreground">
                      {formatDate(payment.initiatedAt)}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-medium">{formatCurrency(payment.amount)}</p>
                    <PaymentStatusBadge status={payment.paymentStatus} />
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
    </DashboardLayout>
  );
}
