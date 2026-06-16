'use client';

/**
 * Admin Dashboard Page.
 *
 * Displays KiteHub admin statistics including:
 * - Instance counts by status
 * - Revenue metrics
 * - Pending payments
 * - Quick action links
 *
 * @author KiteHub Team
 * @since PR 5.8
 */

import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useAdminDashboard, useAdminPendingPayments } from '@/hooks/use-admin';
import { Building2, CheckCircle, Clock, XCircle, TrendingUp, CreditCard, ArrowRight, DollarSign, Inbox } from 'lucide-react';

/**
 * Format number as Vietnamese currency (VND).
 */
function formatVND(value: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
}

/**
 * Loading skeleton component.
 */
function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <div className="animate-pulse h-8 w-48 bg-muted rounded" />
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="animate-pulse h-32 bg-muted rounded-lg" />
        ))}
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        {[...Array(2)].map((_, i) => (
          <div key={i} className="animate-pulse h-48 bg-muted rounded-lg" />
        ))}
      </div>
    </div>
  );
}

export default function AdminDashboardPage() {
  const { data: stats, isLoading, error } = useAdminDashboard();
  // GAP-1440: pendingPayments is not part of the dashboard endpoint — derive the
  // count from the pending-payments list so the KPI shows real data, not 0.
  const { data: pendingPayments } = useAdminPendingPayments();
  const pendingPaymentsCount = pendingPayments?.length ?? 0;

  if (isLoading) {
    return <DashboardSkeleton />;
  }

  if (error) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <div className="mt-4 text-destructive">
          Đã xảy ra lỗi khi tải dữ liệu dashboard
        </div>
      </div>
    );
  }

  // GAP-1375: render an explicit empty state instead of a blank screen when the
  // API returns no stats (e.g. 204 / undefined on a brand-new platform). The
  // previous `return null` left the admin staring at a white page that looked
  // like a crash.
  if (!stats) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <Card className="shadow-soft">
          <CardContent className="flex flex-col items-center justify-center py-16 text-center">
            <div className="rounded-full bg-muted p-4 text-muted-foreground">
              <Inbox className="h-8 w-8" aria-hidden="true" />
            </div>
            <h2 className="mt-4 text-lg font-semibold">Chưa có dữ liệu thống kê</h2>
            <p className="mt-1 max-w-md text-sm text-muted-foreground">
              Hệ thống chưa ghi nhận instance hoặc giao dịch nào. Khi có trung tâm
              đăng ký, số liệu tổng quan sẽ hiển thị tại đây.
            </p>
            <Link
              href="/admin/instances"
              className="mt-6 inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
            >
              Quản lý Instance <ArrowRight className="h-4 w-4" />
            </Link>
          </CardContent>
        </Card>
      </div>
    );
  }

  const statCards = [
    { label: 'Tổng Instance', value: stats.totalInstances, icon: Building2, color: 'from-primary to-primary/70', bg: 'bg-primary/10 text-primary' },
    { label: 'Đang hoạt động', value: stats.activeInstances, icon: CheckCircle, color: 'from-green-500 to-green-600', bg: 'bg-green-50 dark:bg-green-950/30 text-green-600' },
    { label: 'Đang dùng thử', value: stats.trialInstances, icon: Clock, color: 'from-blue-500 to-blue-600', bg: 'bg-blue-50 dark:bg-blue-950/30 text-blue-600' },
    { label: 'Tạm ngưng', value: stats.suspendedInstances, icon: XCircle, color: 'from-red-500 to-red-600', bg: 'bg-red-50 dark:bg-red-950/30 text-red-600' },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Dashboard</h1>

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {statCards.map((s) => (
          <Card key={s.label} className="shadow-soft hover:shadow-soft-lg transition-all">
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-muted-foreground">{s.label}</p>
                  <p className="mt-1 text-3xl font-bold">{s.value}</p>
                </div>
                <div className={`rounded-xl p-3 ${s.bg}`}>
                  <s.icon className="h-5 w-5" />
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Revenue & Quick Actions */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card className="shadow-soft">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <DollarSign className="h-5 w-5 text-primary" />
              Doanh thu
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {[
              { label: 'Tổng doanh thu', value: formatVND(stats.totalRevenue), color: '' },
              { label: 'Doanh thu tháng này', value: formatVND(stats.monthlyRevenue), color: '' },
              { label: 'Thanh toán chờ xác nhận', value: String(pendingPaymentsCount), color: 'text-orange-600 dark:text-orange-400' },
            ].map((r) => (
              <div key={r.label} className="flex justify-between items-center py-1">
                <span className="text-sm text-muted-foreground">{r.label}</span>
                <span className={`text-lg font-semibold ${r.color}`}>{r.value}</span>
              </div>
            ))}
          </CardContent>
        </Card>

        <Card className="shadow-soft">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5 text-primary" />
              Tổng quan
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between items-center py-1">
              <span className="text-sm text-muted-foreground">Instance mới trong tháng</span>
              <span className="text-lg font-semibold text-green-600 dark:text-green-400">{stats.newInstancesThisMonth}</span>
            </div>

            <div className="pt-2 space-y-2">
              <Link
                href="/admin/instances"
                className="flex items-center justify-center gap-2 w-full px-4 py-2.5 bg-primary text-primary-foreground rounded-xl hover:bg-primary/90 transition-colors text-sm font-semibold"
              >
                Quản lý Instance <ArrowRight className="h-4 w-4" />
              </Link>
              <Link
                href="/admin/payments"
                className="flex items-center justify-center gap-2 w-full px-4 py-2.5 border-2 rounded-xl hover:border-primary hover:text-primary transition-colors text-sm font-semibold"
              >
                <CreditCard className="h-4 w-4" />
                Xem Thanh toán
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
