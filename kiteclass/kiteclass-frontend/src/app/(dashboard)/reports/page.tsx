/**
 * Reports dashboard — Owner top-level revenue + attendance analytics (GAP-865).
 *
 * Consumes GAP-775 ReportController:
 *   - GET /api/v1/reports/revenue?months=12   → "Doanh thu tháng" KPI + 12-month chart
 *   - GET /api/v1/reports/attendance?months=12 → "Tỷ lệ điểm danh" KPI + 12-month chart
 *
 * Both endpoints are `hasRole('ADMIN')`; this page is Owner/admin-only —
 * non-admin sees a friendly "không có quyền" notice (FE role guard mirrors
 * BE role gate). Tenant scope handled by apiClient X-Tenant-Id interceptor.
 *
 * @author KiteClass Team
 * @since GAP-865 (Wave local-doable campaign)
 */

'use client';

import { TrendingUp, ClipboardCheck, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { MonthlyBarChart } from '@/components/reports/monthly-bar-chart';
import { useRevenueReport, useAttendanceReport } from '@/hooks/use-reports';
import { useAuthStore } from '@/stores/auth-store';
import { UserType } from '@/types/auth';

const VND_FORMATTER = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
});

const formatVnd = (n: number | null | undefined): string => {
  if (n === null || n === undefined) return '0đ';
  return VND_FORMATTER.format(n).replace(/\s?₫/, 'đ');
};

/** `92.5` → `92,5%` (VN decimal comma). */
const formatPercent = (n: number | null | undefined): string => {
  if (n === null || n === undefined) return '0%';
  return `${n.toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%`;
};

export default function ReportsPage() {
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.userType === UserType.ADMIN;

  const {
    data: revenue,
    isLoading: revenueLoading,
    error: revenueError,
  } = useRevenueReport(12);

  const {
    data: attendance,
    isLoading: attendanceLoading,
    error: attendanceError,
  } = useAttendanceReport(12);

  // FE role guard — mirror BE hasRole('ADMIN'). Render guard only after auth
  // hydration (user resolved); while user is null we let the queries run and
  // rely on BE 403 + error state below.
  if (user && !isAdmin) {
    return (
      <div className="container mx-auto space-y-6 py-8">
        <Card>
          <CardContent className="flex flex-col items-center justify-center gap-3 py-16 text-center">
            <AlertCircle className="h-10 w-10 text-muted-foreground" />
            <h2 className="text-lg font-semibold">Không có quyền truy cập</h2>
            <p className="max-w-md text-sm text-muted-foreground">
              Trang Báo cáo dành cho chủ trung tâm (quản trị viên). Tài khoản của
              bạn không có quyền xem doanh thu và tỷ lệ điểm danh toàn trung tâm.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto space-y-6 py-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold">Báo cáo</h1>
        <p className="text-muted-foreground mt-1">
          Tổng quan doanh thu và tỷ lệ điểm danh toàn trung tâm trong 12 tháng gần nhất.
        </p>
      </div>

      {/* KPI cards */}
      <div className="grid gap-4 md:grid-cols-2">
        {/* Revenue KPI */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Tổng doanh thu (12 tháng)
            </CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {revenueLoading ? (
              <div className="h-8 w-32 animate-pulse rounded bg-muted" />
            ) : revenueError ? (
              <p className="text-sm text-destructive">Không thể tải doanh thu.</p>
            ) : (
              <div className="text-2xl font-bold tabular-nums">
                {formatVnd(revenue?.totalRevenue)}
              </div>
            )}
            <p className="text-xs text-muted-foreground mt-1">
              Tổng thanh toán đã hoàn tất trong cửa sổ 12 tháng
            </p>
          </CardContent>
        </Card>

        {/* Attendance KPI */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Tỷ lệ điểm danh (12 tháng)
            </CardTitle>
            <ClipboardCheck className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {attendanceLoading ? (
              <div className="h-8 w-24 animate-pulse rounded bg-muted" />
            ) : attendanceError ? (
              <p className="text-sm text-destructive">Không thể tải điểm danh.</p>
            ) : (
              <div className="text-2xl font-bold tabular-nums">
                {formatPercent(attendance?.overallPresentRate)}
              </div>
            )}
            <p className="text-xs text-muted-foreground mt-1">
              Tỷ lệ buổi có mặt trên tổng số buổi điểm danh
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Revenue chart */}
      <Card>
        <CardHeader>
          <CardTitle>Doanh thu theo tháng</CardTitle>
        </CardHeader>
        <CardContent>
          {revenueLoading && (
            <div className="h-[240px] animate-pulse rounded bg-muted" />
          )}
          {revenueError && (
            <p className="text-sm text-destructive">
              Không thể tải biểu đồ doanh thu. Vui lòng thử lại.
            </p>
          )}
          {revenue && !revenueLoading && !revenueError && (
            <MonthlyBarChart
              data={revenue.points.map((p) => ({ month: p.month, value: p.amount }))}
              formatValue={formatVnd}
              color="rgb(37, 99, 235)"
              emptyHint="Chưa có dữ liệu doanh thu trong 12 tháng gần nhất"
            />
          )}
        </CardContent>
      </Card>

      {/* Attendance chart */}
      <Card>
        <CardHeader>
          <CardTitle>Tỷ lệ điểm danh theo tháng</CardTitle>
        </CardHeader>
        <CardContent>
          {attendanceLoading && (
            <div className="h-[240px] animate-pulse rounded bg-muted" />
          )}
          {attendanceError && (
            <p className="text-sm text-destructive">
              Không thể tải biểu đồ điểm danh. Vui lòng thử lại.
            </p>
          )}
          {attendance && !attendanceLoading && !attendanceError && (
            <MonthlyBarChart
              data={attendance.points.map((p) => ({
                month: p.month,
                value: p.presentRate,
              }))}
              formatValue={formatPercent}
              color="rgb(34, 197, 94)"
              emptyHint="Chưa có dữ liệu điểm danh trong 12 tháng gần nhất"
            />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
