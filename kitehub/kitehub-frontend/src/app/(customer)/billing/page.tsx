'use client';

import dynamic from 'next/dynamic';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useActiveSubscription } from '@/hooks/use-subscriptions';
import { CurrentPlanCard } from '@/components/billing/CurrentPlanCard';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';

// GAP-236 Sub-PR B — PlanComparison renders the full pricing matrix (4 tiers
// × many features). Defer it so the header + CurrentPlanCard render first.
const PlanComparison = dynamic(
  () => import('@/components/billing/PlanComparison').then((m) => ({ default: m.PlanComparison })),
  { ssr: false, loading: () => (
    <div className="flex items-center justify-center py-12">
      <LoadingSpinner />
    </div>
  ) }
);
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { useSearchParams, useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { CreditCard, Receipt, ArrowUpCircle } from 'lucide-react';
import { toast } from 'sonner';

export default function BillingPage() {
  const user = useAuthStore((state) => state.user);
  const searchParams = useSearchParams();
  const router = useRouter();

  // Get user's instances (assuming 1 user = 1 instance for now)
  const { data: instances, isLoading: instancesLoading, error: instancesError } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;

  // Get active subscription
  const { data: subscription, isLoading: subLoading, error: subError } = useActiveSubscription(instanceId?.toString());

  // Handle success messages from redirects
  useEffect(() => {
    const success = searchParams.get('success');
    if (success === 'payment') {
      toast.success('Thanh toán thành công!');
    } else if (success === 'downgrade') {
      toast.success('Đã lên lịch hạ gói thành công!');
    }
  }, [searchParams]);

  if (instancesLoading || subLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  if (instancesError) {
    return <ErrorAlert message="Không thể tải thông tin thanh toán. Vui lòng thử lại." />;
  }

  // subError (400) = no subscription yet (trial user) → show plan comparison
  if (!subscription || subError) {
    return (
      <div className="space-y-6">
        {/* Page Header */}
        <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-primary/10 p-3 text-primary">
              <CreditCard className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Chưa có gói đăng ký</h1>
              <p className="text-muted-foreground">
                Bạn đang trong giai đoạn dùng thử. Chọn gói phù hợp với nhu cầu của bạn.
              </p>
            </div>
          </div>
        </div>

        <PlanComparison currentTier={null} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-primary/10 p-3 text-primary">
              <CreditCard className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Thanh toán & Đăng ký</h1>
              <p className="text-muted-foreground">
                Quản lý gói đăng ký và lịch sử thanh toán của bạn
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => router.push('/billing/history')}
            >
              <Receipt className="mr-2 h-4 w-4" />
              Lịch sử
            </Button>
            <Button
              size="sm"
              onClick={() => router.push('/billing/upgrade')}
            >
              <ArrowUpCircle className="mr-2 h-4 w-4" />
              Nâng cấp
            </Button>
          </div>
        </div>
      </div>

      {/* Content */}
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
