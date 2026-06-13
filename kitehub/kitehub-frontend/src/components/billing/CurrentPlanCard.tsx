'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Calendar, CreditCard, Zap, ArrowUpCircle, Receipt } from 'lucide-react';
import { useRouter } from 'next/navigation';
import type { Subscription } from '@/types/subscription';
import { PLAN_DETAILS, formatPrice, getDaysRemaining } from '@/lib/pricing';

interface CurrentPlanCardProps {
  subscription: Subscription;
}

const tierColors: Record<string, string> = {
  FREE: 'from-gray-500/10 to-gray-400/10',
  BASIC: 'from-blue-500/10 to-blue-400/10',
  PREMIUM: 'from-purple-500/10 to-purple-400/10',
  ENTERPRISE: 'from-amber-500/10 to-amber-400/10',
};

const tierIconColors: Record<string, string> = {
  FREE: 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400',
  BASIC: 'bg-blue-100 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400',
  PREMIUM: 'bg-purple-100 dark:bg-purple-950/50 text-purple-600 dark:text-purple-400',
  ENTERPRISE: 'bg-amber-100 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400',
};

export function CurrentPlanCard({ subscription }: CurrentPlanCardProps) {
  const router = useRouter();
  const plan = PLAN_DETAILS[subscription.tier];

  // Calculate days remaining and progress
  const daysRemaining = getDaysRemaining(subscription.expiresAt);
  const totalDays = subscription.billingCycle === 'MONTHLY' ? 30 : 365;
  const daysUsed = totalDays - daysRemaining;
  const progressPercent = Math.min(100, Math.max(0, (daysUsed / totalDays) * 100));

  // Format renewal date
  const renewalDate = new Date(subscription.expiresAt).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });

  // Get status badge variant
  const statusVariant = subscription.status === 'ACTIVE' ? 'default' :
                       subscription.status === 'PENDING' ? 'secondary' : 'destructive';

  const gradientColor = tierColors[subscription.tier] ?? tierColors.FREE;
  const iconColor = tierIconColors[subscription.tier] ?? tierIconColors.FREE;

  return (
    <Card className="shadow-soft overflow-hidden">
      {/* Plan Header with gradient */}
      <div className={`bg-gradient-to-br ${gradientColor} p-6 border-b`}>
        <div className="flex items-center justify-between mb-3">
          <div className={`rounded-xl p-2.5 ${iconColor}`}>
            <Zap className="h-5 w-5" />
          </div>
          <Badge variant={statusVariant}>
            {subscription.status === 'ACTIVE' ? 'Đang hoạt động' :
             subscription.status === 'PENDING' ? 'Chờ thanh toán' :
             subscription.status === 'CANCELLED' ? 'Đã hủy' : 'Hết hạn'}
          </Badge>
        </div>
        <h3 className="text-2xl font-bold">{plan.name}</h3>
        <p className="text-sm text-muted-foreground mt-1">
          {formatPrice(plan.monthlyPrice, subscription.billingCycle)}
        </p>
      </div>

      <CardContent className="p-6 space-y-4">
        {/* Renewal Info — GAP-1258: VietQR manual không tự trừ tiền, dùng từ
            "nhắc gia hạn" thay vì "gia hạn" để tránh ngụ ý auto-charge. */}
        <div className="flex items-center gap-2 text-sm">
          <Calendar className="h-4 w-4 text-muted-foreground" />
          <span className="text-muted-foreground">
            {subscription.autoRenew ? 'Nhắc gia hạn vào' : 'Hết hạn vào'} {renewalDate}
          </span>
        </div>

        {/* Billing Cycle Progress */}
        <div>
          <div className="flex justify-between text-sm mb-2">
            <span className="text-muted-foreground">Kỳ thanh toán</span>
            <span className="font-medium">{daysRemaining} ngày còn lại</span>
          </div>
          <Progress value={progressPercent} className="h-2" />
        </div>

        {/* Auto-renew Status — GAP-1258: relabel. VietQR/chuyển khoản thủ công
            (SUB-11) KHÔNG tự động trừ tiền; "tự động" ở đây chỉ là nhắc gia hạn,
            owner vẫn phải chuyển khoản tay + admin đối soát (SUB-19). */}
        <div className="flex items-start gap-2 p-3 bg-muted/50 rounded-xl">
          <CreditCard className="h-4 w-4 text-muted-foreground mt-0.5 shrink-0" />
          <div className="text-sm">
            <span
              className="inline-flex items-center gap-1"
              title="Phase 1 BETA dùng chuyển khoản VietQR thủ công — hệ thống chỉ gửi nhắc gia hạn, KHÔNG tự trừ tiền. Bạn chuyển khoản và quản trị viên đối soát."
            >
              Tự động nhắc gia hạn (cần chuyển khoản thủ công):{' '}
              <strong>{subscription.autoRenew ? 'Bật' : 'Tắt'}</strong>
            </span>
            <p className="mt-1 text-xs text-muted-foreground">
              Hệ thống chỉ gửi nhắc gia hạn — không tự động trừ tiền. Bạn chuyển khoản
              VietQR và quản trị viên xác nhận.
            </p>
          </div>
        </div>

        {/* Pending Tier Change */}
        {subscription.pendingTier && (
          <div className="p-3 bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 rounded-xl">
            <p className="text-sm text-blue-900 dark:text-blue-100">
              Gói <strong>{PLAN_DETAILS[subscription.pendingTier].name}</strong> sẽ được áp dụng từ {renewalDate}
            </p>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-2 pt-1">
          <Button
            onClick={() => router.push('/billing/upgrade')}
            className="flex-1"
          >
            <ArrowUpCircle className="mr-2 h-4 w-4" />
            Thay đổi gói
          </Button>
          <Button
            variant="outline"
            onClick={() => router.push('/billing/history')}
          >
            <Receipt className="mr-2 h-4 w-4" />
            Lịch sử
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
