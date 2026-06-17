'use client';

import { useRouter } from 'next/navigation';
import { Clock, ArrowRight, X } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import { useCancelPendingPayment } from '@/hooks/use-subscriptions';
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
  const cancelPendingPayment = useCancelPendingPayment();

  if (pending.status !== 'PENDING') return null;

  // GAP-1471 — abandon the in-flight pending payment so the owner can request a fresh one.
  // On success the ['subscriptions'] invalidation re-fetches pending-payment-status → null,
  // so this banner unmounts.
  const handleCancel = () => {
    cancelPendingPayment.mutate(pending.subscriptionId, {
      onSuccess: () => {
        toast.success('Đã hủy yêu cầu thanh toán. Bạn có thể tạo lại.');
      },
      onError: () => {
        toast.error('Không thể hủy yêu cầu thanh toán. Vui lòng thử lại.');
      },
    });
  };

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
      <div className="mt-4 flex flex-wrap gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={() => router.push(`/billing/payment/${pending.pendingPaymentId}`)}
          data-testid="pending-payment-view-cta"
        >
          Xem thông tin chuyển khoản
          <ArrowRight className="ml-2 h-4 w-4" />
        </Button>

        <AlertDialog>
          <AlertDialogTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              className="text-amber-800 hover:text-amber-900 dark:text-amber-200 dark:hover:text-amber-100"
              disabled={cancelPendingPayment.isPending}
              data-testid="pending-payment-cancel-cta"
            >
              <X className="mr-2 h-4 w-4" />
              {cancelPendingPayment.isPending ? 'Đang hủy…' : 'Hủy yêu cầu thanh toán'}
            </Button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Hủy yêu cầu thanh toán?</AlertDialogTitle>
              <AlertDialogDescription>
                Yêu cầu chuyển khoản <strong>{formatVnd(pending.amountVnd)}</strong> đang chờ
                sẽ bị hủy. Gói đăng ký hiện tại của bạn không thay đổi. Bạn có thể tạo lại yêu
                cầu thanh toán bất cứ lúc nào.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Giữ lại</AlertDialogCancel>
              <AlertDialogAction
                onClick={handleCancel}
                data-testid="pending-payment-cancel-confirm"
              >
                Hủy yêu cầu
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  );
}
