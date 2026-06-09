/**
 * Pricing section — displays course pricing tiers from tenant CMS data.
 *
 * Anti-fabrication (GAP-958): renders ONLY real tenant-configured pricing tiers.
 * When no pricing is configured the section hides entirely — never shows invented
 * prices. page.tsx only emits slots.plans when the backend returns non-empty
 * pricingTiers.
 *
 * @since 2026-04-04
 */

import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import Link from 'next/link';
import type { SlotData, SlotItem } from '@/lib/template/slots';

interface PricingSectionProps {
  slots?: SlotData;
}

export function PricingSection({ slots }: PricingSectionProps) {
  const plans = slots?.plans as SlotItem[] | undefined;
  if (!plans || plans.length === 0) return null;
  // Highlight the middle tier when ≥3 tiers exist; otherwise no forced highlight.
  const featuredIndex = plans.length >= 3 ? 1 : -1;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-4">Bảng giá</h2>
        <p className="text-center text-muted-foreground mb-12 max-w-2xl mx-auto">
          Học phí minh bạch, linh hoạt — chọn gói phù hợp với mục tiêu và ngân sách
        </p>
        <div className="grid md:grid-cols-3 gap-8 max-w-5xl mx-auto">
          {plans.map((plan, index) => (
            <article key={plan.title}>
              <Card
                className={
                  index === featuredIndex
                    ? 'relative h-full rounded-xl border-2 border-theme-primary shadow-xl transition-shadow hover:shadow-2xl md:-translate-y-2'
                    : 'h-full rounded-xl shadow-md transition-shadow hover:shadow-xl'
                }
              >
                {index === featuredIndex && (
                  <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                    <Badge className="bg-theme-cta px-3 text-white shadow-sm">Phổ biến nhất</Badge>
                  </div>
                )}
                <CardHeader className="text-center pb-2 pt-8">
                  <div className="text-3xl mb-2">{plan.icon}</div>
                  <h3 className="font-bold text-xl">{plan.title}</h3>
                  <p className="text-2xl font-bold text-theme-primary mt-2">{plan.description}</p>
                </CardHeader>
                <CardContent className="pt-4">
                  {plan.items && (
                    <ul className="space-y-2 mb-6">
                      {plan.items.map((item) => (
                        <li key={item} className="text-sm flex items-start gap-2">
                          <span className="text-theme-primary mt-0.5 shrink-0">✓</span>
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                  <Button
                    asChild
                    className={
                      index === featuredIndex
                        ? 'w-full bg-theme-cta font-semibold text-white hover:bg-theme-cta/90'
                        : 'w-full'
                    }
                    variant={index === featuredIndex ? 'default' : 'outline'}
                  >
                    <Link href="/contact">Liên hệ tư vấn</Link>
                  </Button>
                </CardContent>
              </Card>
            </article>
          ))}
        </div>
        <p className="text-center text-xs text-muted-foreground mt-8">
          * Học phí có thể thay đổi theo từng khóa học. Liên hệ để nhận báo giá chính xác.
        </p>
      </div>
    </section>
  );
}
