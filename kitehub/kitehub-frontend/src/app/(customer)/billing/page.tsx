'use client';

import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useActiveSubscription } from '@/hooks/use-subscriptions';
import { CurrentPlanCard } from '@/components/billing/CurrentPlanCard';
import { PlanComparison } from '@/components/billing/PlanComparison';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { useSearchParams } from 'next/navigation';
import { useEffect } from 'react';

export default function BillingPage() {
  const user = useAuthStore((state) => state.user);
  const searchParams = useSearchParams();

  // Get user's instances (assuming 1 user = 1 instance for now)
  const { data: instances, isLoading: instancesLoading, error: instancesError } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;

  // Get active subscription
  const { data: subscription, isLoading: subLoading, error: subError } = useActiveSubscription(instanceId);

  // Handle success messages from redirects
  useEffect(() => {
    const success = searchParams.get('success');
    if (success === 'payment') {
      // Show success toast (will implement in Task #8)
      console.log('Payment successful!');
    } else if (success === 'downgrade') {
      console.log('Downgrade scheduled successfully!');
    }
  }, [searchParams]);

  if (instancesLoading || subLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <LoadingSpinner />
      </div>
    );
  }

  if (instancesError || subError) {
    return <ErrorAlert message="Không thể tải thông tin thanh toán. Vui lòng thử lại." />;
  }

  if (!subscription) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center">
          <h1 className="text-2xl font-bold mb-4">Chưa có gói đăng ký</h1>
          <p className="text-muted-foreground mb-6">
            Bạn chưa có gói đăng ký nào. Vui lòng chọn gói phù hợp với nhu cầu của bạn.
          </p>
          <PlanComparison currentTier={null} />
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold">Thanh toán & Đăng ký</h1>
        <p className="text-muted-foreground mt-2">
          Quản lý gói đăng ký và lịch sử thanh toán của bạn
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-1">
          <CurrentPlanCard subscription={subscription} />
        </div>

        <div className="lg:col-span-2">
          <PlanComparison currentTier={subscription.tier} />
        </div>
      </div>
    </div>
  );
}
