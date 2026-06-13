'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Sparkles, ArrowRight, Phone } from 'lucide-react';
import {
  PLAN_DETAILS,
  formatPrice,
  recommendTierByStudents,
  type PricingTier,
  type TierRecommendation,
} from '@/lib/pricing';

interface TierRecommenderProps {
  /** Optional callback when a non-enterprise tier is chosen (e.g. pre-select in wizard). */
  onSelectTier?: (tier: PricingTier) => void;
}

/**
 * GAP-1269 — Tier recommender by student count.
 *
 * Owner enters their expected number of students; we suggest the smallest tier
 * whose cap fits (FREE 10 / BASIC 50 / PREMIUM 200 / ENTERPRISE unlimited, per
 * SUB-22). ENTERPRISE → "Liên hệ tư vấn" CTA (sales-assisted, custom pricing)
 * routing to /contact?plan=enterprise (same target as TierSelector GAP-1101).
 */
export function TierRecommender({ onSelectTier }: TierRecommenderProps) {
  const router = useRouter();
  const [input, setInput] = useState('');
  const [recommendation, setRecommendation] = useState<TierRecommendation | null>(null);

  const handleRecommend = () => {
    const count = parseInt(input, 10);
    if (Number.isNaN(count) || count < 0) {
      setRecommendation(null);
      return;
    }
    setRecommendation(recommendTierByStudents(count));
  };

  const plan = recommendation ? PLAN_DETAILS[recommendation.tier] : null;

  return (
    <Card className="rounded-xl shadow-sm" data-testid="tier-recommender">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Sparkles className="h-4 w-4 text-primary" aria-hidden />
          Gợi ý gói phù hợp
        </CardTitle>
        <p className="text-sm text-muted-foreground">
          Nhập số học viên dự kiến, chúng tôi sẽ gợi ý gói tiết kiệm nhất.
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
          <div className="flex-1 space-y-1.5">
            <Label htmlFor="recommender-student-count">Số học viên dự kiến</Label>
            <Input
              id="recommender-student-count"
              type="number"
              min={0}
              inputMode="numeric"
              placeholder="Ví dụ: 80"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleRecommend();
              }}
              data-testid="recommender-input"
            />
          </div>
          <Button
            type="button"
            onClick={handleRecommend}
            data-testid="recommender-submit"
          >
            Gợi ý gói
          </Button>
        </div>

        {recommendation && plan && (
          <div
            className="rounded-xl border border-primary/20 bg-primary/5 p-4"
            data-testid="recommender-result"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-sm text-muted-foreground">Gói gợi ý:</span>
                <Badge data-testid="recommender-tier">{plan.name}</Badge>
              </div>
              <span className="text-sm font-semibold">
                {recommendation.isEnterprise
                  ? 'Giá tùy chỉnh'
                  : formatPrice(plan.monthlyPrice, 'MONTHLY')}
              </span>
            </div>
            <p className="mt-2 text-sm text-muted-foreground">{recommendation.reason}</p>

            <div className="mt-3">
              {recommendation.isEnterprise ? (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => router.push('/contact?plan=enterprise')}
                  data-testid="recommender-enterprise-cta"
                >
                  <Phone className="mr-2 h-4 w-4" />
                  Liên hệ tư vấn
                </Button>
              ) : (
                <Button
                  type="button"
                  onClick={() => {
                    if (onSelectTier) {
                      onSelectTier(recommendation.tier);
                    } else {
                      router.push(`/billing/upgrade?tier=${recommendation.tier}`);
                    }
                  }}
                  data-testid="recommender-select-cta"
                >
                  Chọn gói {plan.name}
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
