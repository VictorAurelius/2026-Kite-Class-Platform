'use client';

import { useRouter } from 'next/navigation';
import { RefreshCw, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import type { Subscription } from '@/types/subscription';
import type { InstanceStatus } from '@/types/instance';

interface ReactivateBannerProps {
  subscription?: Subscription | null;
  instanceStatus?: InstanceStatus;
}

/**
 * GAP-1263-FE — Reactivate / win-back CTA.
 *
 * Shown when the subscription is CANCELLED/EXPIRED OR the instance is SUSPENDED.
 * Offers a "Kích hoạt lại" CTA that routes to the upgrade/create flow
 * (UC-SUB-05 manual renew / UC-SUB-01 create-new). The upgrade page already
 * branches to create-subscription when no ACTIVE subscription exists, so this
 * reuses that path. A dedicated BE reactivate endpoint may be added later; for
 * now the existing create/upgrade flow restores a paid subscription.
 */
export function ReactivateBanner({ subscription, instanceStatus }: ReactivateBannerProps) {
  const router = useRouter();

  const subInactive =
    subscription?.status === 'CANCELLED' || subscription?.status === 'EXPIRED';
  const instanceSuspended = instanceStatus === 'SUSPENDED' || instanceStatus === 'EXPIRED';

  if (!subInactive && !instanceSuspended) return null;

  const headline = instanceSuspended
    ? 'Trung tâm của bạn đang bị tạm ngưng'
    : 'Gói đăng ký đã kết thúc';

  return (
    <div
      className="rounded-2xl border border-primary/30 bg-gradient-to-r from-primary/10 to-accent/10 p-5"
      data-testid="reactivate-banner"
      role="status"
    >
      <div className="flex items-start gap-3">
        <div className="rounded-xl bg-primary/10 p-2.5 text-primary">
          <RefreshCw className="h-5 w-5" aria-hidden />
        </div>
        <div className="flex-1 space-y-1">
          <p className="font-semibold">{headline}</p>
          <p className="text-sm text-muted-foreground">
            Kích hoạt lại để khôi phục đầy đủ tính năng. Dữ liệu của bạn được giữ lại
            trong 30 ngày — đăng ký lại ngay để không gián đoạn hoạt động.
          </p>
        </div>
      </div>
      <div className="mt-4">
        <Button
          onClick={() => router.push('/billing/upgrade')}
          data-testid="reactivate-cta"
        >
          <RefreshCw className="mr-2 h-4 w-4" />
          Kích hoạt lại
          <ArrowRight className="ml-2 h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
