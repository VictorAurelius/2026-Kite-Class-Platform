'use client';

import { useEffect } from 'react';
import dynamic from 'next/dynamic';
import { useRouter, useParams } from 'next/navigation';
import { usePayment } from '@/hooks/use-payments';
import { PaymentInfo } from '@/components/billing/PaymentInfo';
import { PaymentStatusCard } from '@/components/billing/PaymentStatusCard';
import { BetaModeBanner } from '@/components/billing/BetaModeBanner';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';

// GAP-236 Sub-PR B — QRCodeDisplay only renders when payment.status ===
// 'PENDING'. Lazy-load it so completed/failed/expired flows skip the chunk.
const QRCodeDisplay = dynamic(
  () => import('@/components/billing/QRCodeDisplay').then((m) => ({ default: m.QRCodeDisplay })),
  { ssr: false }
);
import { ErrorAlert } from '@/components/common/ErrorAlert';
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
import { ArrowLeft, CreditCard, X } from 'lucide-react';
import { toast } from 'sonner';

export default function PaymentPage() {
  const router = useRouter();
  const params = useParams();
  const paymentId = params.id as string;

  const { data: payment, isLoading, error } = usePayment(paymentId);
  const cancelPendingPayment = useCancelPendingPayment();

  // GAP-1471 — abandon this in-flight pending payment (keyed by subscription id) so the owner
  // can request a fresh one. On success the payment is soft-deleted (this page would 404), so
  // redirect back to /billing.
  const handleCancelPending = () => {
    if (!payment) return;
    cancelPendingPayment.mutate(payment.subscriptionId, {
      onSuccess: () => {
        toast.success('Đã hủy yêu cầu thanh toán. Bạn có thể tạo lại.');
        router.push('/billing');
      },
      onError: () => {
        toast.error('Không thể hủy yêu cầu thanh toán. Vui lòng thử lại.');
      },
    });
  };

  // Redirect to billing page when payment is completed
  useEffect(() => {
    if (payment?.status === 'COMPLETED') {
      toast.success('Thanh toán thành công! Đang chuyển hướng...');
      setTimeout(() => {
        router.push('/billing?success=upgrade');
      }, 2000);
    }
  }, [payment?.status, router]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !payment) {
    return (
      <div className="space-y-4">
        <ErrorAlert message="Không tìm thấy thông tin thanh toán" />
        <Button
          variant="outline"
          onClick={() => router.push('/billing')}
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại trang thanh toán
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center gap-4 mb-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => router.push('/billing')}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Quay lại
          </Button>
        </div>
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-primary/10 p-3 text-primary">
            <CreditCard className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Thanh toán</h1>
            <p className="text-muted-foreground">
              Hoàn tất thanh toán để nâng cấp gói đăng ký
            </p>
          </div>
        </div>
      </div>

      {/* Beta payment-mode notice (GAP-977) — only when the override flag is on */}
      <BetaModeBanner />

      {/* Payment Status */}
      <PaymentStatusCard status={payment.status} createdAt={payment.createdAt} />

      {/* Payment Content */}
      <div className="grid gap-6 md:grid-cols-2">
        {/* QR Code (only show for PENDING) */}
        {payment.status === 'PENDING' && payment.qrCodeUrl && (
          <QRCodeDisplay
            qrCodeUrl={payment.qrCodeUrl}
            expiresAt={payment.expiresAt}
          />
        )}

        {/* Payment Info */}
        <PaymentInfo payment={payment} />
      </div>

      {/* Auto-refresh indicator + cancel affordance for PENDING status */}
      {payment.status === 'PENDING' && (
        <div className="space-y-3 text-center">
          <p className="text-sm text-muted-foreground flex items-center justify-center gap-2">
            <span className="inline-block w-2 h-2 bg-blue-600 rounded-full animate-pulse"></span>
            Tự động kiểm tra trạng thái thanh toán mỗi 5 giây
          </p>
          {/* GAP-1471 — hủy yêu cầu thanh toán (vd khi QR sai/cũ) để tạo lại */}
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="text-muted-foreground"
                disabled={cancelPendingPayment.isPending}
                data-testid="payment-cancel-pending-cta"
              >
                <X className="mr-2 h-4 w-4" />
                {cancelPendingPayment.isPending ? 'Đang hủy…' : 'Hủy yêu cầu thanh toán'}
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Hủy yêu cầu thanh toán?</AlertDialogTitle>
                <AlertDialogDescription>
                  Yêu cầu chuyển khoản đang chờ sẽ bị hủy. Gói đăng ký hiện tại của bạn không
                  thay đổi. Bạn có thể tạo lại yêu cầu thanh toán bất cứ lúc nào.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Giữ lại</AlertDialogCancel>
                <AlertDialogAction
                  onClick={handleCancelPending}
                  data-testid="payment-cancel-pending-confirm"
                >
                  Hủy yêu cầu
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      )}

      {/* Failed/Expired actions */}
      {(payment.status === 'FAILED' || payment.status === 'EXPIRED') && (
        <div className="flex justify-center gap-4">
          <Button
            variant="outline"
            onClick={() => router.push('/billing')}
          >
            Quay lại trang thanh toán
          </Button>
          <Button
            onClick={() => router.push('/billing/upgrade')}
          >
            Thử lại
          </Button>
        </div>
      )}
    </div>
  );
}
