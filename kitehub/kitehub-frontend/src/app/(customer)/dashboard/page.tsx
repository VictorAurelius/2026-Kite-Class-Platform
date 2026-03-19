'use client';

import Link from 'next/link';
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
  ArrowRight, Building2, Palette, CreditCard, Clock, Sparkles,
  TrendingUp, Smartphone, Zap, CheckCircle2, Circle, AlertTriangle,
  ExternalLink,
} from 'lucide-react';
import { getTenantUrl, getTenantDisplayUrl } from '@/lib/tenant-url';

export default function DashboardPage() {
  const { user } = useAuthStore();
  const { data: instances, isLoading, error, refetch } = useOwnerInstances(user?.id);

  const greeting = (() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Chào buổi sáng';
    if (hour < 18) return 'Chào buổi chiều';
    return 'Chào buổi tối';
  })();

  const firstInstance = instances?.[0];
  const hasTrialExpiringSoon = firstInstance?.isOnTrial
    && firstInstance?.trialDaysLeft != null
    && firstInstance.trialDaysLeft <= 3;

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6 sm:p-8">
        <h1 className="text-2xl font-bold">
          {greeting}, {user?.name ?? user?.email?.split('@')[0]} 👋
        </h1>
        <p className="mt-1 text-muted-foreground">
          Chúc bạn một ngày hiệu quả! Đây là tổng quan trung tâm của bạn.
        </p>

        {/* Quick actions */}
        <div className="mt-5 flex flex-wrap gap-3">
          <Link
            href="/branding"
            className="inline-flex items-center gap-2 rounded-xl border bg-card px-4 py-2 text-sm font-medium hover:border-primary hover:text-primary transition-all shadow-soft"
          >
            <Palette className="h-4 w-4" />
            AI Branding
          </Link>
          <Link
            href="/billing"
            className="inline-flex items-center gap-2 rounded-xl border bg-card px-4 py-2 text-sm font-medium hover:border-primary hover:text-primary transition-all shadow-soft"
          >
            <CreditCard className="h-4 w-4" />
            Thanh toán
          </Link>
          <Link
            href="/settings"
            className="inline-flex items-center gap-2 rounded-xl border bg-card px-4 py-2 text-sm font-medium hover:border-primary hover:text-primary transition-all shadow-soft"
          >
            <Building2 className="h-4 w-4" />
            Cài đặt
          </Link>
        </div>
      </div>

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

        <Link
          href="/billing/upgrade"
          className="group relative rounded-2xl overflow-hidden border shadow-soft hover:shadow-soft-lg transition-all"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-primary/10 via-cyan-500/5 to-teal-500/10" />
          <div className="relative p-6">
            <div className="flex items-center gap-2 mb-3">
              <div className="rounded-xl bg-gradient-to-br from-primary to-teal-500 p-2.5 text-white">
                <TrendingUp className="h-5 w-5" />
              </div>
              <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-[10px] font-semibold text-primary uppercase">Nâng cấp</span>
            </div>
            <h3 className="text-lg font-bold">Mở rộng quy mô trung tâm</h3>
            <p className="mt-1.5 text-sm text-muted-foreground leading-relaxed">
              Không giới hạn học viên, đa chi nhánh, báo cáo nâng cao. Từ 199.000đ/tháng.
            </p>
            <span className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-primary group-hover:underline">
              Xem bảng giá <ArrowRight className="h-3.5 w-3.5 group-hover:translate-x-1 transition-transform" />
            </span>
          </div>
        </Link>
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
import type { Instance } from '@/types/instance';

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
    <Card className="shadow-soft">
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
