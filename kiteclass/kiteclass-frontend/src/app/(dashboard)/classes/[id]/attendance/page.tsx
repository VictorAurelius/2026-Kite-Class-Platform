/**
 * Take attendance page for a specific class session.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

export const dynamic = 'force-dynamic';

import { use, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Save, CheckCircle2 } from 'lucide-react';
import Link from 'next/link';
import { DashboardLayout } from '@/components/layout';
import { LoadingSpinner, ErrorAlert } from '@/components/common';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Label } from '@/components/ui/label';
import { useClass, useClassSessions } from '@/hooks/use-classes';
import { useActiveEnrollmentsByClass } from '@/hooks/use-enrollments';
import { useMarkBulkAttendance } from '@/hooks/use-attendance';
import {
  AttendanceStatus,
  type AttendanceRecord,
} from '@/types/attendance';
import {
  AttendanceFormList,
  AttendanceStatsCards,
  type StudentAttendanceRow,
} from '@/components/attendance';
import { toast } from '@/hooks/use-toast';

export default function TakeAttendancePage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const classId = parseInt(id);
  const router = useRouter();

  // Fetch class sessions
  const { data: classData, isLoading: classLoading } = useClass(classId);
  const { data: sessions, isLoading: sessionsLoading } = useClassSessions(classId);
  const { data: enrollments, isLoading: enrollmentsLoading } =
    useActiveEnrollmentsByClass(classId, { size: 100 });

  // Selected session for attendance
  const [selectedSessionId, setSelectedSessionId] = useState<number | null>(null);

  const markBulkMutation = useMarkBulkAttendance();

  // Auto-select first session if available
  useState(() => {
    if (sessions && sessions.length > 0 && !selectedSessionId) {
      setSelectedSessionId(sessions[0].id);
    }
  });

  // Initialize attendance state from enrollments
  const [attendanceRows, setAttendanceRows] = useState<StudentAttendanceRow[]>(
    []
  );

  // Initialize rows when enrollments load
  useState(() => {
    if (enrollments?.content && attendanceRows.length === 0) {
      setAttendanceRows(
        enrollments.content.map((enrollment) => ({
          enrollmentId: enrollment.id,
          studentName: enrollment.studentName,
          status: AttendanceStatus.PRESENT,
          notes: '',
        }))
      );
    }
  });

  const handleStatusChange = (enrollmentId: number, status: AttendanceStatus) => {
    setAttendanceRows((rows) =>
      rows.map((row) =>
        row.enrollmentId === enrollmentId ? { ...row, status } : row
      )
    );
  };

  const handleNotesChange = (enrollmentId: number, notes: string) => {
    setAttendanceRows((rows) =>
      rows.map((row) =>
        row.enrollmentId === enrollmentId ? { ...row, notes } : row
      )
    );
  };

  const handleMarkAllPresent = () => {
    setAttendanceRows((rows) =>
      rows.map((row) => ({ ...row, status: AttendanceStatus.PRESENT }))
    );
    toast({
      title: 'Đã đánh dấu',
      description: 'Tất cả học viên được đánh dấu có mặt',
    });
  };

  const handleSaveAttendance = async () => {
    if (!selectedSessionId) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng chọn buổi học để điểm danh',
        variant: 'destructive',
      });
      return;
    }

    const records: AttendanceRecord[] = attendanceRows.map((row) => ({
      enrollmentId: row.enrollmentId,
      status: row.status,
      notes: row.notes || undefined,
    }));

    try {
      await markBulkMutation.mutateAsync({
        sessionId: selectedSessionId,
        records,
      });

      // Navigate back to class detail
      router.push(`/classes/${classId}`);
    } catch (_error) {
      // Error handled by mutation
    }
  };

  if (classLoading || enrollmentsLoading || sessionsLoading) {
    return (
      <DashboardLayout>
        <LoadingSpinner />
      </DashboardLayout>
    );
  }

  if (!classData) {
    return (
      <DashboardLayout>
        <ErrorAlert message="Không tìm thấy lớp học" />
      </DashboardLayout>
    );
  }

  if (!sessions || sessions.length === 0) {
    return (
      <DashboardLayout>
        <ErrorAlert message="Lớp học chưa có lịch học. Vui lòng tạo lịch học trước khi điểm danh." />
      </DashboardLayout>
    );
  }

  const stats = {
    total: attendanceRows.length,
    present: attendanceRows.filter((r) => r.status === AttendanceStatus.PRESENT).length,
    absent: attendanceRows.filter((r) => r.status === AttendanceStatus.ABSENT).length,
    late: attendanceRows.filter((r) => r.status === AttendanceStatus.LATE).length,
    excused: attendanceRows.filter((r) => r.status === AttendanceStatus.EXCUSED).length,
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href={`/classes/${classId}`}>
              <Button variant="outline" size="icon">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <div>
              <h1 className="text-2xl font-bold">Điểm danh</h1>
              <p className="text-muted-foreground">{classData.name}</p>
            </div>
          </div>

          <div className="flex gap-2">
            <Button variant="outline" onClick={handleMarkAllPresent}>
              <CheckCircle2 className="mr-2 h-4 w-4" />
              Đánh dấu tất cả có mặt
            </Button>
            <Button
              onClick={handleSaveAttendance}
              disabled={markBulkMutation.isPending}
            >
              <Save className="mr-2 h-4 w-4" />
              {markBulkMutation.isPending ? 'Đang lưu...' : 'Lưu điểm danh'}
            </Button>
          </div>
        </div>

        {/* Session Selector */}
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-4">
              <Label htmlFor="session-select" className="min-w-[100px] font-medium">
                Buổi học:
              </Label>
              <Select
                value={selectedSessionId?.toString() || ''}
                onValueChange={(value) => setSelectedSessionId(parseInt(value))}
              >
                <SelectTrigger id="session-select" className="w-full max-w-md">
                  <SelectValue placeholder="Chọn buổi học để điểm danh" />
                </SelectTrigger>
                <SelectContent>
                  {sessions?.map((session) => (
                    <SelectItem key={session.id} value={session.id.toString()}>
                      Buổi {session.sessionNumber} -{' '}
                      {new Date(session.scheduledDate).toLocaleDateString('vi-VN', {
                        weekday: 'long',
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                      })}
                      {session.status === 'COMPLETED' && ' (Đã hoàn thành)'}
                      {session.status === 'CANCELLED' && ' (Đã hủy)'}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* Stats */}
        <AttendanceStatsCards stats={stats} />

        {/* Attendance List */}
        <AttendanceFormList
          rows={attendanceRows}
          onStatusChange={handleStatusChange}
          onNotesChange={handleNotesChange}
        />
      </div>
    </DashboardLayout>
  );
}
