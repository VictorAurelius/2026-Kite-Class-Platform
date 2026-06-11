/**
 * Features / "Tính năng nổi bật" section.
 *
 * Anti-fabrication + audience fit (GAP-1205): the ported default features were
 * KiteClass PLATFORM capabilities ("Hệ thống LMS", "Thanh toán & Báo cáo") —
 * a pitch TO center owners, wrong audience for a tenant landing (visitors are
 * parents/students) and not the tenant's own content. So this section renders
 * ONLY tenant-provided slot data and hides entirely when none is configured
 * (cf. Teachers/Pricing hide-when-empty, GAP-958).
 */

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { BookOpen, Users, TrendingUp, CheckCircle2, Award, Calendar, Star } from 'lucide-react';
import type { SlotData, SlotItem } from '@/lib/template/slots';

const ICON_MAP: Record<string, React.ComponentType<{ className?: string }>> = {
  book: BookOpen,
  users: Users,
  trending: TrendingUp,
  award: Award,
  calendar: Calendar,
  star: Star,
};

interface FeaturesSectionProps {
  slots?: SlotData;
  /** Title override (GAP-1208); defaults to "Tính năng nổi bật". */
  heading?: string;
  /** Optional lead paragraph; only rendered when provided. */
  subheading?: string;
}

export function FeaturesSection({ slots, heading, subheading }: FeaturesSectionProps) {
  const features = slots?.features as SlotItem[] | undefined;
  if (!features || features.length === 0) return null;

  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className={`text-3xl font-bold text-center ${subheading ? 'mb-4' : 'mb-12'}`}>{heading ?? 'Tính năng nổi bật'}</h2>
        {subheading && (
          <p className="text-center text-muted-foreground mb-12 max-w-2xl mx-auto">{subheading}</p>
        )}
        <div className="grid md:grid-cols-3 gap-8">
          {features.map((feature) => {
            const IconComponent = ICON_MAP[feature.icon || 'star'] || Star;
            return (
              <Card key={feature.title}>
                <CardHeader>
                  <IconComponent className="h-12 w-12 mb-4 text-theme-primary" />
                  <CardTitle>{feature.title}</CardTitle>
                  {feature.description && <CardDescription>{feature.description}</CardDescription>}
                </CardHeader>
                {feature.items && feature.items.length > 0 && (
                  <CardContent>
                    <ul className="space-y-2 text-sm">
                      {feature.items.map((item) => (
                        <li key={item} className="flex items-start gap-2">
                          <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5" />
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </CardContent>
                )}
              </Card>
            );
          })}
        </div>
      </div>
    </section>
  );
}
