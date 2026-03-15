'use client';

import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useActiveSubscription } from '@/hooks/use-subscriptions';
import { usePaymentHistory } from '@/hooks/use-payments';
import { PaymentHistoryTable } from '@/components/billing/PaymentHistoryTable';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { ArrowLeft } from 'lucide-react';
import { useRouter } from 'next/navigation';

export default function PaymentHistoryPage() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);

  // Get user's instance and subscription
  const { data: instances } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;
  const { data: subscription } = useActiveSubscription(instanceId);

  // Get payment history
  const { data: payments, isLoading, error } = usePaymentHistory(subscription?.id);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        <ErrorAlert message="Không thể tải lịch sử thanh toán" />
        <Button
          variant="outline"
          onClick={() => router.push('/billing')}
          className="mt-4"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      {/* Header */}
      <div className="mb-8">
        <Button
          variant="ghost"
          onClick={() => router.push('/billing')}
          className="mb-4"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại
        </Button>
        <h1 className="text-3xl font-bold mb-2">Lịch sử thanh toán</h1>
        <p className="text-muted-foreground">
          Xem tất cả các giao dịch thanh toán của bạn
        </p>
      </div>

      {/* Payment History Table */}
      <PaymentHistoryTable payments={payments || []} />
    </div>
  );
}
