/**
 * Attendance reports page - shows attendance statistics and trends.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

export const dynamic = 'force-dynamic';

import { useState } from 'react';
import { ArrowLeft, Download, Users, TrendingUp, TrendingDown, CheckCircle } from 'lucide-react';
import Link from 'next/link';
import { DashboardLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Label } from '@/components/ui/label';
import { Progress } from '@/components/ui/progress';
import { useAllActiveClasses } from '@/hooks/use-classes';
import { useAttendanceByClass } from '@/hooks/use-attendance';
import { AttendanceStatusLabels } from '@/types/attendance';
import { AttendanceCalendar } from '@/components/attendance';

export default function AttendanceReportsPage() {
  const { data: classes = [] } = useAllActiveClasses();
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);

  // Fetch attendance data for selected class
  const { data: attendanceData } = useAttendanceByClass(
    selectedClassId || 0,
    { page: 0, size: 1000 },
    { enabled: !!selectedClassId }
  );

  // Calculate statistics
  const stats = attendanceData?.content
    ? {
        total: attendanceData.content.length,
        present: attendanceData.content.filter((a) => a.status === 'PRESENT').length,
        absent: attendanceData.content.filter((a) => a.status === 'ABSENT').length,
        late: attendanceData.content.filter((a) => a.status === 'LATE').length,
        excused: attendanceData.content.filter((a) => a.status === 'EXCUSED').length,
        makeup: attendanceData.content.filter((a) => a.status === 'MAKEUP').length,
      }
    : { total: 0, present: 0, absent: 0, late: 0, excused: 0, makeup: 0 };

  const presentRate = stats.total > 0 ? (stats.present / stats.total) * 100 : 0;
  const absentRate = stats.total > 0 ? (stats.absent / stats.total) * 100 : 0;

  // Group by student for student-level stats
  const studentStats = attendanceData?.content
    ? Object.values(
        attendanceData.content.reduce((acc, record) => {
          if (!acc[record.enrollmentId]) {
            acc[record.enrollmentId] = {
              studentName: record.studentName,
              total: 0,
              present: 0,
              absent: 0,
              late: 0,
              excused: 0,
              makeup: 0,
            };
          }
          acc[record.enrollmentId]!.total++;
          acc[record.enrollmentId]![record.status.toLowerCase() as keyof typeof acc[typeof record.enrollmentId]]++;
          return acc;
        }, {} as Record<number, { studentName: string; total: number; present: number; absent: number; late: number; excused: number; makeup: number }>)
      )
    : [];

  // Export to CSV
  const handleExport = () => {
    if (!attendanceData?.content || attendanceData.content.length === 0) {
      alert('Không có dữ liệu để xuất');
      return;
    }

    const csvHeaders = [
      'Học viên',
      'Buổi học',
      'Trạng thái',
      'Ngày điểm danh',
      'Ghi chú',
      'Điểm',
    ].join(',');

    const csvRows = attendanceData.content.map((record) =>
      [
        `"${record.studentName}"`,
        record.sessionNumber || '',
        AttendanceStatusLabels[record.status],
        new Date(record.markedDate).toLocaleDateString('vi-VN'),
        `"${(record.notes || '').replace(/"/g, '""')}"`,
        record.pointsAwarded,
      ].join(',')
    );

    const csv = [csvHeaders, ...csvRows].join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `bao-cao-diem-danh-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/attendance">
              <Button variant="outline" size="icon">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <div>
              <h1 className="text-3xl font-bold">Báo cáo điểm danh</h1>
              <p className="text-muted-foreground">
                Xem thống kê và xu hướng điểm danh
              </p>
            </div>
          </div>
          <Button onClick={handleExport} disabled={!selectedClassId || stats.total === 0}>
            <Download className="mr-2 h-4 w-4" />
            Xuất CSV
          </Button>
        </div>

        {/* Filters */}
        <Card>
          <CardHeader>
            <CardTitle>Bộ lọc</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="class-select">Lớp học</Label>
                <Select
                  value={selectedClassId?.toString() || ''}
                  onValueChange={(value) => setSelectedClassId(parseInt(value))}
                >
                  <SelectTrigger id="class-select">
                    <SelectValue placeholder="Chọn lớp học để xem báo cáo" />
                  </SelectTrigger>
                  <SelectContent>
                    {classes.map((classItem) => (
                      <SelectItem key={classItem.id} value={classItem.id.toString()}>
                        {classItem.name} - {classItem.classCode}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
          </CardContent>
        </Card>

        {selectedClassId ? (
          <>
            {/* Summary Stats */}
            <div className="grid gap-4 md:grid-cols-4">
              <Card>
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">
                    Tổng số lần điểm danh
                  </CardTitle>
                  <Users className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{stats.total}</div>
                  <p className="text-xs text-muted-foreground">
                    Tất cả bản ghi
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">
                    Tỷ lệ có mặt
                  </CardTitle>
                  <TrendingUp className="h-4 w-4 text-green-600" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-green-600">
                    {presentRate.toFixed(1)}%
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {stats.present} / {stats.total} lần
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">
                    Tỷ lệ vắng
                  </CardTitle>
                  <TrendingDown className="h-4 w-4 text-red-600" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-red-600">
                    {absentRate.toFixed(1)}%
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {stats.absent} / {stats.total} lần
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">
                    Học viên
                  </CardTitle>
                  <CheckCircle className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{studentStats.length}</div>
                  <p className="text-xs text-muted-foreground">
                    Đã điểm danh
                  </p>
                </CardContent>
              </Card>
            </div>

            {/* Attendance Breakdown */}
            <Card>
              <CardHeader>
                <CardTitle>Phân bố trạng thái</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-green-600 font-medium">Có mặt</span>
                    <span className="text-muted-foreground">
                      {stats.present} ({((stats.present / stats.total) * 100).toFixed(1)}%)
                    </span>
                  </div>
                  <Progress
                    value={(stats.present / stats.total) * 100}
                    className="h-2 bg-green-100"
                  />
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-red-600 font-medium">Vắng</span>
                    <span className="text-muted-foreground">
                      {stats.absent} ({((stats.absent / stats.total) * 100).toFixed(1)}%)
                    </span>
                  </div>
                  <Progress
                    value={(stats.absent / stats.total) * 100}
                    className="h-2 bg-red-100"
                  />
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-yellow-600 font-medium">Đi trễ</span>
                    <span className="text-muted-foreground">
                      {stats.late} ({((stats.late / stats.total) * 100).toFixed(1)}%)
                    </span>
                  </div>
                  <Progress
                    value={(stats.late / stats.total) * 100}
                    className="h-2 bg-yellow-100"
                  />
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-blue-600 font-medium">Có phép</span>
                    <span className="text-muted-foreground">
                      {stats.excused} ({((stats.excused / stats.total) * 100).toFixed(1)}%)
                    </span>
                  </div>
                  <Progress
                    value={(stats.excused / stats.total) * 100}
                    className="h-2 bg-blue-100"
                  />
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-purple-600 font-medium">Học bù</span>
                    <span className="text-muted-foreground">
                      {stats.makeup} ({((stats.makeup / stats.total) * 100).toFixed(1)}%)
                    </span>
                  </div>
                  <Progress
                    value={(stats.makeup / stats.total) * 100}
                    className="h-2 bg-purple-100"
                  />
                </div>
              </CardContent>
            </Card>

            {/* Calendar View */}
            <AttendanceCalendar
              attendanceRecords={attendanceData?.content || []}
              onDateClick={(_date) => {
                // TODO: Show attendance details for this date
              }}
            />

            {/* Student-level Statistics */}
            <Card>
              <CardHeader>
                <CardTitle>Thống kê theo học viên</CardTitle>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Học viên</TableHead>
                      <TableHead className="text-center">Tổng</TableHead>
                      <TableHead className="text-center text-green-600">Có mặt</TableHead>
                      <TableHead className="text-center text-red-600">Vắng</TableHead>
                      <TableHead className="text-center text-yellow-600">Đi trễ</TableHead>
                      <TableHead className="text-center text-blue-600">Có phép</TableHead>
                      <TableHead className="text-right">Tỷ lệ có mặt</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {studentStats.map((student, idx) => {
                      const rate = (student.present / student.total) * 100;
                      return (
                        <TableRow key={idx}>
                          <TableCell className="font-medium">{student.studentName}</TableCell>
                          <TableCell className="text-center">{student.total}</TableCell>
                          <TableCell className="text-center text-green-600">
                            {student.present}
                          </TableCell>
                          <TableCell className="text-center text-red-600">
                            {student.absent}
                          </TableCell>
                          <TableCell className="text-center text-yellow-600">
                            {student.late}
                          </TableCell>
                          <TableCell className="text-center text-blue-600">
                            {student.excused}
                          </TableCell>
                          <TableCell className="text-right">
                            <span
                              className={`font-medium ${
                                rate >= 80
                                  ? 'text-green-600'
                                  : rate >= 60
                                  ? 'text-yellow-600'
                                  : 'text-red-600'
                              }`}
                            >
                              {rate.toFixed(1)}%
                            </span>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </>
        ) : (
          <Card>
            <CardContent className="py-12">
              <div className="text-center text-muted-foreground">
                <Users className="mx-auto h-16 w-16 opacity-20" />
                <p className="mt-4 text-lg">
                  Vui lòng chọn lớp học để xem báo cáo
                </p>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardLayout>
  );
}
