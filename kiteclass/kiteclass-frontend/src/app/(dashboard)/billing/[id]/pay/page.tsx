/**
 * Create payment page for invoice.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { DashboardLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { PaymentMethodSelector } from '@/components/billing/payment-method-selector';
import { useInvoice } from '@/hooks/use-invoices';
import { useCreatePayment } from '@/hooks/use-payments';
import { PaymentMethod } from '@/types/payment';
import { formatCurrency } from '@/lib/utils';
import { AlertCircle } from 'lucide-react';

const schema = z.object({
  amount: z.preprocess(
    (v) => (v === '' ? undefined : Number(v)),
    z.number().min(0.01, 'Số tiền phải lớn hơn 0')
  ),
  paymentMethod: z.nativeEnum(PaymentMethod),
});

type FormData = z.infer<typeof schema>;

export default function CreatePaymentPage() {
  const params = useParams();
  const invoiceId = parseInt(params.id as string);
  const { data: invoice, isLoading, isError } = useInvoice(invoiceId);
  const createPaymentMutation = useCreatePayment();

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      amount: invoice?.balanceDue || 0,
      paymentMethod: PaymentMethod.CASH,
    },
  });

  const paymentMethod = watch('paymentMethod');

  const onSubmit = (data: FormData) => {
    createPaymentMutation.mutate({
      invoiceId,
      amount: data.amount,
      paymentMethod: data.paymentMethod,
    });
  };

  if (isLoading) return (
    <DashboardLayout>
      <div className="flex justify-center py-12">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" aria-label="Đang tải" />
      </div>
    </DashboardLayout>
  );

  if (isError || !invoice)
    return (
      <DashboardLayout>
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <AlertCircle className="mb-4 h-12 w-12 text-destructive" />
          <h2 className="mb-2 text-xl font-semibold">Không tìm thấy hóa đơn</h2>
          <p className="mb-4 text-muted-foreground">Hóa đơn không tồn tại hoặc đã bị xóa.</p>
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
        <p className="text-muted-foreground">
          Hóa đơn: {invoice.invoiceNumber}
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Thông tin thanh toán</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="mb-4 space-y-2">
            <div className="flex justify-between">
              <span>Tổng tiền:</span>
              <span className="font-medium">
                {formatCurrency(invoice.total)}
              </span>
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

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="amount">Số tiền thanh toán</Label>
              <Input
                id="amount"
                type="number"
                step="0.01"
                {...register('amount')}
              />
              {errors.amount && (
                <p className="text-sm text-destructive">
                  {errors.amount.message}
                </p>
              )}
              <p className="text-sm text-muted-foreground">
                Tối đa: {formatCurrency(invoice.balanceDue)}
              </p>
            </div>

            <div>
              <Label className="mb-3 block">Phương thức thanh toán</Label>
              <PaymentMethodSelector
                value={paymentMethod}
                onChange={(value) => setValue('paymentMethod', value)}
              />
            </div>

            <Button
              type="submit"
              className="w-full"
              disabled={createPaymentMutation.isPending}
            >
              {createPaymentMutation.isPending ? 'Đang xử lý...' : 'Thanh toán'}
            </Button>
          </form>
        </CardContent>
      </Card>

      {paymentMethod !== PaymentMethod.CASH &&
        paymentMethod !== PaymentMethod.BANK_TRANSFER && (
          <Card className="border-blue-200 bg-blue-50">
            <CardContent className="pt-6">
              <p className="text-sm text-blue-900">
                <strong>Lưu ý:</strong> Bạn sẽ được chuyển đến trang thanh toán
                của{' '}
                {paymentMethod === PaymentMethod.VNPAY
                  ? 'VNPay'
                  : paymentMethod === PaymentMethod.MOMO
                    ? 'MoMo'
                    : paymentMethod === PaymentMethod.ZALOPAY
                      ? 'ZaloPay'
                      : 'cổng thanh toán'}
                .
              </p>
            </CardContent>
          </Card>
        )}
    </div>
    </DashboardLayout>
  );
}
