/**
 * Admin attendance statistics dashboard.
 * System-wide attendance analytics with trends and breakdowns.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import { useState, useMemo } from 'react';
import { Download, Calendar } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  DynamicAttendanceTrendsChart,
  DynamicClassStatsTable,
} from '@/components/attendance';
import {
  useSystemAttendanceStats,
  useAttendanceTrends,
  useClassAttendanceStats,
} from '@/hooks/use-attendance';
import { useAllActiveClasses } from '@/hooks/use-classes';
import { DashboardLayout } from '@/components/layout';
import { exportToCSV } from '@/lib/csv-export';
import { getDefaultDateRange } from '@/lib/chart-utils';
import type { ClassAttendanceBreakdown } from '@/types/attendance';

export default function AdminAttendanceStatsPage() {
  const defaultRange = getDefaultDateRange();
  const [startDate, setStartDate] = useState(defaultRange.startDate);
  const [endDate, setEndDate] = useState(defaultRange.endDate);

  // Fetch all active classes
  const { data: classes, isLoading: isLoadingClasses, error: classesError } = useAllActiveClasses();

  // Fetch system-wide stats
  const { data: systemStats, isLoading: isLoadingStats, error: statsError } =
    useSystemAttendanceStats({ startDate, endDate });

  // Fetch trends data
  const classIds = classes?.map((c) => c.id) || [];
  const { data: trends, isLoading: _isLoadingTrends } = useAttendanceTrends(
    classIds,
    { startDate, endDate }
  );

  // Fetch individual class stats (for breakdown table)
  // eslint-disable-next-line react-hooks/rules-of-hooks -- hooks-in-map is acknowledged pattern here
  const classStatsQueries = classes?.map((classItem) =>
    // eslint-disable-next-line react-hooks/rules-of-hooks
    useClassAttendanceStats(classItem.id)
  ) || [];

  // Combine class data with stats for breakdown
  // eslint-disable-next-line react-hooks/exhaustive-deps -- classStatsQueries uses hooks-in-map pattern
  const classBreakdown: ClassAttendanceBreakdown[] = useMemo(() => {
    if (!classes) return [];

    const breakdown: ClassAttendanceBreakdown[] = [];

    classes.forEach((classItem, index) => {
      const stats = classStatsQueries[index]?.data;
      if (!stats) return;

      breakdown.push({
        classId: classItem.id,
        className: classItem.name,
        teacherName: undefined,
        totalSessions: stats.totalSessions,
        presentCount: stats.presentCount,
        absentCount: stats.absentCount,
        lateCount: stats.lateCount,
        excusedCount: stats.excusedCount,
        attendanceRate: stats.attendanceRate,
      });
    });

    return breakdown;
  }, [classes, classStatsQueries]);

  // Handle CSV export
  const handleExport = () => {
    if (classBreakdown.length === 0) return;

    const exportData = classBreakdown.map((item) => ({
      'Lớp học': item.className,
      'Giáo viên': item.teacherName || '',
      'Tổng buổi': item.totalSessions,
      'Có mặt': item.presentCount,
      'Vắng': item.absentCount,
      'Đi trễ': item.lateCount,
      'Có phép': item.excusedCount,
      'Tỷ lệ (%)': item.attendanceRate.toFixed(1),
    }));

    exportToCSV(
      exportData,
      [
        'Lớp học',
        'Giáo viên',
        'Tổng buổi',
        'Có mặt',
        'Vắng',
        'Đi trễ',
        'Có phép',
        'Tỷ lệ (%)',
      ],
      `thong-ke-diem-danh-${new Date().toISOString().split('T')[0]}`
    );
  };

  const isLoading = isLoadingClasses || isLoadingStats;

  return (
    <DashboardLayout>
    <div className="container mx-auto space-y-6 py-8">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Thống kê điểm danh</h1>
          <p className="text-muted-foreground">
            Tổng quan và phân tích hệ thống điểm danh
          </p>
        </div>
        <Button onClick={handleExport} disabled={classBreakdown.length === 0}>
          <Download className="mr-2 h-4 w-4" />
          Xuất báo cáo CSV
        </Button>
      </div>

      {/* Error State */}
      {(classesError || statsError) && (
        <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
          <p className="text-sm text-destructive">Không thể tải dữ liệu. Vui lòng thử lại.</p>
        </div>
      )}

      {/* Date Range Filter */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="h-5 w-5" />
            Khoảng thời gian
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-4">
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium">Từ ngày:</label>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-[160px]"
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium">Đến ngày:</label>
              <Input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-[160px]"
              />
            </div>
            <Button
              variant="outline"
              onClick={() => {
                const range = getDefaultDateRange();
                setStartDate(range.startDate);
                setEndDate(range.endDate);
              }}
            >
              30 ngày gần nhất
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* System Stats Cards */}
      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-4">
          {['Tổng lớp học', 'Tổng buổi học', 'Tỷ lệ điểm danh TB', 'Tổng vắng'].map((label) => (
            <Card key={label}>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  {label}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="h-8 w-20 animate-pulse rounded bg-muted" />
                <div className="mt-2 h-3 w-24 animate-pulse rounded bg-muted" />
              </CardContent>
            </Card>
          ))}
        </div>
      ) : !systemStats && !classesError && !statsError ? (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <p className="text-lg font-medium text-muted-foreground">Chưa có dữ liệu</p>
          <p className="text-sm text-muted-foreground mt-1">Chưa có dữ liệu thống kê điểm danh nào.</p>
        </div>
      ) : systemStats ? (
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Tổng lớp học
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">{systemStats.totalClasses}</div>
              <p className="text-xs text-muted-foreground">Đang hoạt động</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Tổng buổi học
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">{systemStats.totalSessions}</div>
              <p className="text-xs text-muted-foreground">Đã điểm danh</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Tỷ lệ điểm danh TB
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold text-green-600">
                {systemStats.overallAttendanceRate.toFixed(1)}%
              </div>
              <p className="text-xs text-muted-foreground">Trung bình toàn hệ thống</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Tổng vắng
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold text-red-600">
                {systemStats.absentCount}
              </div>
              <p className="text-xs text-muted-foreground">Lượt học viên vắng</p>
            </CardContent>
          </Card>
        </div>
      ) : null}

      {/* Trends Chart — lazy-loaded; below the fold on initial paint */}
      <DynamicAttendanceTrendsChart
        data={trends || []}
        height={300}
        showGrid={true}
      />

      {/* Class Breakdown Table — lazy-loaded; chart + table both render on hydrate */}
      <DynamicClassStatsTable
        data={classBreakdown}
        isLoading={isLoadingClasses}
        sortBy="attendanceRate"
        sortOrder="desc"
      />
    </div>
    </DashboardLayout>
  );
}
