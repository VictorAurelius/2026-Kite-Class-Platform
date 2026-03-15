'use client';

import { useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { usePayment } from '@/hooks/use-payments';
import { QRCodeDisplay } from '@/components/billing/QRCodeDisplay';
import { PaymentInfo } from '@/components/billing/PaymentInfo';
import { PaymentStatusCard } from '@/components/billing/PaymentStatusCard';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { ArrowLeft } from 'lucide-react';
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
      <div className="flex items-center justify-center min-h-screen">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !payment) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-2xl">
        <ErrorAlert message="Không tìm thấy thông tin thanh toán" />
        <Button
          variant="outline"
          onClick={() => router.push('/billing')}
          className="mt-4"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại trang thanh toán
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      {/* Header */}
      <div className="mb-6">
        <Button
          variant="ghost"
          onClick={() => router.push('/billing')}
          className="mb-4"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại
        </Button>
        <h1 className="text-3xl font-bold">Thanh toán</h1>
        <p className="text-muted-foreground mt-1">
          Hoàn tất thanh toán để nâng cấp gói đăng ký
        </p>
      </div>

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
        <div className="mt-6 text-center">
          <p className="text-sm text-muted-foreground flex items-center justify-center gap-2">
            <span className="inline-block w-2 h-2 bg-blue-600 rounded-full animate-pulse"></span>
            Tự động kiểm tra trạng thái thanh toán mỗi 5 giây
          </p>
        </div>
      )}

      {/* Failed/Expired actions */}
      {(payment.status === 'FAILED' || payment.status === 'EXPIRED') && (
        <div className="mt-6 flex justify-center gap-4">
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
