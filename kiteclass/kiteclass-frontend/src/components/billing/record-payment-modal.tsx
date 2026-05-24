/**
 * RecordPaymentModal — Wave beta-readiness-4 Bucket C / GAP-292b.
 *
 * Modal cho giáo viên/admin "Đánh dấu hóa đơn đã thu" với 4 phương thức
 * (CASH / BANK_TRANSFER / VIETQR / MOMO). Gắn vào trang invoice detail.
 *
 * Distinct với gateway payment flow (`PaymentForm` → redirect VNPAY/MoMo) —
 * modal này record manually sau khi nhận tiền thực tế.
 */

'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  PaymentRecordMethod,
  PAYMENT_RECORD_METHOD_LABELS,
} from '@/types/payment-record';
import { paymentRecordsApi } from '@/lib/api/payment-records';

const recordPaymentSchema = z.object({
  method: z.nativeEnum(PaymentRecordMethod, {
    errorMap: () => ({ message: 'Vui lòng chọn phương thức thanh toán' }),
  }),
  amount: z
    .number({ invalid_type_error: 'Vui lòng nhập số tiền' })
    .positive({ message: 'Số tiền phải lớn hơn 0' }),
  paidAt: z.string().optional(),
  note: z
    .string()
    .max(500, { message: 'Ghi chú tối đa 500 ký tự' })
    .optional(),
});

type RecordPaymentFormData = z.infer<typeof recordPaymentSchema>;

interface RecordPaymentModalProps {
  invoiceId: number;
  /** Default amount auto-populated from invoice balance (VND). */
  defaultAmount?: number;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

/**
 * Generate a UUID v4 for Idempotency-Key header.
 * Uses crypto.randomUUID if available, fallback to manual generation.
 */
function generateIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export function RecordPaymentModal({
  invoiceId,
  defaultAmount,
  isOpen,
  onClose,
  onSuccess,
}: RecordPaymentModalProps) {
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    watch,
  } = useForm<RecordPaymentFormData>({
    resolver: zodResolver(recordPaymentSchema),
    defaultValues: {
      method: PaymentRecordMethod.CASH,
      amount: defaultAmount,
      paidAt: new Date().toISOString().slice(0, 10), // YYYY-MM-DD today
      note: '',
    },
  });

  const selectedMethod = watch('method');

  const onSubmit = async (data: RecordPaymentFormData) => {
    setSubmitting(true);
    setSubmitError(null);
    try {
      const idempotencyKey = generateIdempotencyKey();
      const paidAt = data.paidAt
        ? new Date(data.paidAt + 'T00:00:00Z').toISOString()
        : undefined;
      await paymentRecordsApi.record(
        invoiceId,
        {
          method: data.method,
          amount: data.amount,
          paidAt,
          note: data.note,
        },
        idempotencyKey
      );
      reset();
      onSuccess?.();
      onClose();
    } catch (err) {
      const msg =
        err instanceof Error
          ? err.message
          : 'Có lỗi xảy ra khi ghi nhận thanh toán';
      setSubmitError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Đánh dấu hóa đơn đã thu</DialogTitle>
          <DialogDescription>
            Ghi nhận khoản thanh toán bạn vừa nhận từ phụ huynh / học viên.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Phương thức thanh toán */}
          <div className="space-y-2">
            <Label>Phương thức thanh toán</Label>
            <div className="grid grid-cols-1 gap-2">
              {Object.values(PaymentRecordMethod).map((method) => (
                <label
                  key={method}
                  className={`flex items-center gap-2 rounded-md border p-2 cursor-pointer hover:bg-accent ${
                    selectedMethod === method ? 'border-primary bg-accent' : ''
                  }`}
                >
                  <input
                    type="radio"
                    value={method}
                    {...register('method')}
                    className="h-4 w-4"
                  />
                  <span>{PAYMENT_RECORD_METHOD_LABELS[method]}</span>
                </label>
              ))}
            </div>
            {errors.method && (
              <p className="text-sm text-destructive">{errors.method.message}</p>
            )}
          </div>

          {/* Số tiền */}
          <div className="space-y-2">
            <Label htmlFor="amount">Số tiền (đ)</Label>
            <Input
              id="amount"
              type="number"
              step="1000"
              min="1000"
              placeholder="Ví dụ: 1500000"
              {...register('amount', { valueAsNumber: true })}
            />
            {errors.amount && (
              <p className="text-sm text-destructive">{errors.amount.message}</p>
            )}
          </div>

          {/* Ngày thu */}
          <div className="space-y-2">
            <Label htmlFor="paidAt">Ngày thu</Label>
            <Input
              id="paidAt"
              type="date"
              {...register('paidAt')}
            />
          </div>

          {/* Ghi chú */}
          <div className="space-y-2">
            <Label htmlFor="note">Ghi chú (tùy chọn)</Label>
            <Textarea
              id="note"
              rows={3}
              placeholder="Ví dụ: Phụ huynh em Trần Thị Hồng thanh toán tháng 5/2026 bằng chuyển khoản Vietcombank"
              {...register('note')}
            />
            {errors.note && (
              <p className="text-sm text-destructive">{errors.note.message}</p>
            )}
          </div>

          {submitError && (
            <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
              {submitError}
            </div>
          )}

          <DialogFooter className="gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={submitting}
            >
              Hủy
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Đang ghi nhận...' : 'Xác nhận đã thu'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
