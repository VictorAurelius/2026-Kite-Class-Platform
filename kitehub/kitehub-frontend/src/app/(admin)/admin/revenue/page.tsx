'use client';

/**
 * Admin Revenue Page.
 *
 * GAP-1441: previously a static stub hardcoding "0đ" with no data fetch — the
 * backend `/api/platform/admin/revenue` (kitehub-admin
 * `AnalyticsService.getRevenueReport()`) was orphaned. Now wired to
 * `useAdminRevenue` (current-month range) and renders real totalRevenue + MRR +
 * a daily-revenue chart + revenue-by-tier breakdown.
 */

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useAdminRevenue } from '@/hooks/use-admin';
import { DollarSign, TrendingUp, Calendar } from 'lucide-react';

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

/** Current-month date range as ISO `yyyy-MM-dd` strings (BE default range). */
function currentMonthRange(): { startDate: string; endDate: string } {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);
  const toIso = (d: Date) => d.toISOString().slice(0, 10);
  return { startDate: toIso(start), endDate: toIso(now) };
}

const PERIOD_LABEL_VI: Record<string, string> = {
  DAILY: 'Ngày',
  WEEKLY: 'Tuần',
  MONTHLY: 'Tháng',
  QUARTERLY: 'Quý',
  YEARLY: 'Năm',
};

function RevenueSkeleton() {
  return (
    <div className="space-y-6">
      <div className="animate-pulse h-8 w-40 bg-muted rounded" />
      <div className="grid gap-4 md:grid-cols-3">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="animate-pulse h-28 bg-muted rounded-lg" />
        ))}
      </div>
      <div className="animate-pulse h-64 bg-muted rounded-lg" />
    </div>
  );
}

export default function RevenuePage() {
  const { startDate, endDate } = currentMonthRange();
  const { data: report, isLoading, error } = useAdminRevenue('MONTHLY', startDate, endDate);

  if (isLoading) {
    return <RevenueSkeleton />;
  }

  if (error) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold">Doanh thu</h1>
        <div className="text-destructive">Đã xảy ra lỗi khi tải dữ liệu doanh thu</div>
      </div>
    );
  }

  const totalRevenue = report?.totalRevenue ?? 0;
  const mrr = report?.mrr ?? 0;
  const periodLabel = PERIOD_LABEL_VI[report?.period ?? 'MONTHLY'] ?? 'Tháng';
  const dailyRevenue = report?.dailyRevenue ?? [];
  const revenueByTier = report?.revenueByTier ?? [];
  const maxDaily = dailyRevenue.reduce((max, d) => Math.max(max, d.revenue), 0);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Doanh thu</h1>

      <div className="grid gap-4 md:grid-cols-3">
        <Card className="shadow-soft">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Tổng doanh thu</p>
                <p className="mt-1 text-3xl font-bold">{formatVND(totalRevenue)}</p>
              </div>
              <div className="rounded-xl bg-primary/10 p-3 text-primary">
                <DollarSign className="h-5 w-5" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="shadow-soft">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Doanh thu định kỳ (MRR)</p>
                <p className="mt-1 text-3xl font-bold">{formatVND(mrr)}</p>
              </div>
              <div className="rounded-xl bg-green-50 dark:bg-green-950/30 p-3 text-green-600">
                <TrendingUp className="h-5 w-5" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="shadow-soft">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Kỳ thanh toán</p>
                <p className="mt-1 text-3xl font-bold">{periodLabel}</p>
              </div>
              <div className="rounded-xl bg-blue-50 dark:bg-blue-950/30 p-3 text-blue-600">
                <Calendar className="h-5 w-5" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Daily revenue chart */}
      <Card className="shadow-soft">
        <CardHeader>
          <CardTitle>Biểu đồ doanh thu theo ngày</CardTitle>
        </CardHeader>
        <CardContent>
          {dailyRevenue.length === 0 || maxDaily === 0 ? (
            <div className="flex items-center justify-center h-64 rounded-xl bg-muted/30 border border-dashed">
              <p className="text-muted-foreground text-sm">
                Chưa có dữ liệu doanh thu trong kỳ này
              </p>
            </div>
          ) : (
            <div className="flex items-end gap-1 h-64" role="img" aria-label="Biểu đồ doanh thu theo ngày">
              {dailyRevenue.map((d) => (
                <div
                  key={d.date}
                  className="flex-1 bg-primary/70 hover:bg-primary rounded-t transition-colors"
                  style={{ height: `${Math.max((d.revenue / maxDaily) * 100, 1)}%` }}
                  title={`${d.date}: ${formatVND(d.revenue)}`}
                />
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Revenue by tier */}
      <Card className="shadow-soft">
        <CardHeader>
          <CardTitle>Doanh thu theo gói</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {revenueByTier.length === 0 ? (
            <p className="text-muted-foreground text-sm">Chưa có doanh thu theo gói</p>
          ) : (
            revenueByTier.map((t) => (
              <div key={t.tier} className="flex justify-between items-center py-1">
                <span className="text-sm text-muted-foreground">
                  {t.tier} <span className="text-xs">({t.subscriptionCount} gói)</span>
                </span>
                <span className="text-lg font-semibold">{formatVND(t.revenue)}</span>
              </div>
            ))
          )}
        </CardContent>
      </Card>
    </div>
  );
}
