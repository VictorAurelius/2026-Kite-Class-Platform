/**
 * Gradebook overview — class list for grade entry.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import Link from 'next/link';
import { ChevronRight } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { TEACHER_REPORT_ROWS } from '@/components/teacher/teacher-mock-data';

export default function TeacherGradesOverviewPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Sổ điểm</h1>
        <p className="text-muted-foreground">
          Chọn lớp để vào điểm theo MoET (TT 22/2021/TT-BGDĐT)
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Các lớp được phân công</CardTitle>
        </CardHeader>
        <CardContent className="divide-y divide-border">
          {TEACHER_REPORT_ROWS.map((row) => (
            <Link
              key={row.classId}
              href={`/teacher/grades/${row.classId}`}
              className="flex items-center justify-between py-3 hover:bg-muted/40"
            >
              <div>
                <div className="font-medium">
                  {row.className} · {row.subject}
                </div>
                <div className="text-sm text-muted-foreground">
                  {row.totalStudents} học sinh · TB lớp{' '}
                  <span className="tabular-nums">
                    {row.averageGrade.toFixed(1)}
                  </span>
                </div>
              </div>
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            </Link>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
