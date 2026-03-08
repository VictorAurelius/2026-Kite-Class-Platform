/**
 * Chart and statistics utilities.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import type {
  AttendanceStatsResponse,
  AttendanceTrendPoint,
  ClassAttendanceBreakdown,
  SystemAttendanceStats,
} from '@/types/attendance';

/**
 * Aggregate attendance statistics from multiple classes.
 *
 * @param statsArray - Array of class attendance statistics
 * @returns System-wide aggregated statistics
 */
export function aggregateStats(
  statsArray: AttendanceStatsResponse[]
): SystemAttendanceStats {
  const totalSessions = statsArray.reduce(
    (sum, s) => sum + (s?.totalSessions || 0),
    0
  );
  const presentCount = statsArray.reduce(
    (sum, s) => sum + (s?.presentCount || 0),
    0
  );
  const absentCount = statsArray.reduce(
    (sum, s) => sum + (s?.absentCount || 0),
    0
  );
  const lateCount = statsArray.reduce(
    (sum, s) => sum + (s?.lateCount || 0),
    0
  );
  const excusedCount = statsArray.reduce(
    (sum, s) => sum + (s?.excusedCount || 0),
    0
  );
  const makeupCount = statsArray.reduce(
    (sum, s) => sum + (s?.makeupCount || 0),
    0
  );

  const totalMarked =
    presentCount + absentCount + lateCount + excusedCount + makeupCount;
  const overallAttendanceRate =
    totalMarked > 0
      ? ((presentCount + lateCount + makeupCount) / totalMarked) * 100
      : 0;

  return {
    totalClasses: statsArray.length,
    totalSessions,
    totalStudents: statsArray.length, // Approximate
    overallAttendanceRate,
    presentCount,
    absentCount,
    lateCount,
    excusedCount,
    makeupCount,
  };
}

/**
 * Generate trends data from raw attendance records.
 * Groups records by date and calculates attendance rate.
 *
 * @param rawData - Array of attendance records
 * @returns Array of trend points sorted by date
 */
export function generateTrendsData(
  rawData: Array<{ markedDate: string; status: string }>
): AttendanceTrendPoint[] {
  // Group by date
  const dateMap = new Map<string, { present: number; total: number }>();

  rawData.forEach((record) => {
    const date = record.markedDate.split('T')[0]; // Extract date part
    const existing = dateMap.get(date) || { present: 0, total: 0 };

    existing.total += 1;
    if (
      record.status === 'PRESENT' ||
      record.status === 'LATE' ||
      record.status === 'MAKEUP'
    ) {
      existing.present += 1;
    }

    dateMap.set(date, existing);
  });

  // Convert to trend points array
  return Array.from(dateMap.entries())
    .map(([date, stats]) => ({
      date,
      attendanceRate:
        stats.total > 0 ? (stats.present / stats.total) * 100 : 0,
      presentCount: stats.present,
      totalSessions: stats.total,
    }))
    .sort((a, b) => a.date.localeCompare(b.date));
}

/**
 * Convert class statistics to breakdown format for tables.
 *
 * @param classId - Class ID
 * @param className - Class name
 * @param stats - Class attendance statistics
 * @param teacherName - Optional teacher name
 * @returns Class attendance breakdown
 */
export function toClassBreakdown(
  classId: number,
  className: string,
  stats: AttendanceStatsResponse,
  teacherName?: string
): ClassAttendanceBreakdown {
  return {
    classId,
    className,
    teacherName,
    totalSessions: stats.totalSessions,
    presentCount: stats.presentCount,
    absentCount: stats.absentCount,
    lateCount: stats.lateCount,
    excusedCount: stats.excusedCount,
    attendanceRate: stats.attendanceRate,
  };
}

/**
 * Calculate average attendance rate from array of stats.
 *
 * @param statsArray - Array of attendance statistics
 * @returns Average attendance rate (0-100)
 */
export function calculateAverageRate(
  statsArray: AttendanceStatsResponse[]
): number {
  if (statsArray.length === 0) return 0;

  const sum = statsArray.reduce((acc, s) => acc + (s?.attendanceRate || 0), 0);
  return sum / statsArray.length;
}

/**
 * Format date range for display.
 *
 * @param startDate - Start date (ISO string)
 * @param endDate - End date (ISO string)
 * @returns Formatted date range string
 */
export function formatDateRange(startDate: string, endDate: string): string {
  const start = new Date(startDate).toLocaleDateString('vi-VN');
  const end = new Date(endDate).toLocaleDateString('vi-VN');
  return `${start} - ${end}`;
}

/**
 * Get default date range (last 30 days).
 *
 * @returns Object with startDate and endDate (ISO strings)
 */
export function getDefaultDateRange(): { startDate: string; endDate: string } {
  const endDate = new Date();
  const startDate = new Date();
  startDate.setDate(startDate.getDate() - 30);

  return {
    startDate: startDate.toISOString().split('T')[0],
    endDate: endDate.toISOString().split('T')[0],
  };
}

/**
 * Calculate percentage change between two values.
 *
 * @param current - Current value
 * @param previous - Previous value
 * @returns Percentage change (positive or negative)
 */
export function calculatePercentageChange(
  current: number,
  previous: number
): number {
  if (previous === 0) return current > 0 ? 100 : 0;
  return ((current - previous) / previous) * 100;
}
