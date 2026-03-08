/**
 * Class attendance statistics table component.
 * Displays per-class attendance breakdown for admin dashboard.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import { useMemo } from 'react';
import { DataTable } from '@/components/common/data-table';
import { getClassAttendanceBreakdownColumns } from '@/components/tables/columns/attendance-columns';
import type { ClassAttendanceBreakdown } from '@/types/attendance';

interface ClassStatsTableProps {
  data: ClassAttendanceBreakdown[];
  isLoading?: boolean;
  sortBy?: 'className' | 'attendanceRate' | 'totalSessions';
  sortOrder?: 'asc' | 'desc';
}

export function ClassStatsTable({
  data,
  isLoading = false,
  sortBy = 'attendanceRate',
  sortOrder = 'desc',
}: ClassStatsTableProps) {
  const columns = getClassAttendanceBreakdownColumns();

  // Sort data
  const sortedData = useMemo(() => {
    if (!data) return [];

    return [...data].sort((a, b) => {
      let comparison = 0;

      switch (sortBy) {
        case 'className':
          comparison = a.className.localeCompare(b.className);
          break;
        case 'attendanceRate':
          comparison = a.attendanceRate - b.attendanceRate;
          break;
        case 'totalSessions':
          comparison = a.totalSessions - b.totalSessions;
          break;
        default:
          comparison = 0;
      }

      return sortOrder === 'asc' ? comparison : -comparison;
    });
  }, [data, sortBy, sortOrder]);

  if (isLoading) {
    return (
      <div className="rounded-lg border">
        <div className="p-8">
          <div className="space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-12 animate-pulse rounded bg-muted" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (!sortedData || sortedData.length === 0) {
    return (
      <div className="rounded-lg border bg-card">
        <div className="flex flex-col items-center justify-center p-12 text-center">
          <div className="mb-4 text-6xl">📊</div>
          <h3 className="mb-2 text-lg font-semibold">
            Chưa có dữ liệu thống kê
          </h3>
          <p className="text-sm text-muted-foreground">
            Thống kê lớp học sẽ hiển thị tại đây khi có dữ liệu điểm danh
          </p>
        </div>
      </div>
    );
  }

  // Calculate summary statistics
  const summary = {
    totalClasses: sortedData.length,
    totalSessions: sortedData.reduce((sum, c) => sum + c.totalSessions, 0),
    avgAttendanceRate:
      sortedData.reduce((sum, c) => sum + c.attendanceRate, 0) / sortedData.length,
    bestClass: sortedData.reduce((best, current) =>
      current.attendanceRate > best.attendanceRate ? current : best
    ),
    worstClass: sortedData.reduce((worst, current) =>
      current.attendanceRate < worst.attendanceRate ? current : worst
    ),
  };

  return (
    <div className="space-y-4">
      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <div className="rounded-lg border bg-card p-4">
          <div className="text-sm text-muted-foreground">Tổng lớp học</div>
          <div className="text-2xl font-bold">{summary.totalClasses}</div>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <div className="text-sm text-muted-foreground">Tổng buổi học</div>
          <div className="text-2xl font-bold">{summary.totalSessions}</div>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <div className="text-sm text-muted-foreground">Tỷ lệ TB</div>
          <div className="text-2xl font-bold text-green-600">
            {summary.avgAttendanceRate.toFixed(1)}%
          </div>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <div className="text-sm text-muted-foreground">Cao nhất</div>
          <div className="text-xl font-bold text-green-600">
            {summary.bestClass.attendanceRate.toFixed(1)}%
          </div>
          <div className="text-xs text-muted-foreground">
            {summary.bestClass.className}
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="rounded-lg border bg-card">
        <div className="p-4">
          <h3 className="mb-4 font-semibold">Chi tiết theo lớp</h3>
          <DataTable columns={columns} data={sortedData} />
        </div>
      </div>
    </div>
  );
}
