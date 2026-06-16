'use client';

import { useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Check, X, Sparkles } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { cn } from '@/lib/utils';
import { PLAN_DETAILS, formatVnd, type PricingTier } from '@/lib/pricing';

interface PlanComparisonProps {
  currentTier: PricingTier | null;
}

// GAP-1465: dùng formatVnd từ lib/pricing để format số tiền ĐỒNG NHẤT với
// TierSelector (/billing/upgrade) — trước đây formatVND local ("500.000₫")
// khác formatPrice ("500.000 ₫/tháng") gây hiển thị lệch giữa 2 route.
function formatVND(amount: number): string {
  if (amount === 0) return 'Miễn phí';
  if (amount < 0) return 'Liên hệ';
  return formatVnd(amount);
}

export function PlanComparison({ currentTier }: PlanComparisonProps) {
  const router = useRouter();
  const [annual, setAnnual] = useState(false);
  const tiers: PricingTier[] = ['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'];

  const handleSelectPlan = (tier: PricingTier) => {
    if (tier === currentTier) return;
    if (tier === 'ENTERPRISE') {
      // GAP-1101 cross-flow sweep: Enterprise = sales-assisted. Navigate to the
      // lead-capture form instead of mailto:sales@kitehub.me (wrong domain —
      // canonical is support@kitehub.me / /contact).
      router.push('/contact?plan=enterprise');
      return;
    }
    router.push(`/billing/upgrade?tier=${tier}`);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold">So sánh các gói</h2>
          <p className="text-muted-foreground text-sm mt-1">
            Chọn gói phù hợp với quy mô và nhu cầu của bạn
          </p>
        </div>

        {/* Annual/Monthly Toggle */}
        <div className="flex items-center gap-3">
          <span className={cn('text-sm', !annual && 'font-semibold')}>Tháng</span>
          <button
            onClick={() => setAnnual(!annual)}
            className={cn(
              'relative inline-flex h-6 w-11 items-center rounded-full transition-colors',
              annual ? 'bg-primary' : 'bg-muted-foreground/30'
            )}
          >
            <span
              className={cn(
                'inline-block h-4 w-4 rounded-full bg-white dark:bg-foreground transition-transform',
                annual ? 'translate-x-6' : 'translate-x-1'
              )}
            />
          </button>
          <span className={cn('text-sm', annual && 'font-semibold')}>
            Năm
            <span className="ml-1.5 rounded-full bg-green-100 dark:bg-green-950/50 px-2 py-0.5 text-xs font-medium text-green-700 dark:text-green-400">
              -10%
            </span>
          </span>
        </div>
      </div>

      {/* Plan Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {tiers.map((tier) => {
          const plan = PLAN_DETAILS[tier];
          const isCurrent = tier === currentTier;
          const isHighlighted = tier === 'PREMIUM';
          const price = annual ? plan.yearlyPrice : plan.monthlyPrice;
          const period = annual ? '/năm' : '/tháng';

          return (
            <Card
              key={tier}
              className={cn(
                'relative flex flex-col shadow-soft transition-all hover:shadow-lg',
                isHighlighted && 'border-primary ring-1 ring-primary',
                isCurrent && 'border-primary/50 bg-primary/5'
              )}
            >
              {/* Highlighted Badge */}
              {isHighlighted && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                  <Badge className="bg-primary text-primary-foreground px-3 py-1 shadow-md">
                    <Sparkles className="h-3 w-3 mr-1" />
                    Phổ biến nhất
                  </Badge>
                </div>
              )}

              <CardContent className={cn('flex flex-col flex-1 p-6', isHighlighted && 'pt-8')}>
                {/* Plan Name */}
                <div className="flex items-center justify-between mb-1">
                  <h3 className="text-lg font-bold">{plan.name}</h3>
                  {isCurrent && (
                    <Badge variant="secondary" className="text-xs">
                      Hiện tại
                    </Badge>
                  )}
                </div>

                {/* Price */}
                <div className="mt-2 mb-4">
                  <span className="text-3xl font-bold">{formatVND(price)}</span>
                  {price > 0 && (
                    <span className="text-sm text-muted-foreground ml-1">{period}</span>
                  )}
                  {annual && plan.monthlyPrice > 0 && (
                    <p className="text-xs text-muted-foreground mt-1 line-through">
                      {formatVND(plan.monthlyPrice * 12)}/năm
                    </p>
                  )}
                </div>

                {/* Features */}
                <ul className="flex-1 space-y-2.5 mb-6">
                  {plan.features.map((feature, idx) => (
                    <li key={idx} className="flex items-start gap-2">
                      <div className="rounded-full bg-green-100 dark:bg-green-950/50 p-0.5 mt-0.5 flex-shrink-0">
                        <Check className="h-3 w-3 text-green-600 dark:text-green-400" />
                      </div>
                      <span className="text-sm">{feature}</span>
                    </li>
                  ))}
                  {plan.limits.map((limit, idx) => (
                    <li key={idx} className="flex items-start gap-2">
                      <div className="rounded-full bg-muted p-0.5 mt-0.5 flex-shrink-0">
                        <X className="h-3 w-3 text-muted-foreground" />
                      </div>
                      <span className="text-sm text-muted-foreground">{limit}</span>
                    </li>
                  ))}
                </ul>

                {/* CTA Button */}
                <Button
                  onClick={() => handleSelectPlan(tier)}
                  variant={isHighlighted ? 'default' : isCurrent ? 'outline' : 'outline'}
                  className={cn(
                    'w-full',
                    isHighlighted && !isCurrent && 'bg-primary hover:bg-primary/90'
                  )}
                  disabled={isCurrent}
                >
                  {isCurrent
                    ? 'Đang sử dụng'
                    : tier === 'ENTERPRISE'
                    ? 'Liên hệ'
                    : currentTier && tier < currentTier
                    ? 'Hạ gói'
                    : 'Nâng cấp'}
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
