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
import { useAdminDashboard } from '@/hooks/use-admin';

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

  if (!stats) {
    return null;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Dashboard</h1>

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Tổng Instance
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">{stats.totalInstances}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Đang hoạt động
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-green-600">
              {stats.activeInstances}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Đang dùng thử
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-blue-600">
              {stats.trialInstances}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Tạm ngưng
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-red-600">
              {stats.suspendedInstances}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Revenue & Quick Actions */}
      <div className="grid gap-4 md:grid-cols-2">
        {/* Revenue Card */}
        <Card>
          <CardHeader>
            <CardTitle>Doanh thu</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-muted-foreground">Tổng doanh thu</span>
              <span className="text-xl font-semibold">
                {formatVND(stats.totalRevenue)}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-muted-foreground">Doanh thu tháng này</span>
              <span className="text-xl font-semibold">
                {formatVND(stats.monthlyRevenue)}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-muted-foreground">Thanh toán chờ xác nhận</span>
              <span className="text-xl font-semibold text-orange-600">
                {stats.pendingPayments}
              </span>
            </div>
          </CardContent>
        </Card>

        {/* New Instances & Quick Actions */}
        <Card>
          <CardHeader>
            <CardTitle>Tổng quan</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-muted-foreground">Instance mới trong tháng</span>
              <span className="text-xl font-semibold text-green-600">
                {stats.newInstancesThisMonth}
              </span>
            </div>

            {/* Quick Actions */}
            <div className="pt-4 space-y-2">
              <Link
                href="/admin/instances"
                className="block w-full px-4 py-2 text-center bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
              >
                Quản lý Instance
              </Link>
              <Link
                href="/admin/payments"
                className="block w-full px-4 py-2 text-center bg-secondary text-secondary-foreground rounded-md hover:bg-secondary/90 transition-colors"
              >
                Xem Thanh toán
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
