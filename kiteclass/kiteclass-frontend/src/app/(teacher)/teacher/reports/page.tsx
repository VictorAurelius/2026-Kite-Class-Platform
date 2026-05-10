/**
 * Reports overview — class-level analytics for subject teacher / GVCN.
 *
 * Maps to `kiteclass-teacher/reports-overview-default.html` + empty +
 * loading variants.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import Link from 'next/link';
import { ChevronRight, TrendingUp, Users, Award } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { TEACHER_REPORT_ROWS } from '@/components/teacher/teacher-mock-data';

function pct(n: number) {
  return `${Math.round(n * 100)}%`;
}

export default function TeacherReportsPage() {
  const totalStudents = TEACHER_REPORT_ROWS.reduce(
    (sum, r) => sum + r.totalStudents,
    0,
  );
  const overallAverage =
    TEACHER_REPORT_ROWS.reduce((sum, r) => sum + r.averageGrade * r.totalStudents, 0) /
    Math.max(totalStudents, 1);
  const overallAttendance =
    TEACHER_REPORT_ROWS.reduce(
      (sum, r) => sum + r.attendanceRate * r.totalStudents,
      0,
    ) / Math.max(totalStudents, 1);
  const totalExcellent = TEACHER_REPORT_ROWS.reduce(
    (sum, r) => sum + r.excellentCount,
    0,
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Báo cáo</h1>
        <p className="text-muted-foreground">
          Tổng quan kết quả học tập + chuyên cần các lớp dạy
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">TB tổng</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold tabular-nums">
              {overallAverage.toFixed(2)}
            </div>
            <p className="text-xs text-muted-foreground">
              Trung bình điểm số trên {TEACHER_REPORT_ROWS.length} lớp
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Chuyên cần</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold tabular-nums">
              {pct(overallAttendance)}
            </div>
            <p className="text-xs text-muted-foreground">Tổng {totalStudents} học sinh</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Học sinh xuất sắc</CardTitle>
            <Award className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold tabular-nums text-green-600">
              {totalExcellent}
            </div>
            <p className="text-xs text-muted-foreground">Điểm TB ≥ 9.0</p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Phân tích theo lớp</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-border">
              <tr className="text-left">
                <th className="px-3 py-2">Lớp / Môn</th>
                <th className="px-3 py-2 text-right">Sĩ số</th>
                <th className="px-3 py-2 text-right">TB lớp</th>
                <th className="px-3 py-2 text-right">Xuất sắc</th>
                <th className="px-3 py-2 text-right">Giỏi</th>
                <th className="px-3 py-2 text-right">TB</th>
                <th className="px-3 py-2 text-right">Yếu</th>
                <th className="px-3 py-2 text-right">Chuyên cần</th>
                <th className="px-3 py-2"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {TEACHER_REPORT_ROWS.map((row) => (
                <tr key={row.classId}>
                  <td className="px-3 py-2 font-medium">
                    {row.className}
                    <span className="ml-2 text-xs text-muted-foreground">
                      {row.subject}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-right tabular-nums">
                    {row.totalStudents}
                  </td>
                  <td className="px-3 py-2 text-right tabular-nums font-semibold">
                    {row.averageGrade.toFixed(1)}
                  </td>
                  <td className="px-3 py-2 text-right tabular-nums text-green-700">
                    {row.excellentCount}
                  </td>
                  <td className="px-3 py-2 text-right tabular-nums">
                    {row.goodCount}
                  </td>
                  <td className="px-3 py-2 text-right tabular-nums">
                    {row.averageCount}
                  </td>
                  <td className="px-3 py-2 text-right tabular-nums text-red-700">
                    {row.weakCount}
                  </td>
                  <td className="px-3 py-2 text-right tabular-nums">
                    {pct(row.attendanceRate)}
                  </td>
                  <td className="px-3 py-2 text-right">
                    <Link
                      href={`/teacher/reports/${row.classId}`}
                      className="inline-flex items-center text-primary hover:underline"
                    >
                      Chi tiết
                      <ChevronRight className="h-4 w-4" />
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  );
}
