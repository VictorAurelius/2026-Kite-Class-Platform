'use client';

import { useRouter } from 'next/navigation';
import { Clock, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { formatVnd } from '@/lib/pricing';
import type { PendingPaymentStatus } from '@/types/subscription';

interface PendingPaymentBannerProps {
  pending: PendingPaymentStatus;
}

/**
 * GAP-1257-FE — "Đang chờ xác nhận" banner.
 *
 * VietQR manual transfer (SUB-11) does NOT auto-capture — after the owner
 * transfers, a platform admin reconciles the bank statement and confirms
 * (SUB-19, UC-SUB-07). This banner tells the owner the request is awaiting
 * admin confirmation, shows the amount + expiry + SLA, and links back to the
 * payment page (QR / transfer content).
 *
 * Data source: `usePendingPaymentStatus` (GET pending-payment-status — BE-4).
 * Render only when a PENDING payment exists.
 */
export function PendingPaymentBanner({ pending }: PendingPaymentBannerProps) {
  const router = useRouter();

  if (pending.status !== 'PENDING') return null;

  const expiryLabel = pending.expiresAt
    ? new Date(pending.expiresAt).toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : null;

  return (
    <div
      className="rounded-2xl border border-amber-300 bg-amber-50 dark:border-amber-800 dark:bg-amber-950/30 p-5"
      data-testid="pending-payment-banner"
      role="status"
    >
      <div className="flex items-start gap-3">
        <div className="rounded-xl bg-amber-100 dark:bg-amber-900/50 p-2.5 text-amber-700 dark:text-amber-300">
          <Clock className="h-5 w-5 animate-pulse" aria-hidden />
        </div>
        <div className="flex-1 space-y-1">
          <p className="font-semibold text-amber-900 dark:text-amber-100">
            Đang chờ xác nhận thanh toán
          </p>
          <p className="text-sm text-amber-800 dark:text-amber-200">
            Yêu cầu chuyển khoản <strong>{formatVnd(pending.amountVnd)}</strong> của bạn
            đang chờ quản trị viên đối soát sao kê ngân hàng rồi xác nhận. Gói mới chỉ
            được kích hoạt sau khi xác nhận.
          </p>
          <p className="text-sm text-amber-800 dark:text-amber-200">
            Thời gian xử lý dự kiến:{' '}
            <strong>{pending.adminConfirmSla ?? 'trong vòng 24 giờ làm việc'}</strong>.
            {expiryLabel ? ` Nội dung chuyển khoản có hiệu lực đến ${expiryLabel}.` : ''}
          </p>
        </div>
      </div>
      <div className="mt-4">
        <Button
          variant="outline"
          size="sm"
          onClick={() => router.push(`/billing/payment/${pending.pendingPaymentId}`)}
          data-testid="pending-payment-view-cta"
        >
          Xem thông tin chuyển khoản
          <ArrowRight className="ml-2 h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
