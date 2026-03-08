/**
 * Attendance table columns configuration.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import { ColumnDef } from '@tanstack/react-table';
import { Badge } from '@/components/ui/badge';
import {
  Attendance,
  AttendanceStatusLabels,
  AttendanceStatusColors,
} from '@/types/attendance';

/**
 * Get attendance history table columns.
 */
export const getAttendanceHistoryColumns = (): ColumnDef<Attendance>[] => [
  {
    accessorKey: 'markedDate',
    header: 'Ngày',
    cell: ({ row }) => {
      const date = new Date(row.original.markedDate);
      return (
        <div>
          <div className="font-medium">
            {date.toLocaleDateString('vi-VN')}
          </div>
          <div className="text-xs text-muted-foreground">
            {date.toLocaleTimeString('vi-VN', {
              hour: '2-digit',
              minute: '2-digit',
            })}
          </div>
        </div>
      );
    },
  },
  {
    accessorKey: 'sessionNumber',
    header: 'Buổi học',
    cell: ({ row }) => `Buổi ${row.original.sessionNumber || row.original.sessionId}`,
  },
  {
    accessorKey: 'status',
    header: 'Trạng thái',
    cell: ({ row }) => (
      <Badge className={AttendanceStatusColors[row.original.status]}>
        {AttendanceStatusLabels[row.original.status]}
      </Badge>
    ),
  },
  {
    accessorKey: 'notes',
    header: 'Ghi chú',
    cell: ({ row }) => (
      <div className="max-w-[300px] truncate">
        {row.original.notes || '—'}
      </div>
    ),
  },
  {
    accessorKey: 'markedByName',
    header: 'Điểm danh bởi',
    cell: ({ row }) => row.original.markedByName || '—',
  },
];

/**
 * Get student attendance summary columns (for class view).
 */
export const getStudentAttendanceSummaryColumns = (): ColumnDef<{
  studentName: string;
  totalSessions: number;
  presentCount: number;
  absentCount: number;
  lateCount: number;
  excusedCount: number;
  attendanceRate: number;
}>[] => [
  {
    accessorKey: 'studentName',
    header: 'Học viên',
  },
  {
    accessorKey: 'totalSessions',
    header: 'Tổng buổi',
  },
  {
    accessorKey: 'presentCount',
    header: 'Có mặt',
    cell: ({ row }) => (
      <span className="text-green-600">{row.original.presentCount}</span>
    ),
  },
  {
    accessorKey: 'absentCount',
    header: 'Vắng',
    cell: ({ row }) => (
      <span className="text-red-600">{row.original.absentCount}</span>
    ),
  },
  {
    accessorKey: 'lateCount',
    header: 'Đi trễ',
    cell: ({ row }) => (
      <span className="text-yellow-600">{row.original.lateCount}</span>
    ),
  },
  {
    accessorKey: 'excusedCount',
    header: 'Có phép',
    cell: ({ row }) => (
      <span className="text-blue-600">{row.original.excusedCount}</span>
    ),
  },
  {
    accessorKey: 'attendanceRate',
    header: 'Tỷ lệ',
    cell: ({ row }) => {
      const rate = row.original.attendanceRate;
      const color =
        rate >= 90
          ? 'text-green-600'
          : rate >= 75
          ? 'text-yellow-600'
          : 'text-red-600';
      return <span className={`font-semibold ${color}`}>{rate.toFixed(1)}%</span>;
    },
  },
];

/**
 * Get class attendance breakdown columns (for admin dashboard).
 */
export const getClassAttendanceBreakdownColumns = (): ColumnDef<{
  classId: number;
  className: string;
  teacherName?: string;
  totalSessions: number;
  presentCount: number;
  absentCount: number;
  lateCount: number;
  excusedCount: number;
  attendanceRate: number;
}>[] => [
  {
    accessorKey: 'className',
    header: 'Lớp học',
    cell: ({ row }) => (
      <div>
        <div className="font-medium">{row.original.className}</div>
        {row.original.teacherName && (
          <div className="text-xs text-muted-foreground">
            GV: {row.original.teacherName}
          </div>
        )}
      </div>
    ),
  },
  {
    accessorKey: 'totalSessions',
    header: 'Tổng buổi',
  },
  {
    accessorKey: 'presentCount',
    header: 'Có mặt',
    cell: ({ row }) => (
      <span className="text-green-600">{row.original.presentCount}</span>
    ),
  },
  {
    accessorKey: 'absentCount',
    header: 'Vắng',
    cell: ({ row }) => (
      <span className="text-red-600">{row.original.absentCount}</span>
    ),
  },
  {
    accessorKey: 'lateCount',
    header: 'Đi trễ',
    cell: ({ row }) => (
      <span className="text-yellow-600">{row.original.lateCount}</span>
    ),
  },
  {
    accessorKey: 'attendanceRate',
    header: 'Tỷ lệ điểm danh',
    cell: ({ row }) => {
      const rate = row.original.attendanceRate;
      const color =
        rate >= 90
          ? 'text-green-600'
          : rate >= 75
          ? 'text-yellow-600'
          : 'text-red-600';
      return (
        <span className={`font-semibold ${color}`}>
          {rate.toFixed(1)}%
        </span>
      );
    },
  },
];
