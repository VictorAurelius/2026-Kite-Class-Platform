'use client';

import Link from 'next/link';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { StatusBadge } from '@/components/common/StatusBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { EmptyState } from '@/components/common/EmptyState';
import { Button } from '@/components/ui/button';
import { formatDate } from '@/lib/utils';
import { ArrowRight, Building2, Palette, CreditCard, Clock, Sparkles, TrendingUp, Smartphone, Zap } from 'lucide-react';
import { getTenantDisplayUrl } from '@/lib/tenant-url';

export default function DashboardPage() {
  const { user } = useAuthStore();
  const { data: instances, isLoading, error, refetch } = useOwnerInstances(user?.id);

  const greeting = (() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Chào buổi sáng';
    if (hour < 18) return 'Chào buổi chiều';
    return 'Chào buổi tối';
  })();

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
          title="Chưa có instance nào"
          description="Tạo instance KiteClass đầu tiên để bắt đầu quản lý trung tâm giáo dục"
          action={
            <Button onClick={() => window.location.href = '/register'}>
              Tạo instance mới
            </Button>
          }
        />
      )}

      {instances && instances.length > 0 && (
        <>
          <div>
            <h2 className="text-lg font-semibold mb-4">Trung tâm của bạn</h2>
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {instances.map((instance) => (
                <div
                  key={instance.id}
                  className="group rounded-2xl border bg-card shadow-soft hover:shadow-soft-lg transition-all overflow-hidden"
                >
                  {/* Card header with gradient accent */}
                  <div className="h-1.5 bg-gradient-to-r from-primary to-accent" />

                  <div className="p-5">
                    {/* Title + Status */}
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <h3 className="font-semibold text-base truncate">{instance.organizationName}</h3>
                        <p className="text-sm text-muted-foreground mt-0.5">
                          {getTenantDisplayUrl(instance.subdomain)}
                        </p>
                      </div>
                      <StatusBadge status={instance.status} />
                    </div>

                    {/* Info row */}
                    <div className="mt-4 flex items-center gap-3 text-xs text-muted-foreground">
                      <span className="inline-flex items-center gap-1 rounded-lg bg-primary/10 px-2 py-1 font-medium text-primary">
                        {instance.tier}
                      </span>
                      <span className="inline-flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {formatDate(instance.createdAt)}
                      </span>
                    </div>

                    {/* Trial banner */}
                    {instance.isOnTrial && instance.trialExpiresAt && (
                      <div className="mt-3 flex items-center gap-2 rounded-xl bg-blue-50 dark:bg-blue-950/30 px-3 py-2 text-xs">
                        <div className="h-2 w-2 rounded-full bg-blue-500 animate-pulse" />
                        <span className="text-blue-700 dark:text-blue-300">
                          Trial còn <strong>{instance.trialDaysLeft} ngày</strong>
                        </span>
                      </div>
                    )}

                    {/* CTA */}
                    <Link
                      href={`/instances/${instance.id}`}
                      className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors"
                    >
                      Vào quản lý
                      <ArrowRight className="h-4 w-4" />
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </>
      )}

      {/* Promo Banners */}
      <div className="grid gap-5 sm:grid-cols-2">
        {/* AI Branding Promo */}
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
              Upload logo → AI phân tích → tạo website chuyên nghiệp trong 5 phút. Nhiều theme để lựa chọn.
            </p>
            <span className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-purple-600 dark:text-purple-400 group-hover:underline">
              Tạo website ngay <ArrowRight className="h-3.5 w-3.5 group-hover:translate-x-1 transition-transform" />
            </span>
          </div>
        </Link>

        {/* Upgrade Promo */}
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
