/**
 * Attendance statistics overview component with progress bar.
 * Wraps AttendanceStatsCards and adds attendance rate display.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { AttendanceStatsCards } from './attendance-stats-cards';
import type { AttendanceStatsResponse } from '@/types/attendance';

interface AttendanceStatsOverviewProps {
  stats: AttendanceStatsResponse;
  showProgress?: boolean;
  showMakeup?: boolean;
  variant?: 'default' | 'compact';
}

export function AttendanceStatsOverview({
  stats,
  showProgress = true,
  showMakeup = false,
  variant = 'default',
}: AttendanceStatsOverviewProps) {
  const attendanceRate = stats.attendanceRate || 0;
  const rateColor =
    attendanceRate >= 90
      ? 'text-green-600'
      : attendanceRate >= 75
      ? 'text-yellow-600'
      : 'text-red-600';

  const progressColor =
    attendanceRate >= 90
      ? 'bg-green-600'
      : attendanceRate >= 75
      ? 'bg-yellow-600'
      : 'bg-red-600';

  // Transform stats to match AttendanceStatsCards format
  const cardStats = {
    total: stats.totalSessions,
    present: stats.presentCount,
    absent: stats.absentCount,
    late: stats.lateCount,
    excused: stats.excusedCount,
    makeup: stats.makeupCount,
  };

  if (variant === 'compact') {
    return (
      <div className="space-y-4">
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-medium">
              Tỷ lệ điểm danh
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className={`text-3xl font-bold ${rateColor}`}>
              {attendanceRate.toFixed(1)}%
            </div>
            {showProgress && (
              <Progress
                value={attendanceRate}
                className="mt-2"
                indicatorClassName={progressColor}
              />
            )}
          </CardContent>
        </Card>
        <AttendanceStatsCards stats={cardStats} showMakeup={showMakeup} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {showProgress && (
        <Card>
          <CardHeader>
            <CardTitle>Tỷ lệ điểm danh</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">
                Tổng quan
              </span>
              <span className={`text-2xl font-bold ${rateColor}`}>
                {attendanceRate.toFixed(1)}%
              </span>
            </div>
            <Progress
              value={attendanceRate}
              className="h-3"
              indicatorClassName={progressColor}
            />
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>0%</span>
              <span>50%</span>
              <span>100%</span>
            </div>
          </CardContent>
        </Card>
      )}

      <div>
        <h3 className="mb-4 text-lg font-semibold">Chi tiết</h3>
        <AttendanceStatsCards stats={cardStats} showMakeup={showMakeup} />
      </div>
    </div>
  );
}
