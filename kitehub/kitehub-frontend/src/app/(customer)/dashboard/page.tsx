'use client';

import Link from 'next/link';
import dynamic from 'next/dynamic';
import { useMemo } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { StatusBadge } from '@/components/common/StatusBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { EmptyState } from '@/components/common/EmptyState';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { formatDate } from '@/lib/utils';
import {
  ArrowRight, Palette, CreditCard, Clock, Sparkles,
  TrendingUp, Smartphone, Zap, CheckCircle2, Circle, AlertTriangle,
  ExternalLink, HelpCircle, Search, Receipt, Server, Users, RefreshCw,
} from 'lucide-react';
import { getTenantUrl, getTenantDisplayUrl } from '@/lib/tenant-url';
import { OpenSchoolManagementButton } from '@/components/sso/OpenSchoolManagementButton';
import { useOnboardingWizard } from '@/components/onboarding/OnboardingWizard';
import { OnboardingDashboardCTA } from '@/components/onboarding-checklist';
import {
  KPICard,
  useCommandPalette,
} from '@/_shared/dashboard-foundation';
import type { KPIData } from '@/_shared/dashboard-foundation';
import type { Instance } from '@/types/instance';

// GAP-236 Sub-PR B — lazy-load the onboarding wizard. It's conditional
// (`shouldShow`), so we only ship its bundle when first-run users hit the
// dashboard. Hook that drives `shouldShow` stays static (small).
const OnboardingWizard = dynamic(
  () => import('@/components/onboarding/OnboardingWizard').then((m) => ({ default: m.OnboardingWizard })),
  { ssr: false }
);

/**
 * Subscription health snapshot. Wave 31 Bucket A ships with a derived-from-
 * Instance shim because `/api/subscription/health` doesn't exist yet
 * (TODO follow-up: file gap to expose a composite endpoint that returns
 * tier + usage % + 7-day series in one round-trip). Today we synthesize
 * what we can from the owner's instances list and stub the sparklines so
 * the layout is ready when real data lands.
 */
interface SubscriptionHealth {
  /** Tier label of the primary instance — drives the badge in the tier card. */
  tier: string;
  /** Days remaining on trial (null if no active trial). */
  trialDaysLeft: number | null;
  /** Subscription expires-at iso string (null if trial-only / no sub). */
  subscriptionExpiresAt: string | null;
  /** Active instances count. */
  activeInstances: number;
  /** Total instances under owner (active + suspended + expired). */
  totalInstances: number;
  /** Stub 7-day sparkline series — to be replaced by real telemetry. */
  series: {
    activeClasses: number[];
    instances: number[];
    apiCalls: number[];
  };
}

function buildHealthSnapshot(instances: Instance[] | undefined): SubscriptionHealth {
  const list = instances ?? [];
  const primary = list[0];
  const activeInstances = list.filter((i) => i.isActive).length;

  return {
    tier: primary?.tier ?? 'FREE',
    // Chỉ coi là trial khi instance thực sự đang trial — API trả trialDaysLeft=0 (không null)
    // cho tenant PREMIUM/ACTIVE → tránh render "gói thử nghiệm" sai (GAP-1090 residual).
    trialDaysLeft: primary?.isOnTrial ? (primary.trialDaysLeft ?? null) : null,
    subscriptionExpiresAt: primary?.subscriptionExpiresAt ?? null,
    activeInstances,
    totalInstances: list.length,
    // TODO(wave-31-followup): replace stub series with real /api/subscription/health response
    series: {
      activeClasses: [42, 48, 51, 47, 55, 58, 62],
      instances: [list.length, list.length, list.length, list.length, list.length, list.length, list.length],
      apiCalls: [820, 920, 1100, 980, 1240, 1420, 1620],
    },
  };
}

function buildKpis(health: SubscriptionHealth): KPIData[] {
  const tierTone: KPIData['tone'] =
    health.tier === 'ENTERPRISE' || health.tier === 'PREMIUM'
      ? 'positive'
      : health.trialDaysLeft != null && health.trialDaysLeft <= 3
        ? 'warning'
        : 'neutral';

  const lastClasses = health.series.activeClasses[health.series.activeClasses.length - 1] ?? 0;
  const lastApi = health.series.apiCalls[health.series.apiCalls.length - 1] ?? 0;

  return [
    {
      label: 'Gói hiện tại',
      value: health.tier,
      tone: tierTone,
      icon: <Sparkles className="h-4 w-4" />,
      sparkline: [],
    },
    {
      label: 'Ngày còn lại',
      value:
        health.trialDaysLeft != null
          ? `${health.trialDaysLeft} ngày`
          : health.subscriptionExpiresAt
            ? '—'
            : '∞',
      tone: health.trialDaysLeft != null && health.trialDaysLeft <= 3 ? 'warning' : 'neutral',
      icon: <Clock className="h-4 w-4" />,
    },
    {
      label: 'Phiên bản hoạt động',
      value: `${health.activeInstances}/${Math.max(1, health.totalInstances)}`,
      tone: health.activeInstances > 0 ? 'positive' : 'neutral',
      icon: <Server className="h-4 w-4" />,
      sparkline: health.series.instances,
    },
    {
      label: 'Lớp đang vận hành',
      value: String(lastClasses),
      delta: 8.2,
      tone: 'positive',
      icon: <Users className="h-4 w-4" />,
      sparkline: health.series.activeClasses,
    },
    {
      label: 'Lượt gọi API · 7 ngày',
      value: String(lastApi),
      delta: 24.4,
      tone: 'positive',
      icon: <Zap className="h-4 w-4" />,
      sparkline: health.series.apiCalls,
    },
    {
      label: 'Quota làm lại brand',
      value: '7/10',
      tone: 'neutral',
      icon: <RefreshCw className="h-4 w-4" />,
    },
  ];
}

export default function DashboardPage() {
  const { user } = useAuthStore();
  const { data: instances, isLoading, error, refetch } = useOwnerInstances(user?.id);
  const { shouldShow, showWizard, hideWizard } = useOnboardingWizard();
  const palette = useCommandPalette();

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Chào buổi sáng';
    if (hour < 18) return 'Chào buổi chiều';
    return 'Chào buổi tối';
  }, []);

  const health = useMemo(() => buildHealthSnapshot(instances), [instances]);
  const kpis = useMemo(() => buildKpis(health), [health]);

  const firstInstance = instances?.[0];
  const hasTrialExpiringSoon = firstInstance?.isOnTrial
    && firstInstance?.trialDaysLeft != null
    && firstInstance.trialDaysLeft <= 3;

  return (
    <div className="space-y-8">
      {/* Onboarding Wizard — lazy-loaded; render only when actually visible
          so the chunk does not download for returning users. */}
      {firstInstance && shouldShow && !isLoading && (
        <OnboardingWizard
          instance={firstInstance}
          open={true}
          onClose={hideWizard}
        />
      )}

      {/* Welcome Banner — kitehub-pro v2 sticky header pattern */}
      <header
        className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6 sm:p-8"
        data-testid="dashboard-header"
      >
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">
              {greeting}, {user?.name ?? user?.email?.split('@')[0]} 👋
            </h1>
            <p className="mt-1 text-muted-foreground">
              Đây là tổng quan tài khoản KiteHub của bạn — gói {health.tier}, {health.activeInstances} phiên bản hoạt động.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={palette.toggle}
              aria-label="Mở bảng lệnh ⌘K"
              data-testid="open-command-palette"
              className="inline-flex items-center gap-2 rounded-xl border bg-card px-4 py-2 text-sm font-medium hover:border-primary hover:text-primary transition-all shadow-soft"
            >
              <Search className="h-4 w-4" />
              <span>Tìm nhanh</span>
              <kbd className="ml-2 rounded border bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">⌘K</kbd>
            </button>
            <Link
              href="/branding"
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-primary to-accent px-4 py-2 text-sm font-semibold text-primary-foreground hover:shadow-lg transition-all"
            >
              <Sparkles className="h-4 w-4" />
              Tạo brand mới
            </Link>
          </div>
        </div>
      </header>

      {/* GAP-559 (Wave 79 Bucket D) — Onboarding entry-point CTA card.
          Auto-hides when checklist reaches 100% so returning Owners
          don't see a redundant nudge. */}
      <OnboardingDashboardCTA />

      {/* KPI strip — Wave 31 Bucket A: 6 KPI cards spec-mapped from
          kitehub-pro v2 dashboard-default.html § "Chỉ số chính" */}
      <section aria-label="Chỉ số chính" data-testid="kpi-strip">
        <div className="mb-3 text-xs font-medium uppercase tracking-wider text-muted-foreground">
          Chỉ số chính · 7 ngày qua
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          {kpis.map((kpi) => (
            <KPICard key={kpi.label} {...kpi} />
          ))}
        </div>
      </section>

      {/* Tier-status card — kitehub-pro v2 trial-banner equivalent */}
      <Card
        className="border-l-4 border-l-primary shadow-soft"
        data-testid="tier-status-card"
      >
        <CardContent className="flex flex-wrap items-center justify-between gap-4 p-5">
          <div className="flex items-start gap-4">
            <div className="rounded-xl bg-primary/10 p-3 text-primary">
              <CreditCard className="h-5 w-5" />
            </div>
            <div>
              <div className="text-sm font-semibold">
                Gói hiện tại: <span className="text-primary">{health.tier}</span>
              </div>
              <div className="mt-1 text-sm text-muted-foreground">
                {health.trialDaysLeft != null
                  ? `Bạn đang dùng gói thử nghiệm — còn ${health.trialDaysLeft} ngày để trải nghiệm đầy đủ tính năng PRO.`
                  : health.subscriptionExpiresAt
                    ? `Gói hiện tại sẽ gia hạn vào ${formatDate(health.subscriptionExpiresAt)}.`
                    : 'Bạn đang dùng gói cơ bản. Nâng cấp để mở khoá tính năng nâng cao.'}
              </div>
            </div>
          </div>
          <div className="flex gap-2">
            <Link
              href="/billing"
              className="inline-flex items-center gap-2 rounded-xl border bg-card px-4 py-2 text-sm font-medium hover:bg-muted transition-colors"
            >
              <Receipt className="h-4 w-4" />
              Hóa đơn
            </Link>
            <Link
              href="/billing/upgrade"
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              <TrendingUp className="h-4 w-4" />
              Nâng cấp
            </Link>
          </div>
        </CardContent>
      </Card>

      {/* Trial Expiry Warning */}
      {hasTrialExpiringSoon && (
        <div className="flex items-start gap-3 rounded-2xl border border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-950/30 p-5">
          <div className="rounded-xl bg-orange-500/10 p-2.5 text-orange-600">
            <AlertTriangle className="h-5 w-5" />
          </div>
          <div className="flex-1">
            <h3 className="font-semibold text-orange-900 dark:text-orange-100">
              Trial sắp hết hạn
            </h3>
            <p className="mt-1 text-sm text-orange-700 dark:text-orange-300">
              Trung tâm <strong>{firstInstance?.organizationName}</strong> còn <strong>{firstInstance?.trialDaysLeft} ngày</strong> dùng thử.
              Nâng cấp gói để tiếp tục sử dụng không gián đoạn.
            </p>
            <Link
              href="/billing/upgrade"
              className="mt-3 inline-flex items-center gap-2 rounded-xl bg-orange-600 px-4 py-2 text-sm font-semibold text-white hover:bg-orange-700 transition-colors"
            >
              <CreditCard className="h-4 w-4" />
              Nâng cấp ngay
            </Link>
          </div>
        </div>
      )}

      {/* Setup Checklist */}
      {firstInstance && (
        <SetupChecklist instance={firstInstance} />
      )}

      {/* Content */}
      {isLoading && <LoadingSpinner className="mt-12" />}

      {error && (
        <ErrorAlert
          message="Không thể tải danh sách instance"
          onRetry={() => refetch()}
        />
      )}

      {instances && instances.length === 0 && (
        <EmptyState
          title="Chưa có trung tâm nào"
          description="Tạo trung tâm KiteClass đầu tiên để bắt đầu quản lý"
          action={
            <Button onClick={() => window.location.href = '/register'}>
              Tạo trung tâm mới
            </Button>
          }
        />
      )}

      {instances && instances.length > 0 && (
        <div>
          <h2 className="text-lg font-semibold mb-4">Trung tâm của bạn</h2>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {instances.map((instance) => (
              <div
                key={instance.id}
                className="group rounded-2xl border bg-card shadow-soft hover:shadow-soft-lg transition-all overflow-hidden"
              >
                <div className="h-1.5 bg-gradient-to-r from-primary to-accent" />
                <div className="p-5">
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <h3 className="font-semibold text-base truncate">{instance.organizationName}</h3>
                      <p className="text-sm text-muted-foreground mt-0.5">
                        {getTenantDisplayUrl(instance.subdomain)}
                      </p>
                    </div>
                    <div className="relative group/status">
                      <StatusBadge status={instance.status} />
                      {/* Tooltip */}
                      <div className="absolute right-0 top-full mt-1 z-10 hidden group-hover/status:block w-48 rounded-lg border bg-popover p-2 text-xs text-muted-foreground shadow-md">
                        {instance.status === 'TRIAL' && 'Dùng thử miễn phí 14 ngày. Nâng cấp gói để tiếp tục.'}
                        {instance.status === 'ACTIVE' && 'Trung tâm đang hoạt động bình thường.'}
                        {instance.status === 'SUSPENDED' && 'Trung tâm bị tạm ngưng. Liên hệ hỗ trợ.'}
                        {instance.status === 'EXPIRED' && 'Gói đã hết hạn. Gia hạn để tiếp tục.'}
                      </div>
                    </div>
                  </div>

                  <div className="mt-4 flex items-center gap-3 text-xs text-muted-foreground">
                    <span className="inline-flex items-center gap-1 rounded-lg bg-primary/10 px-2 py-1 font-medium text-primary">
                      {instance.tier}
                    </span>
                    <span className="inline-flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {formatDate(instance.createdAt)}
                    </span>
                  </div>

                  {instance.isOnTrial && instance.trialExpiresAt && (
                    <div className="mt-3 flex items-center gap-2 rounded-xl bg-blue-50 dark:bg-blue-950/30 px-3 py-2 text-xs">
                      <div className="h-2 w-2 rounded-full bg-blue-500 animate-pulse" />
                      <span className="text-blue-700 dark:text-blue-300">
                        Trial còn <strong>{instance.trialDaysLeft} ngày</strong>
                      </span>
                    </div>
                  )}

                  {/* Action buttons */}
                  <div className="mt-4 flex gap-2">
                    <Link
                      href={`/instances/${instance.id}`}
                      className="flex-1 flex items-center justify-center gap-2 rounded-xl bg-primary py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors"
                    >
                      Vào quản lý
                      <ArrowRight className="h-4 w-4" />
                    </Link>
                    <a
                      href={getTenantUrl(instance.subdomain)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center justify-center rounded-xl border px-3 py-2.5 text-sm hover:bg-muted transition-colors"
                      title="Mở trang web trung tâm"
                    >
                      <ExternalLink className="h-4 w-4" />
                    </a>
                  </div>

                  {/* Cross-product SSO → KiteClass owner-shell (ADR-040, GAP-1138):
                      owner/staff vào quản lý nghiệp vụ trường không cần đăng nhập lại. */}
                  <OpenSchoolManagementButton className="mt-2" variant="outline" />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Promo Banners */}
      <div className="grid gap-5 sm:grid-cols-2">
        <Link
          href="/branding"
          className="group relative rounded-2xl overflow-hidden border shadow-soft hover:shadow-soft-lg transition-all"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-purple-500/10 via-pink-500/5 to-accent/10" />
          <div className="relative p-6">
            <div className="flex items-center gap-2 mb-3">
              <div className="rounded-xl bg-gradient-to-br from-purple-500 to-pink-500 p-2.5 text-white">
                <Sparkles className="h-5 w-5" />
              </div>
              <span className="rounded-full bg-purple-100 dark:bg-purple-900/30 px-2.5 py-0.5 text-[10px] font-semibold text-purple-700 dark:text-purple-300 uppercase">Mới</span>
            </div>
            <h3 className="text-lg font-bold">AI tạo website cho trung tâm</h3>
            <p className="mt-1.5 text-sm text-muted-foreground leading-relaxed">
              Upload logo → AI phân tích → tạo website chuyên nghiệp trong 5 phút.
            </p>
            <span className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-purple-600 dark:text-purple-400 group-hover:underline">
              Tạo website ngay <ArrowRight className="h-3.5 w-3.5 group-hover:translate-x-1 transition-transform" />
            </span>
          </div>
        </Link>

        <button
          type="button"
          onClick={showWizard}
          className="group relative rounded-2xl overflow-hidden border shadow-soft hover:shadow-soft-lg transition-all text-left"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-primary/10 via-cyan-500/5 to-teal-500/10" />
          <div className="relative p-6">
            <div className="flex items-center gap-2 mb-3">
              <div className="rounded-xl bg-gradient-to-br from-primary to-teal-500 p-2.5 text-white">
                <HelpCircle className="h-5 w-5" />
              </div>
              <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-[10px] font-semibold text-primary uppercase">Hướng dẫn</span>
            </div>
            <h3 className="text-lg font-bold">Xem lại hướng dẫn nhanh</h3>
            <p className="mt-1.5 text-sm text-muted-foreground leading-relaxed">
              Mở wizard onboarding 4 bước nếu bạn cần ôn lại quy trình thiết lập.
            </p>
            <span className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-primary group-hover:underline">
              Mở hướng dẫn <ArrowRight className="h-3.5 w-3.5 group-hover:translate-x-1 transition-transform" />
            </span>
          </div>
        </button>
      </div>

      {/* Tips Section */}
      <div className="rounded-2xl border bg-muted/20 p-6">
        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-4">Mẹo sử dụng KiteClass</h3>
        <div className="grid gap-4 sm:grid-cols-3">
          {[
            { icon: Zap, title: 'Thiết lập nhanh', desc: 'Thêm khóa học và giáo viên để bắt đầu điểm danh ngay.', href: '/settings' },
            { icon: Smartphone, title: 'Dùng trên điện thoại', desc: 'Mở trình duyệt điện thoại → truy cập trang quản lý.', href: '/settings' },
            { icon: Palette, title: 'Tạo website miễn phí', desc: 'AI sẽ tạo website từ logo trong 5 phút.', href: '/branding' },
          ].map((tip) => (
            <Link key={tip.title} href={tip.href} className="flex items-start gap-3 rounded-xl p-3 hover:bg-muted/50 transition-colors group">
              <div className="shrink-0 rounded-lg bg-primary/10 p-2 text-primary mt-0.5">
                <tip.icon className="h-4 w-4" />
              </div>
              <div>
                <p className="text-sm font-medium group-hover:text-primary transition-colors">{tip.title}</p>
                <p className="text-xs text-muted-foreground mt-0.5 leading-relaxed">{tip.desc}</p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}

// Setup Checklist Component
function SetupChecklist({ instance }: { instance: Instance }) {
  const steps = [
    { label: 'Đăng ký tài khoản', done: true, href: null },
    { label: 'Tạo trung tâm', done: true, href: null },
    { label: 'Tạo thương hiệu AI', done: false, href: '/branding' },
    { label: 'Truy cập trang web trung tâm', done: false, href: getTenantUrl(instance.subdomain) },
    { label: 'Nâng cấp gói (tùy chọn)', done: instance.tier !== 'FREE' && !instance.isOnTrial, href: '/billing/upgrade' },
  ];

  const completedCount = steps.filter(s => s.done).length;
  const allDone = completedCount === steps.length;

  // Don't show if all done
  if (allDone) return null;

  return (
    <Card className="shadow-soft" data-testid="setup-checklist">
      <CardContent className="pt-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold">Bắt đầu nhanh</h3>
          <span className="text-xs text-muted-foreground">
            {completedCount}/{steps.length} hoàn thành
          </span>
        </div>

        {/* Progress bar */}
        <div className="h-2 rounded-full bg-muted mb-4">
          <div
            className="h-2 rounded-full bg-primary transition-all"
            style={{ width: `${(completedCount / steps.length) * 100}%` }}
          />
        </div>

        <div className="space-y-2">
          {steps.map((step) => (
            <div key={step.label} className="flex items-center gap-3">
              {step.done ? (
                <CheckCircle2 className="h-5 w-5 text-green-500 shrink-0" />
              ) : (
                <Circle className="h-5 w-5 text-muted-foreground shrink-0" />
              )}
              {step.href && !step.done ? (
                <Link
                  href={step.href}
                  target={step.href.startsWith('http') ? '_blank' : undefined}
                  className="text-sm hover:text-primary transition-colors"
                >
                  {step.label}
                </Link>
              ) : (
                <span className={`text-sm ${step.done ? 'text-muted-foreground line-through' : ''}`}>
                  {step.label}
                </span>
              )}
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
