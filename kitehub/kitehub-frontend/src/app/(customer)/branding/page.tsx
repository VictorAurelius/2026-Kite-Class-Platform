'use client';

/**
 * Branding Hub Page — Wave 31 Bucket C (KH pro v2 port)
 *
 * Ports the `kitehub-pro-v2` branding hub design (`branding-hub-{default,
 * dark,loading,quota-empty}.html`) into production. Integrates G11
 * ThemePreview from `@kite/shared-ui` (Wave 29 Bucket C output).
 *
 * Scope (Wave 31 Bucket C):
 *   - Theme customization section (G11 ThemePreview integration).
 *   - Quota counter widget per `ai-branding-guidelines.md` §4.3.
 *   - Template gallery preview (6 cards — kit `branding-hub-default.html`).
 *   - Wizard CTA placeholder linking `/branding/wizard` (Wave 32 owns the
 *     route + 6-step Direction C wizard internals).
 *
 * Out of scope (Wave 32):
 *   - Wizard internals (welcome, audience, tone, template, preview-approve).
 *   - Enterprise Advanced Mode toggle.
 *   - Quality Gate widget.
 *
 * Mock data is acceptable per Wave 31 brief; real wiring tracked as
 * follow-up against `useAssets` + a new `useBrandingQuota` hook.
 */

import { useEffect } from 'react';
import dynamic from 'next/dynamic';
import { useRouter, useSearchParams } from 'next/navigation';
import { ThemePreview } from '@kite/shared-ui';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { useBrandingTier } from '@/hooks/use-branding-tier';
import { useAssets, useBrandingDeployStatus } from '@/hooks/use-branding';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import {
  Sparkles,
  Image as ImageIcon,
  Grid3x3,
  ArrowRight,
  Palette,
  LayoutGrid,
  TrendingUp,
  CheckCircle2,
  ExternalLink,
} from 'lucide-react';
import { toast } from 'sonner';

// GAP-236 Sub-PR B — lazy-load the recent-assets preview grid (below the fold).
const RecentAssetsGrid = dynamic(() => import('./RecentAssetsGrid'), {
  ssr: false,
});

// Mock theme palette — wave 31 scope. Live theme will come from the
// branding job preview state once wizard internals land (Wave 32).
const MOCK_THEME = {
  primary: '#2563eb',
  secondary: '#06b6d4',
  background: '#ffffff',
  foreground: '#0f172a',
} as const;

// Template gallery — mirrors kit `branding-hub-default.html` 6 cards.
const TEMPLATE_GALLERY = [
  {
    id: 'sky-wave',
    title: 'Bầu trời xanh',
    hint: 'Chuyên nghiệp · Trung tâm Anh ngữ',
    gradient: 'linear-gradient(135deg, #2563eb, #06b6d4)',
    label: 'Sky · Wave',
  },
  {
    id: 'warm-glow',
    title: 'Ấm áp Vàng',
    hint: 'Thân thiện · Mầm non · Tiểu học',
    gradient: 'linear-gradient(135deg, #ea580c, #f59e0b)',
    label: 'Warm · Glow',
  },
  {
    id: 'fresh-leaf',
    title: 'Tươi mát',
    hint: 'Năng động · Trung tâm Toán',
    gradient: 'linear-gradient(135deg, #16a34a, #65a30d)',
    label: 'Fresh · Leaf',
  },
  {
    id: 'modern-pop',
    title: 'Hiện đại',
    hint: 'Sang trọng · Luyện thi THPTQG',
    gradient: 'linear-gradient(135deg, #7c3aed, #ec4899)',
    label: 'Modern · Pop',
  },
  {
    id: 'minimal-slate',
    title: 'Tối giản',
    hint: 'Chuyên nghiệp · Cao cấp',
    gradient: 'linear-gradient(135deg, #0f172a, #475569)',
    label: 'Minimal · Slate',
  },
  {
    id: 'joyful-sun',
    title: 'Vui tươi',
    hint: 'Thân thiện · IELTS · TOEIC',
    gradient: 'linear-gradient(135deg, #be185d, #f59e0b)',
    label: 'Joyful · Sun',
  },
] as const;

export default function BrandingDashboardPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const user = useAuthStore((state) => state.user);
  const { data: instances, isLoading: instancesLoading } = useOwnerInstances(user?.id);
  const instanceId = instances?.[0]?.id;
  // GAP-1091a: tier thật + regenerate quota từ subscription — KHÔNG hardcode 'PRO'/10.
  const { tier, regenerateQuota, isLoading: tierLoading } = useBrandingTier(instanceId);
  const { data: assets, isLoading: assetsLoading } = useAssets(instanceId);
  // GAP-1108: post-deploy summary (DEPLOYED state + landing frontendUrl).
  const { data: deployStatus } = useBrandingDeployStatus(instanceId);

  useEffect(() => {
    if (searchParams.get('success') === 'true') {
      toast.success('Branding đã được xuất bản thành công!');
    }
  }, [searchParams]);

  if (instancesLoading || assetsLoading || tierLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  const hasAssets = assets && assets.length > 0;
  const recentAssets = assets?.slice(0, 6) || [];

  // Regenerate-usage (số lượt ĐÃ dùng) chưa có endpoint riêng — tracked follow-up.
  // Tier + monthly limit lấy từ subscription thật (useBrandingTier); used = 0 cho tới
  // khi /branding/quota ship. regenerateQuota === -1 = ENTERPRISE không giới hạn.
  const regenerateUsed = 0;
  const isUnlimitedQuota = regenerateQuota === -1;
  const quotaLimit = regenerateQuota;
  const quotaRemaining = isUnlimitedQuota
    ? Infinity
    : Math.max(0, quotaLimit - regenerateUsed);
  const quotaPct = isUnlimitedQuota
    ? 0
    : Math.min(100, Math.round((regenerateUsed / quotaLimit) * 100));
  const quotaEmpty = !isUnlimitedQuota && quotaRemaining === 0;
  // CTA "Nâng cấp PREMIUM" chỉ hiện cho FREE/BASIC — PREMIUM/ENTERPRISE đã ≥ PREMIUM.
  const canUpgradeTier = tier === 'FREE' || tier === 'BASIC';

  // GAP-1108: surface the deploy-success card only after a successful deploy.
  const showDeployCard = Boolean(deployStatus?.deployed);
  const deployedDateLabel = deployStatus?.deployedAt
    ? new Date(deployStatus.deployedAt).toLocaleDateString('vi-VN')
    : null;

  return (
    <div className="space-y-6">
      {/* Deploy-success card (GAP-1108) — shown only after instance is DEPLOYED */}
      {showDeployCard && (
        <Card className="shadow-soft border-emerald-200 bg-emerald-50/60">
          <CardContent className="p-5">
            <div className="flex items-start justify-between gap-4 flex-wrap">
              <div className="flex items-start gap-3">
                <div className="rounded-xl bg-emerald-500/15 p-3 text-emerald-600">
                  <CheckCircle2 className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-emerald-900">
                    Trang web của bạn đã sẵn sàng 🎉
                  </h2>
                  <p className="text-sm text-emerald-800/80 mt-0.5">
                    Bộ nhận diện thương hiệu đã được triển khai
                    {deployStatus?.templateId
                      ? ` · template ${deployStatus.templateId}`
                      : ''}
                    {deployedDateLabel ? ` · ${deployedDateLabel}` : ''}
                    .
                  </p>
                  {deployStatus?.frontendUrl && (
                    <p className="text-xs font-mono text-emerald-700/80 mt-1 break-all">
                      {deployStatus.frontendUrl}
                    </p>
                  )}
                </div>
              </div>
              {deployStatus?.frontendUrl && (
                <a
                  href={deployStatus.frontendUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label="Xem trang landing vừa triển khai"
                >
                  <Button className="bg-emerald-600 hover:bg-emerald-700">
                    <ExternalLink className="w-4 h-4 mr-2" />
                    Xem landing
                  </Button>
                </a>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Hero header — matches kit-pro-v2 page-header */}
      <div className="rounded-2xl bg-gradient-to-r from-purple-500/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-purple-500/10 p-3 text-purple-600">
              <Palette className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Thương hiệu AI</h1>
              <p className="text-muted-foreground">
                Tạo brand cho trung tâm bằng AI · template-first · không cần prompt
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => router.push('/branding/templates')}>
              <LayoutGrid className="w-4 h-4 mr-2" />
              Template Gallery
            </Button>
            {/* Wizard CTA — placeholder href; Wave 32 owns the wizard route. */}
            <a href="/branding/wizard" aria-label="Tạo lại brand bằng AI">
              <Button disabled={quotaEmpty}>
                <Sparkles className="w-4 h-4 mr-2" />
                Tạo lại brand
              </Button>
            </a>
          </div>
        </div>
      </div>

      {/* Quota counter widget — visible regenerate counter per ai-branding-guidelines.md §4.3 */}
      <Card className="shadow-soft">
        <CardContent className="p-5">
          <div className="flex items-center justify-between gap-4 flex-wrap">
            <div>
              <div className="text-xs text-muted-foreground uppercase tracking-wide">
                Quota làm lại brand · gói {tier}
              </div>
              <div className="text-xl font-bold mt-1">
                {isUnlimitedQuota
                  ? 'Không giới hạn lượt trong tháng này'
                  : `${quotaRemaining} / ${quotaLimit} lượt còn lại trong tháng này`}
              </div>
            </div>
            <div className="flex-1 max-w-md min-w-[200px]">
              <div
                className="h-2 rounded-full bg-muted overflow-hidden"
                role="progressbar"
                aria-valuenow={quotaPct}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-label="Tiến độ quota làm lại brand"
              >
                <div
                  className="h-full bg-primary transition-all"
                  style={{ width: `${100 - quotaPct}%` }}
                />
              </div>
              <div className="text-xs text-muted-foreground mt-1">
                Reset đầu tháng
                {canUpgradeTier && ' · nâng cấp PREMIUM để có 30 lượt'}
              </div>
            </div>
            {canUpgradeTier && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => router.push('/billing')}
              >
                <TrendingUp className="w-4 h-4 mr-2" />
                Nâng cấp PREMIUM
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Theme customization section — G11 ThemePreview from @kite/shared-ui */}
      <Card className="shadow-soft">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <div className="rounded-lg bg-primary/10 p-2 text-primary">
              <Palette className="h-4 w-4" />
            </div>
            Tuỳ biến theme · Live preview
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground mb-4">
            Live preview WCAG AA — đổi màu primary/foreground để xem độ tương phản trước khi áp dụng.
          </p>
          <ThemePreview brandColors={MOCK_THEME} initialMode="light" lang="vi" />
        </CardContent>
      </Card>

      {/* Branding status (existing functionality) */}
      <Card className="shadow-soft">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <div className="rounded-lg bg-purple-500/10 p-2 text-purple-600">
              <ImageIcon className="h-4 w-4" />
            </div>
            Trạng thái Branding
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <StatCard
              label="Tổng tài nguyên"
              value={assets?.length || 0}
              icon={<ImageIcon className="w-5 h-5 text-purple-600" />}
            />
            <StatCard
              label="Profile Images"
              value={assets?.filter((a) => a.type === 'PROFILE').length || 0}
              icon={<ImageIcon className="w-5 h-5 text-blue-600" />}
            />
            <StatCard
              label="Hero Images"
              value={assets?.filter((a) => a.type === 'HERO').length || 0}
              icon={<ImageIcon className="w-5 h-5 text-green-600" />}
            />
          </div>
        </CardContent>
      </Card>

      {/* Template gallery preview — 6 cards from kit-pro-v2 */}
      <Card className="shadow-soft">
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>6 template AI gợi ý</CardTitle>
            <p className="text-xs text-muted-foreground mt-1">
              Phù hợp với <strong>Trung tâm tiếng Anh</strong> · giọng <strong>Thân thiện</strong>
            </p>
          </div>
          <Button variant="ghost" onClick={() => router.push('/branding/templates')}>
            Xem tất cả
            <ArrowRight className="w-4 h-4 ml-2" />
          </Button>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {TEMPLATE_GALLERY.map((tpl) => (
              <article
                key={tpl.id}
                className="rounded-xl border overflow-hidden hover:shadow-md transition-shadow"
              >
                <div
                  className="h-32 flex items-center justify-center text-white font-semibold"
                  style={{ background: tpl.gradient }}
                >
                  {tpl.label}
                </div>
                <div className="p-3">
                  <div className="font-semibold text-sm">{tpl.title}</div>
                  <div className="text-xs text-muted-foreground mt-0.5">{tpl.hint}</div>
                </div>
              </article>
            ))}
          </div>
          <p className="text-xs text-muted-foreground text-center mt-3">
            ✓ Template-first routing · ~80% request không cần AI compute · phản hồi trong &lt;1 giây
          </p>
        </CardContent>
      </Card>

      {/* Recent Assets / Empty state (existing functionality) */}
      {hasAssets ? (
        <Card className="shadow-soft">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Tài nguyên gần đây</CardTitle>
            <Button variant="ghost" onClick={() => router.push('/branding/assets')}>
              Xem tất cả
              <ArrowRight className="w-4 h-4 ml-2" />
            </Button>
          </CardHeader>
          <CardContent>
            <RecentAssetsGrid assets={recentAssets} />
          </CardContent>
        </Card>
      ) : (
        <EmptyState
          icon={<Grid3x3 className="w-12 h-12" />}
          title="Chưa có tài nguyên Branding"
          description="Bắt đầu tạo bộ nhận diện thương hiệu với AI ngay bây giờ"
          action={
            <a href="/branding/wizard">
              <Button>
                <Sparkles className="w-4 h-4 mr-2" />
                Bắt đầu wizard
              </Button>
            </a>
          }
        />
      )}
    </div>
  );
}

interface StatCardProps {
  label: string;
  value: number;
  icon: React.ReactNode;
}

function StatCard({ label, value, icon }: StatCardProps) {
  return (
    <div className="flex items-center gap-4">
      <div className="p-3 bg-muted/50 rounded-xl">{icon}</div>
      <div>
        <p className="text-2xl font-bold">{value}</p>
        <p className="text-sm text-muted-foreground">{label}</p>
      </div>
    </div>
  );
}
