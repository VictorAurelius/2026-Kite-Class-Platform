/**
 * Class report detail — MoET-format report card output for the subject
 * teacher / GVCN.
 *
 * Maps to `kiteclass-teacher/reports-detail-class.html`. Output complies with
 * Thông tư 22/2021/TT-BGDĐT classification (Xuất sắc / Giỏi / Khá / TB / Yếu).
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { Download, Printer } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  TEACHER_PROFILE,
  TEACHER_REPORT_ROWS,
  buildSampleStudents,
} from '@/components/teacher/teacher-mock-data';

function classifyVN(avg: number): { label: string; color: string } {
  if (avg >= 9.0) return { label: 'Xuất sắc', color: 'text-green-700' };
  if (avg >= 8.0) return { label: 'Giỏi', color: 'text-blue-700' };
  if (avg >= 6.5) return { label: 'Khá', color: 'text-amber-700' };
  if (avg >= 5.0) return { label: 'Trung bình', color: 'text-orange-700' };
  return { label: 'Yếu', color: 'text-red-700' };
}

export default function TeacherReportDetailPage() {
  const params = useParams<{ classId: string }>();
  const classId = params?.classId ?? 'cls-10a2';
  const classMeta =
    TEACHER_REPORT_ROWS.find((c) => c.classId === classId) ??
    TEACHER_REPORT_ROWS[0]!;

  // Generate per-student grades from sample data (deterministic).
  const rows = buildSampleStudents().slice(0, classMeta.totalStudents).map((s, idx) => {
    const seed = (idx * 13 + 17) % 100;
    const avg = Math.max(4.0, Math.min(10.0, 6.5 + seed / 25));
    const cls = classifyVN(avg);
    return { ...s, average: avg, classification: cls };
  });

  return (
    <div className="space-y-4">
      <nav className="text-sm text-muted-foreground" aria-label="Breadcrumb">
        <Link href="/teacher/reports" className="hover:text-foreground">
          Báo cáo
        </Link>{' '}
        / <span className="text-foreground">{classMeta.className}</span>
      </nav>

      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">
            Báo cáo lớp {classMeta.className}
          </h1>
          <p className="text-muted-foreground">
            {classMeta.subject} · {TEACHER_PROFILE.semester}{' '}
            {TEACHER_PROFILE.schoolYear} · GV: {TEACHER_PROFILE.fullName}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm">
            <Printer className="mr-2 h-4 w-4" />
            In phiếu báo
          </Button>
          <Button variant="outline" size="sm">
            <Download className="mr-2 h-4 w-4" />
            Xuất Excel (MoET)
          </Button>
        </div>
      </div>

      <div className="rounded-md bg-muted/40 px-3 py-2 text-sm">
        <span className="font-medium">Tham chiếu:</span> Thông tư
        22/2021/TT-BGDĐT — Quy chế đánh giá học sinh THPT
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Phiếu báo điểm lớp</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-border">
              <tr className="text-left">
                <th className="px-3 py-2">STT</th>
                <th className="px-3 py-2">Họ và tên</th>
                <th className="px-3 py-2">MSHS</th>
                <th className="px-3 py-2 text-right">Điểm TB</th>
                <th className="px-3 py-2">Xếp loại</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {rows.map((r, idx) => (
                <tr key={r.id}>
                  <td className="px-3 py-2 tabular-nums">{idx + 1}</td>
                  <td className="px-3 py-2 font-medium">{r.fullName}</td>
                  <td className="px-3 py-2 text-muted-foreground">
                    {r.studentCode}
                  </td>
                  <td className="px-3 py-2 text-right tabular-nums font-semibold">
                    {r.average.toFixed(2)}
                  </td>
                  <td className={`px-3 py-2 font-medium ${r.classification.color}`}>
                    {r.classification.label}
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
