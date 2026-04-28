/**
 * Create payment page for invoice.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { DashboardLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { DynamicPaymentForm } from '@/components/billing/dynamic-payment-form';
import type { PaymentFormData } from '@/components/billing/dynamic-payment-form';
import { useInvoice } from '@/hooks/use-invoices';
import { useCreatePayment } from '@/hooks/use-payments';
import { formatCurrency } from '@/lib/utils';
import { AlertCircle } from 'lucide-react';

export default function CreatePaymentPage() {
  const params = useParams();
  const invoiceId = parseInt(params.id as string);
  const { data: invoice, isLoading, isError } = useInvoice(invoiceId);
  const createPaymentMutation = useCreatePayment();

  const onSubmit = (data: PaymentFormData) => {
    createPaymentMutation.mutate({
      invoiceId,
      amount: data.amount,
      paymentMethod: data.paymentMethod,
    });
  };

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

  if (isError || !invoice)
    return (
      <DashboardLayout>
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <AlertCircle className="mb-4 h-12 w-12 text-destructive" />
          <h2 className="mb-2 text-xl font-semibold">Không tìm thấy hóa đơn</h2>
          <p className="mb-4 text-muted-foreground">
            Hóa đơn không tồn tại hoặc đã bị xóa.
          </p>
          <Link href="/billing">
            <Button variant="outline">Quay lại danh sách hóa đơn</Button>
          </Link>
        </div>
      </DashboardLayout>
    );

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-2xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Thanh toán hóa đơn</h1>
          <p className="text-muted-foreground">Hóa đơn: {invoice.invoiceNumber}</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Thông tin thanh toán</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="mb-4 space-y-2">
              <div className="flex justify-between">
                <span>Tổng tiền:</span>
                <span className="font-medium">{formatCurrency(invoice.total)}</span>
              </div>
              <div className="flex justify-between">
                <span>Đã thanh toán:</span>
                <span>{formatCurrency(invoice.amountPaid)}</span>
              </div>
              <div className="flex justify-between text-lg font-bold text-destructive">
                <span>Còn lại:</span>
                <span>{formatCurrency(invoice.balanceDue)}</span>
              </div>
            </div>

            {/* Form lazy-loaded — keeps react-hook-form + zod resolver out of initial JS */}
            <DynamicPaymentForm
              defaultAmount={invoice.balanceDue || 0}
              balanceDue={invoice.balanceDue}
              isSubmitting={createPaymentMutation.isPending}
              onSubmit={onSubmit}
            />
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
