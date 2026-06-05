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
import { ArrowLeft, CreditCard } from 'lucide-react';
import { toast } from 'sonner';

export default function PaymentPage() {
  const router = useRouter();
  const params = useParams();
  const paymentId = params.id as string;

  const { data: payment, isLoading, error } = usePayment(paymentId);

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

      {/* Auto-refresh indicator for PENDING status */}
      {payment.status === 'PENDING' && (
        <div className="text-center">
          <p className="text-sm text-muted-foreground flex items-center justify-center gap-2">
            <span className="inline-block w-2 h-2 bg-blue-600 rounded-full animate-pulse"></span>
            Tự động kiểm tra trạng thái thanh toán mỗi 5 giây
          </p>
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
