/**
 * Attendance reports page - shows attendance statistics and trends.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

export const dynamic = 'force-dynamic';

import { useState } from 'react';
import { ArrowLeft, Download, Users, TrendingUp, TrendingDown, CheckCircle, ChevronDown } from 'lucide-react';
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
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuCheckboxItem,
} from '@/components/ui/dropdown-menu';
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
import type { Class } from '@/types/class';
import { DynamicAttendanceCalendar } from '@/components/attendance';
import {
  exportAttendanceXlsx,
  ATTENDANCE_CRITERION_ORDER,
  ATTENDANCE_CRITERION_LABELS,
  type AttendanceExportCriterion,
} from '@/lib/attendance-export';

export default function AttendanceReportsPage() {
  const { data: classes = [], error: classesError } = useAllActiveClasses();
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);

  // Fetch attendance data for selected class
  const { data: attendanceData, error: attendanceError } = useAttendanceByClass(
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

  // GAP-1476: zero-guard division — when a class has 0 attendance records,
  // stats.total === 0 → bare `n / stats.total` renders "NaN%". Always go
  // through safePct so the status-distribution shows 0% (not NaN%).
  const safePct = (n: number) => (stats.total > 0 ? (n / stats.total) * 100 : 0);
  const presentRate = safePct(stats.present);
  const absentRate = safePct(stats.absent);

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

  // Selected export criteria (default: export all four sheets).
  const [exportCriteria, setExportCriteria] = useState<Record<AttendanceExportCriterion, boolean>>({
    detail: true,
    session: true,
    student: true,
    summary: true,
  });

  const toggleCriterion = (criterion: AttendanceExportCriterion) =>
    setExportCriteria((prev) => ({ ...prev, [criterion]: !prev[criterion] }));

  const selectedCount = ATTENDANCE_CRITERION_ORDER.filter((c) => exportCriteria[c]).length;

  const selectedClass = classes.find((c: Class) => c.id === selectedClassId);

  // Export the selected criteria to a single XLSX workbook (one sheet per criterion).
  // Async because `xlsx` is loaded lazily at click time (GAP-1478) so the heavy
  // SheetJS bundle stays out of this route's First Load JS.
  const handleExportXlsx = async () => {
    if (!attendanceData?.content || attendanceData.content.length === 0) {
      alert('Không có dữ liệu để xuất');
      return;
    }
    const criteria = ATTENDANCE_CRITERION_ORDER.filter((c) => exportCriteria[c]);
    if (criteria.length === 0) {
      alert('Vui lòng chọn ít nhất một nội dung để xuất');
      return;
    }
    await exportAttendanceXlsx(
      {
        className: selectedClass?.name ?? 'lop',
        records: attendanceData.content,
        stats,
        studentStats,
      },
      criteria,
    );
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
          <div className="flex items-center gap-2">
            <Button
              onClick={() => void handleExportXlsx()}
              disabled={!selectedClassId || stats.total === 0 || selectedCount === 0}
            >
              <Download className="mr-2 h-4 w-4" />
              Xuất Excel ({selectedCount})
            </Button>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="outline"
                  size="icon"
                  disabled={!selectedClassId || stats.total === 0}
                  aria-label="Chọn nội dung xuất"
                >
                  <ChevronDown className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>Chọn nội dung xuất</DropdownMenuLabel>
                <DropdownMenuSeparator />
                {ATTENDANCE_CRITERION_ORDER.map((criterion) => (
                  <DropdownMenuCheckboxItem
                    key={criterion}
                    checked={exportCriteria[criterion]}
                    onCheckedChange={() => toggleCriterion(criterion)}
                    onSelect={(e) => e.preventDefault()}
                  >
                    {ATTENDANCE_CRITERION_LABELS[criterion]}
                  </DropdownMenuCheckboxItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>

        {/* Error State */}
        {(classesError || attendanceError) && (
          <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
            <p className="text-sm text-destructive">Không thể tải dữ liệu. Vui lòng thử lại.</p>
          </div>
        )}

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
                    {classes.map((classItem: Class) => (
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
                      {stats.present} ({(safePct(stats.present)).toFixed(1)}%)
                    </span>
                  </div>
                  <Progress
                    value={safePct(stats.present)}
                    className="h-2 bg-green-100"
                  />
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-red-600 font-medium">Vắng</span>
                    <span className="text-muted-foreground">
                      {stats.absent} ({(safePct(stats.absent)).toFixed(1)}%)
                    </span>
                  </div>
                  <Progress
                    value={safePct(stats.absent)}
                    className="h-2 bg-red-100"
                  />
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-yellow-600 font-medium">Đi trễ</span>
                    <span className="text-muted-foreground">
                      {stats.late} ({(safePct(stats.late)).toFixed(1)}%)
                    </span>
                  </div>
                  <Progress
                    value={safePct(stats.late)}
                    className="h-2 bg-yellow-100"
                  />
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-blue-600 font-medium">Có phép</span>
                    <span className="text-muted-foreground">
                      {stats.excused} ({(safePct(stats.excused)).toFixed(1)}%)
                    </span>
                  </div>
                  <Progress
                    value={safePct(stats.excused)}
                    className="h-2 bg-blue-100"
                  />
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-purple-600 font-medium">Học bù</span>
                    <span className="text-muted-foreground">
                      {stats.makeup} ({(safePct(stats.makeup)).toFixed(1)}%)
                    </span>
                  </div>
                  <Progress
                    value={safePct(stats.makeup)}
                    className="h-2 bg-purple-100"
                  />
                </div>
              </CardContent>
            </Card>

            {/* Calendar View — lazy-loaded; only shipped when a class is selected */}
            <DynamicAttendanceCalendar
              attendanceRecords={attendanceData?.content || []}
              onDateClick={(_date) => {
                // Date click detail view will be added with the attendance detail modal feature
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
                    {studentStats.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={7} className="py-12 text-center">
                          <div className="flex flex-col items-center justify-center text-center">
                            <p className="text-lg font-medium text-muted-foreground">Chưa có dữ liệu</p>
                            <p className="text-sm text-muted-foreground mt-1">Chưa có bản ghi điểm danh nào cho lớp này.</p>
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
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
