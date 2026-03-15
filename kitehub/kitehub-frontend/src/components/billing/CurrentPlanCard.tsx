'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Calendar, CreditCard, Zap } from 'lucide-react';
import { useRouter } from 'next/navigation';
import type { Subscription } from '@/types/subscription';
import { PLAN_DETAILS, formatPrice, getDaysRemaining } from '@/lib/pricing';

interface CurrentPlanCardProps {
  subscription: Subscription;
}

export function CurrentPlanCard({ subscription }: CurrentPlanCardProps) {
  const router = useRouter();
  const plan = PLAN_DETAILS[subscription.tier];

  // Calculate days remaining and progress
  const daysRemaining = getDaysRemaining(subscription.expiresAt);
  const totalDays = subscription.billingCycle === 'MONTHLY' ? 30 : 365;
  const daysUsed = totalDays - daysRemaining;
  const progressPercent = (daysUsed / totalDays) * 100;

  // Format renewal date
  const renewalDate = new Date(subscription.expiresAt).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });

  // Get status badge variant
  const statusVariant = subscription.status === 'ACTIVE' ? 'default' :
                       subscription.status === 'PENDING' ? 'secondary' : 'destructive';

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">Gói hiện tại</CardTitle>
          <Badge variant={statusVariant}>
            {subscription.status === 'ACTIVE' ? 'Đang hoạt động' :
             subscription.status === 'PENDING' ? 'Chờ thanh toán' :
             subscription.status === 'CANCELLED' ? 'Đã hủy' : 'Hết hạn'}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Plan Name & Price */}
        <div className="flex items-baseline gap-2">
          <Zap className="h-5 w-5 text-primary" />
          <div>
            <h3 className="text-2xl font-bold">{plan.name}</h3>
            <p className="text-sm text-muted-foreground">
              {formatPrice(plan.monthlyPrice, subscription.billingCycle)}
            </p>
          </div>
        </div>

        {/* Renewal Info */}
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-sm">
            <Calendar className="h-4 w-4 text-muted-foreground" />
            <span className="text-muted-foreground">
              {subscription.autoRenew ? 'Gia hạn vào' : 'Hết hạn vào'} {renewalDate}
            </span>
          </div>

          {/* Billing Cycle Progress */}
          <div>
            <div className="flex justify-between text-sm mb-2">
              <span className="text-muted-foreground">Tiến độ kỳ thanh toán</span>
              <span className="font-medium">{daysRemaining} ngày còn lại</span>
            </div>
            <Progress value={progressPercent} className="h-2" />
          </div>
        </div>

        {/* Auto-renew Status */}
        <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
          <CreditCard className="h-4 w-4" />
          <span className="text-sm">
            {subscription.autoRenew
              ? 'Tự động gia hạn: Bật'
              : 'Tự động gia hạn: Tắt'}
          </span>
        </div>

        {/* Pending Tier Change */}
        {subscription.pendingTier && (
          <div className="p-3 bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 rounded-lg">
            <p className="text-sm text-blue-900 dark:text-blue-100">
              Gói <strong>{PLAN_DETAILS[subscription.pendingTier].name}</strong> sẽ được áp dụng từ {renewalDate}
            </p>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-2">
          <Button
            onClick={() => router.push('/billing/upgrade')}
            className="flex-1"
          >
            Thay đổi gói
          </Button>
          <Button
            variant="outline"
            onClick={() => router.push('/billing/history')}
          >
            Lịch sử
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
