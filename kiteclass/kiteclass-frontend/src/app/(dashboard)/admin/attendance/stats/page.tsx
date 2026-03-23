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
import { AttendanceTrendsChart } from '@/components/attendance/attendance-trends-chart';
import { ClassStatsTable } from '@/components/attendance/class-stats-table';
import {
  useSystemAttendanceStats,
  useAttendanceTrends,
  useClassAttendanceStats,
} from '@/hooks/use-attendance';
import { useAllActiveClasses } from '@/hooks/use-classes';
import { exportToCSV } from '@/lib/csv-export';
import { getDefaultDateRange } from '@/lib/chart-utils';
import type { ClassAttendanceBreakdown } from '@/types/attendance';

export default function AdminAttendanceStatsPage() {
  const defaultRange = getDefaultDateRange();
  const [startDate, setStartDate] = useState(defaultRange.startDate);
  const [endDate, setEndDate] = useState(defaultRange.endDate);

  // Fetch all active classes
  const { data: classes, isLoading: isLoadingClasses } = useAllActiveClasses();

  // Fetch system-wide stats
  const { data: systemStats, isLoading: isLoadingStats } =
    useSystemAttendanceStats({ startDate, endDate });

  // Fetch trends data
  const classIds = classes?.map((c) => c.id) || [];
  const { data: trends, isLoading: _isLoadingTrends } = useAttendanceTrends(
    classIds,
    { startDate, endDate }
  );

  // Fetch individual class stats (for breakdown table)
  const classStatsQueries = classes?.map((classItem) =>
    // eslint-disable-next-line react-hooks/rules-of-hooks
    useClassAttendanceStats(classItem.id)
  ) || [];

  // Combine class data with stats for breakdown
  const classBreakdown: ClassAttendanceBreakdown[] = useMemo(() => {
    if (!classes) return [];

    const breakdown: ClassAttendanceBreakdown[] = [];

    classes.forEach((classItem, index) => {
      const stats = classStatsQueries[index]?.data;
      if (!stats) return;

      breakdown.push({
        classId: classItem.id,
        className: classItem.name,
        teacherName: classItem.teacherId ? `Teacher #${classItem.teacherId}` : undefined,
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
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-32 animate-pulse rounded-lg bg-muted" />
          ))}
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

      {/* Trends Chart */}
      <AttendanceTrendsChart
        data={trends || []}
        height={300}
        showGrid={true}
      />

      {/* Class Breakdown Table */}
      <ClassStatsTable
        data={classBreakdown}
        isLoading={isLoadingClasses}
        sortBy="attendanceRate"
        sortOrder="desc"
      />
    </div>
  );
}
