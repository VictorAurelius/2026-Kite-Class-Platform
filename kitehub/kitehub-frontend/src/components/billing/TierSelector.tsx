'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Check, ArrowRight, XCircle } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { PLAN_DETAILS, formatPrice, getTierRank, type PricingTier } from '@/lib/pricing';

interface TierSelectorProps {
  currentTier: PricingTier;
  selectedTier: PricingTier | null;
  onSelect: (tier: PricingTier) => void;
}

export function TierSelector({ currentTier, selectedTier, onSelect }: TierSelectorProps) {
  const router = useRouter();
  const tiers: PricingTier[] = ['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'];

  // GAP-1435: paid owner (currentTier != FREE) has no valid downgrade path to
  // FREE — BE rejects PATCH /downgrade → FREE (SubscriptionService line 280-285,
  // per SUB-01/GAP-1018: cancel ends a subscription, downgrade only moves between
  // paid tiers). So FREE must NOT be a selectable downgrade target for a paid
  // owner; the owner cancels in Settings → Danger Zone to drop to FREE.
  const isDowngradeToFreeForPaidOwner = (tier: PricingTier) =>
    tier === 'FREE' && currentTier !== 'FREE';

  const handleSelect = (tier: PricingTier) => {
    if (tier === currentTier) return; // Can't select current tier
    if (tier === 'ENTERPRISE') {
      // GAP-1101: Enterprise = sales-assisted. Navigate to the lead-capture form
      // instead of the old alert() (which referenced the wrong domain
      // sales@kitehub.me — canonical is support@kitehub.me / /contact).
      router.push('/contact?plan=enterprise');
      return;
    }
    onSelect(tier);
  };

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Chọn gói mới</h2>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {tiers.map((tier) => {
          const plan = PLAN_DETAILS[tier];
          const isCurrent = tier === currentTier;
          const isSelected = tier === selectedTier;
          // GAP-1435: FREE is not a selectable downgrade target for paid owners.
          const downgradeToFree = isDowngradeToFreeForPaidOwner(tier);
          const isDisabled = isCurrent || tier === 'ENTERPRISE' || downgradeToFree;

          const tierRank = getTierRank(tier);
          const currentRank = getTierRank(currentTier);
          const changeType = tierRank > currentRank ? 'upgrade' :
                           tierRank < currentRank ? 'downgrade' : null;

          return (
            <Card
              key={tier}
              className={`
                cursor-pointer transition-all
                ${isSelected ? 'border-primary shadow-lg ring-2 ring-primary' : ''}
                ${isCurrent ? 'opacity-60 cursor-not-allowed' : ''}
                ${isDisabled ? '' : 'hover:shadow-md'}
              `}
              onClick={() => !isDisabled && handleSelect(tier)}
            >
              <CardHeader>
                <div className="flex items-center justify-between mb-2">
                  <CardTitle className="text-lg">{plan.name}</CardTitle>
                  {isCurrent && <Badge variant="secondary">Hiện tại</Badge>}
                  {isSelected && <Badge>Đã chọn</Badge>}
                </div>
                <CardDescription className="text-xl font-bold text-foreground">
                  {formatPrice(plan.monthlyPrice, 'MONTHLY')}
                </CardDescription>
              </CardHeader>

              <CardContent className="space-y-3">
                {/* Key Features (top 3) */}
                <div className="space-y-1.5">
                  {plan.features.slice(0, 3).map((feature, idx) => (
                    <div key={idx} className="flex items-start gap-2">
                      <Check className="h-4 w-4 text-green-600 dark:text-green-400 mt-0.5 flex-shrink-0" />
                      <span className="text-sm">{feature}</span>
                    </div>
                  ))}
                </div>

                {/* GAP-1435: FREE downgrade for paid owner → guidance to cancel,
                    NOT a "Hạ gói" target (BE rejects PATCH /downgrade → FREE). */}
                {downgradeToFree ? (
                  <div className="pt-2 border-t space-y-2" data-testid="downgrade-to-free-guidance">
                    <p className="text-sm text-muted-foreground">
                      Để chuyển về gói Miễn phí, vui lòng hủy đăng ký hiện tại.
                    </p>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        router.push('/settings');
                      }}
                    >
                      <XCircle className="h-4 w-4 mr-2" />
                      Hủy đăng ký
                    </Button>
                  </div>
                ) : (
                  changeType && !isCurrent && (
                    <div className="flex items-center gap-2 pt-2 border-t">
                      <ArrowRight className={`h-4 w-4 ${changeType === 'upgrade' ? 'text-green-600 dark:text-green-400' : 'text-orange-600 dark:text-orange-400'}`} />
                      <span className={`text-sm font-medium ${changeType === 'upgrade' ? 'text-green-600 dark:text-green-400' : 'text-orange-600 dark:text-orange-400'}`}>
                        {changeType === 'upgrade' ? 'Nâng cấp' : 'Hạ gói'}
                      </span>
                    </div>
                  )
                )}
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
