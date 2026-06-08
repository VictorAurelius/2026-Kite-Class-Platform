/**
 * Branding gateway page — entry point that routes to the AI Branding wizard.
 *
 * Wave 30 Bucket D — KC pro v2 token-styled gateway. Surfaces the AI Branding
 * value proposition (per `ai-branding-guidelines.md`: template-first, tier-based
 * regenerate quotas, preview-before-commit) and CTAs into the wizard at
 * `/branding/wizard`.
 *
 * Future scope: tenant-current-theme summary card + lifecycle status (G9
 * `InstanceLifecycleStatus`) once BrandingProvider exposes the active state.
 *
 * @author KiteClass Team
 * @since Wave 30 (GAP-266 — KC pro v2 Phase 4 kit port)
 */

'use client';

import Link from 'next/link';
import { Sparkles, Wand2, Eye, Palette, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { DashboardLayout } from '@/components/layout';

const FEATURES = [
  {
    icon: Wand2,
    title: 'Wizard 6 bước',
    description:
      'Chọn audience, tone và template — hệ thống tự dựng theme, logo và banner phù hợp.',
  },
  {
    icon: Eye,
    title: 'Preview trước khi triển khai',
    description:
      'Xem từng tài nguyên (logo, theme, banner) ở dạng iframe trước khi nhấn deploy. WCAG AA bắt buộc.',
  },
  {
    icon: Palette,
    title: 'Template-first',
    description:
      'Mặc định dùng template đã pass quality gate. AI chỉ chạy khi cần — tiết kiệm thời gian và chi phí.',
  },
];

export default function BrandingGatewayPage() {
  return (
    <DashboardLayout>
    <div className="space-y-6">
      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-2">
          <Sparkles className="h-6 w-6 text-primary" aria-hidden />
          <h1 className="text-3xl font-bold tracking-tight">AI Branding</h1>
        </div>
        <p className="text-muted-foreground">
          Tạo bộ nhận diện thương hiệu chuyên nghiệp cho trung tâm chỉ trong vài phút.
        </p>
      </div>

      {/* Hero CTA card — pro v2 token style */}
      <Card className="rounded-xl border-primary/20 bg-gradient-to-br from-primary/5 to-accent/5 shadow-sm">
        <CardHeader>
          <CardTitle className="text-2xl">Bắt đầu với wizard 6 bước</CardTitle>
          <CardDescription>
            Wizard hướng dẫn chọn template + chỉnh màu + preview + duyệt từng tài nguyên
            trước khi deploy. Phù hợp cho mọi tier (Free / Pro / Premium / Enterprise).
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Link href="/branding/wizard" data-testid="branding-wizard-cta">
            <Button size="lg">
              Mở wizard
              <ArrowRight className="ml-2 h-4 w-4" aria-hidden />
            </Button>
          </Link>
        </CardContent>
      </Card>

      {/* Feature explainer grid */}
      <div className="grid gap-4 md:grid-cols-3">
        {FEATURES.map((feature) => {
          const Icon = feature.icon;
          return (
            <Card key={feature.title} className="rounded-xl shadow-sm">
              <CardHeader>
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <Icon className="h-5 w-5" aria-hidden />
                </div>
                <CardTitle className="text-lg">{feature.title}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">{feature.description}</p>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Secondary navigation links — quick access to related surfaces */}
      <Card className="rounded-xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-lg">Quản lý theme tại đây</CardTitle>
          <CardDescription>
            Cần xem trước theme nhanh hoặc chỉnh màu thủ công? Dùng tab &ldquo;Theme preview&rdquo;
            trong Cài đặt.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Link href="/settings">
            <Button variant="outline">
              Mở Cài đặt → Theme preview
              <ArrowRight className="ml-2 h-4 w-4" aria-hidden />
            </Button>
          </Link>
        </CardContent>
      </Card>
    </div>
    </DashboardLayout>
  );
}
