'use client';

import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useActiveSubscription } from '@/hooks/use-subscriptions';
import { usePaymentHistory } from '@/hooks/use-payments';
import { PaymentHistoryTable } from '@/components/billing/PaymentHistoryTable';
import { CurrentPlanCard } from '@/components/billing/CurrentPlanCard';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Receipt } from 'lucide-react';
import { useRouter } from 'next/navigation';

export default function PaymentHistoryPage() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);

  // Get user's instance and subscription
  const { data: instances } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;
  const { data: subscription } = useActiveSubscription(instanceId?.toString());

  // Get payment history
  const { data: payments, isLoading, error } = usePaymentHistory(subscription?.id);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-4">
        <ErrorAlert message="Không thể tải lịch sử thanh toán" />
        <Button
          variant="outline"
          onClick={() => router.push('/billing')}
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
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
            <Receipt className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Lịch sử thanh toán</h1>
            <p className="text-muted-foreground">
              Xem tất cả các giao dịch thanh toán của bạn
            </p>
          </div>
        </div>
      </div>

      {/* GAP-1267 — self-serve billing portal: gói hiện tại + ngày nhắc gia hạn
          (next-renewal) bên cạnh lịch sử hóa đơn/thanh toán. */}
      {subscription ? (
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-1">
            <CurrentPlanCard subscription={subscription} />
          </div>
          <div className="lg:col-span-2">
            <PaymentHistoryTable payments={payments || []} />
          </div>
        </div>
      ) : (
        <PaymentHistoryTable payments={payments || []} />
      )}
    </div>
  );
}
