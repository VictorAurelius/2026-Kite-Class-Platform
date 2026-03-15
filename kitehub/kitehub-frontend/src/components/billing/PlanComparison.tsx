'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Check, X } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { PLAN_DETAILS, formatPrice, type PricingTier } from '@/lib/pricing';

interface PlanComparisonProps {
  currentTier: PricingTier | null;
}

export function PlanComparison({ currentTier }: PlanComparisonProps) {
  const router = useRouter();
  const tiers: PricingTier[] = ['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'];

  const handleSelectPlan = (tier: PricingTier) => {
    if (tier === currentTier) return; // Already on this tier
    if (tier === 'ENTERPRISE') {
      // Contact sales for enterprise
      window.location.href = 'mailto:sales@kiteclass.com';
      return;
    }

    // Navigate to upgrade flow
    router.push(`/billing/upgrade?tier=${tier}`);
  };

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-2xl font-bold mb-2">So sánh các gói</h2>
        <p className="text-muted-foreground">
          Chọn gói phù hợp với quy mô và nhu cầu của bạn
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {tiers.map((tier) => {
          const plan = PLAN_DETAILS[tier];
          const isCurrent = tier === currentTier;
          const isEnterprise = tier === 'ENTERPRISE';

          return (
            <Card
              key={tier}
              className={isCurrent ? 'border-primary shadow-lg' : ''}
            >
              <CardHeader>
                <div className="flex items-center justify-between mb-2">
                  <CardTitle className="text-xl">{plan.name}</CardTitle>
                  {isCurrent && (
                    <Badge>Hiện tại</Badge>
                  )}
                </div>
                <CardDescription className="text-2xl font-bold text-foreground">
                  {formatPrice(plan.monthlyPrice, 'MONTHLY')}
                </CardDescription>
                {plan.yearlyPrice > 0 && (
                  <p className="text-sm text-muted-foreground">
                    hoặc {formatPrice(plan.yearlyPrice, 'ANNUALLY')}
                  </p>
                )}
              </CardHeader>

              <CardContent className="space-y-4">
                {/* Features */}
                <div className="space-y-2">
                  {plan.features.map((feature, idx) => (
                    <div key={idx} className="flex items-start gap-2">
                      <Check className="h-4 w-4 text-green-600 mt-0.5 flex-shrink-0" />
                      <span className="text-sm">{feature}</span>
                    </div>
                  ))}

                  {plan.limits.map((limit, idx) => (
                    <div key={idx} className="flex items-start gap-2">
                      <X className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                      <span className="text-sm text-muted-foreground">{limit}</span>
                    </div>
                  ))}
                </div>

                {/* CTA Button */}
                <Button
                  onClick={() => handleSelectPlan(tier)}
                  variant={isCurrent ? 'outline' : 'default'}
                  className="w-full"
                  disabled={isCurrent}
                >
                  {isCurrent ? 'Đang sử dụng' :
                   isEnterprise ? 'Liên hệ' :
                   currentTier && tier < currentTier ? 'Hạ gói' : 'Nâng cấp'}
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
