/**
 * Payment form — react-hook-form + zod tree extracted from `/billing/[id]/pay`
 * so the form (and its dependencies) can be lazy-loaded via
 * `dynamic-payment-form.tsx`.
 *
 * GAP-236 Sub-PR B (Wave) — code-split admin + attendance + billing routes.
 *
 * @author KiteClass Team
 */

'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { PaymentMethodSelector } from '@/components/billing/payment-method-selector';
import { PaymentMethod } from '@/types/payment';
import { formatCurrency } from '@/lib/utils';

const schema = z.object({
  amount: z.preprocess(
    (v) => (v === '' ? undefined : Number(v)),
    z.number().min(0.01, 'Số tiền phải lớn hơn 0'),
  ),
  paymentMethod: z.nativeEnum(PaymentMethod),
});

export type PaymentFormData = z.infer<typeof schema>;

export interface PaymentFormProps {
  defaultAmount: number;
  balanceDue: number;
  isSubmitting: boolean;
  onSubmit: (data: PaymentFormData) => void;
}

export function PaymentForm({
  defaultAmount,
  balanceDue,
  isSubmitting,
  onSubmit,
}: PaymentFormProps) {
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<PaymentFormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      amount: defaultAmount,
      paymentMethod: PaymentMethod.CASH,
    },
  });

  const paymentMethod = watch('paymentMethod');

  return (
    <>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="space-y-2">
          <Label htmlFor="amount">Số tiền thanh toán</Label>
          <Input id="amount" type="number" step="0.01" {...register('amount')} />
          {errors.amount && (
            <p className="text-sm text-destructive">{errors.amount.message}</p>
          )}
          <p className="text-sm text-muted-foreground">
            Tối đa: {formatCurrency(balanceDue)}
          </p>
        </div>

        <div>
          <Label className="mb-3 block">Phương thức thanh toán</Label>
          <PaymentMethodSelector
            value={paymentMethod}
            onChange={(value) => setValue('paymentMethod', value)}
          />
        </div>

        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting ? 'Đang xử lý...' : 'Thanh toán'}
        </Button>
      </form>

      {paymentMethod !== PaymentMethod.CASH &&
        paymentMethod !== PaymentMethod.BANK_TRANSFER && (
          <div className="mt-6 rounded-lg border border-blue-200 bg-blue-50 p-4">
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
          </div>
        )}
    </>
  );
}
