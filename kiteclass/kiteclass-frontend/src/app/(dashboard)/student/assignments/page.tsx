/**
 * Student PWA — Bài tập (assignments list, filterable).
 * Wave 49 Bucket C (GAP-269). Mirrors `kiteclass-student/screens/assignments.html`.
 */
'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ChevronRight, FileText } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { StudentMobileShell } from '@/components/student/mobile-shell';

type Filter = 'pending' | 'submitted' | 'graded';

interface Assignment {
  id: string;
  title: string;
  course: string;
  due: string;
  status: Filter;
  grade?: string;
}

const ASSIGNMENTS: readonly Assignment[] = [
  { id: 't1', title: 'Bài luận "Quê hương"', course: 'Văn 10', due: '18/04/2026', status: 'pending' },
  { id: 't2', title: 'Bài tập chương 4', course: 'Toán nâng cao', due: 'Hôm nay', status: 'pending' },
  { id: 't3', title: 'Reading worksheet', course: 'English 10', due: '22/04/2026', status: 'pending' },
  { id: 't4', title: 'Bài tập chương 3', course: 'Toán nâng cao', due: '10/04/2026', status: 'submitted' },
  { id: 't5', title: 'Listening test 2', course: 'English 10', due: '08/04/2026', status: 'submitted' },
  { id: 't6', title: 'Bài tập chương 2', course: 'Toán nâng cao', due: '01/04/2026', status: 'graded', grade: '8.5' },
  { id: 't7', title: 'Phân tích bài thơ', course: 'Văn 10', due: '28/03/2026', status: 'graded', grade: '9.0' },
];

export default function StudentAssignmentsPage() {
  const [filter, setFilter] = useState<Filter>('pending');
  const filtered = ASSIGNMENTS.filter((a) => a.status === filter);

  return (
    <StudentMobileShell title="Bài tập" subtitle="Theo dõi tiến độ và nộp bài">
      <Tabs value={filter} onValueChange={(v) => setFilter(v as Filter)}>
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="pending">Chờ nộp</TabsTrigger>
          <TabsTrigger value="submitted">Đã nộp</TabsTrigger>
          <TabsTrigger value="graded">Đã chấm</TabsTrigger>
        </TabsList>
      </Tabs>

      <ul className="mt-4 space-y-2">
        {filtered.length === 0 ? (
          <li className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
            Không có bài tập nào.
          </li>
        ) : (
          filtered.map((a) => (
            <li key={a.id}>
              <Link href={`/student/assignments/${a.id}`}>
                <Card className="transition-colors hover:bg-accent/50">
                  <CardContent className="flex items-center gap-3 p-3">
                    <FileText className="h-5 w-5 shrink-0 text-muted-foreground" aria-hidden />
                    <div className="min-w-0 flex-1">
                      <div className="truncate text-sm font-medium">{a.title}</div>
                      <div className="text-xs text-muted-foreground">
                        {a.course} · Hạn {a.due}
                      </div>
                    </div>
                    {a.status === 'graded' && a.grade ? (
                      <Badge variant="default" className="bg-emerald-600">
                        {a.grade}
                      </Badge>
                    ) : a.status === 'submitted' ? (
                      <Badge variant="secondary">Đã nộp</Badge>
                    ) : (
                      <Badge variant="outline">Chờ nộp</Badge>
                    )}
                    <ChevronRight className="h-4 w-4 text-muted-foreground" aria-hidden />
                  </CardContent>
                </Card>
              </Link>
            </li>
          ))
        )}
      </ul>
    </StudentMobileShell>
  );
}
