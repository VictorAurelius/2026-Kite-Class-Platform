/**
 * Student attendance history page.
 * Shows student's attendance history with calendar, stats, and table.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import { use, useState } from 'react';
import Link from 'next/link';
import { ChevronLeft, Download } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { AttendanceStatsOverview } from '@/components/attendance/attendance-stats-overview';
import { EnhancedAttendanceCalendar } from '@/components/student/dynamic-attendance-calendar';
import { AttendanceHistoryTable } from '@/components/attendance/attendance-history-table';
import { AttendanceDetailDialog } from '@/components/student/dynamic-attendance-detail-dialog';
import { useStudent } from '@/hooks/use-students';
import { DashboardLayout } from '@/components/layout';
import {
  useStudentAttendanceStats,
  useAttendanceByEnrollment,
} from '@/hooks/use-attendance';
import { exportToCSV } from '@/lib/csv-export';
import type { Attendance } from '@/types/attendance';

interface PageProps {
  params: Promise<{ id: string }>;
}

export default function StudentAttendancePage({ params }: PageProps) {
  const { id } = use(params);
  const studentId = Number(id);

  const [page, setPage] = useState(0);
  const [selectedClass, setSelectedClass] = useState<string>('all');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedRecords, setSelectedRecords] = useState<Attendance[]>([]);

  // Fetch student data
  const { data: student, isLoading: isLoadingStudent } = useStudent(studentId);

  // Fetch attendance stats
  const { data: stats, isLoading: _isLoadingStats } =
    useStudentAttendanceStats(studentId);

  // Note: Using studentId as enrollmentId until enrollment API is available.
  // When enrollment endpoints are ready, fetch actual enrollment IDs from student's enrollments.
  const enrollmentId = studentId;

  // Fetch attendance history
  const {
    data: attendanceData,
    isLoading: isLoadingAttendance,
    error: attendanceError,
  } = useAttendanceByEnrollment(enrollmentId, {
    page,
    size: 20,
  });

  const attendanceRecords = attendanceData?.content || [];
  const totalElements = attendanceData?.totalElements || 0;

  // Handle calendar date click
  const handleDateClick = (date: Date, records: Attendance[]) => {
    setSelectedDate(date);
    setSelectedRecords(records);
    setDialogOpen(true);
  };

  // Handle CSV export
  const handleExport = () => {
    if (!attendanceRecords || attendanceRecords.length === 0) return;

    const exportData = attendanceRecords.map((record) => ({
      Ngày: new Date(record.markedDate).toLocaleDateString('vi-VN'),
      'Buổi học': record.sessionNumber || record.sessionId,
      'Trạng thái': record.status,
      'Ghi chú': record.notes || '',
      'Điểm danh bởi': record.markedByName || '',
    }));

    exportToCSV(
      exportData,
      ['Ngày', 'Buổi học', 'Trạng thái', 'Ghi chú', 'Điểm danh bởi'],
      `diem-danh-${student?.name || studentId}-${new Date().toISOString().split('T')[0]}`
    );
  };

  if (isLoadingStudent) {
    return (
      <DashboardLayout>
      <div className="container mx-auto py-8">
        <div className="mb-6 flex items-center gap-4">
          <div className="h-8 w-32 animate-pulse rounded bg-muted" />
        </div>
        <div className="space-y-6">
          <div className="h-48 animate-pulse rounded-lg bg-muted" />
          <div className="h-96 animate-pulse rounded-lg bg-muted" />
        </div>
      </div>
      </DashboardLayout>
    );
  }

  if (!student) {
    return (
      <DashboardLayout>
      <div className="container mx-auto py-8">
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <div className="mb-4 text-6xl">❌</div>
            <h3 className="mb-2 text-lg font-semibold">
              Không tìm thấy học viên
            </h3>
            <Link href="/students">
              <Button variant="link">Quay lại danh sách</Button>
            </Link>
          </CardContent>
        </Card>
      </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
    <div className="container mx-auto space-y-6 py-8">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="mb-2 flex items-center gap-2 text-sm text-muted-foreground">
            <Link href="/students" className="hover:underline">
              Học viên
            </Link>
            <span>/</span>
            <Link href={`/students/${studentId}`} className="hover:underline">
              {student.name}
            </Link>
            <span>/</span>
            <span>Điểm danh</span>
          </div>
          <h1 className="text-3xl font-bold">
            Lịch sử điểm danh - {student.name}
          </h1>
        </div>
        <div className="flex gap-2">
          <Link href={`/students/${studentId}`}>
            <Button variant="outline" size="sm">
              <ChevronLeft className="mr-2 h-4 w-4" />
              Quay lại
            </Button>
          </Link>
          <Button variant="outline" size="sm" onClick={handleExport}>
            <Download className="mr-2 h-4 w-4" />
            Xuất CSV
          </Button>
        </div>
      </div>

      {/* Stats Overview */}
      {stats && (
        <AttendanceStatsOverview
          stats={stats}
          showProgress={true}
          showMakeup={true}
          variant="default"
        />
      )}

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Bộ lọc</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-4">
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium">Lớp học:</label>
              <Select value={selectedClass} onValueChange={setSelectedClass}>
                <SelectTrigger className="w-[200px]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả</SelectItem>
                  {/* Class filter options will be populated when enrollment API is available */}
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Error State */}
      {attendanceError && (
        <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
          <p className="text-sm text-destructive">Không thể tải dữ liệu điểm danh. Vui lòng thử lại.</p>
        </div>
      )}

      {/* Empty State */}
      {!isLoadingAttendance && !attendanceError && attendanceData && attendanceRecords.length === 0 && (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <p className="text-lg font-medium text-muted-foreground">Chưa có dữ liệu điểm danh</p>
          <p className="text-sm text-muted-foreground mt-1">Học viên chưa có lịch sử điểm danh nào.</p>
        </div>
      )}

      {/* Calendar View */}
      {attendanceRecords.length > 0 && (
        <EnhancedAttendanceCalendar
          attendanceRecords={attendanceRecords}
          onDateClick={handleDateClick}
          showFilters={true}
          showTooltips={true}
        />
      )}

      {/* History Table */}
      <Card>
        <CardHeader>
          <CardTitle>Lịch sử chi tiết</CardTitle>
        </CardHeader>
        <CardContent>
          <AttendanceHistoryTable
            data={attendanceRecords}
            isLoading={isLoadingAttendance}
            totalElements={totalElements}
            page={page}
            size={20}
            onPageChange={setPage}
          />
        </CardContent>
      </Card>

      {/* Detail Dialog */}
      <AttendanceDetailDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        date={selectedDate}
        records={selectedRecords}
      />
    </div>
    </DashboardLayout>
  );
}
