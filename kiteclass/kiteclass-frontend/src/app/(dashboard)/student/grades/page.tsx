/**
 * Student PWA — Điểm số (grades overview).
 * Wave 49 Bucket C (GAP-269). Mirrors `kiteclass-student/screens/grades.html`.
 */
'use client';

import Link from 'next/link';
import { ChevronRight, TrendingUp } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StudentMobileShell } from '@/components/student/mobile-shell';

interface SubjectGrade {
  id: string;
  name: string;
  current: number;
  trend: 'up' | 'down' | 'flat';
  delta: string;
}

const SUBJECTS: readonly SubjectGrade[] = [
  { id: 'math', name: 'Toán nâng cao', current: 8.7, trend: 'up', delta: '+0.4' },
  { id: 'literature', name: 'Ngữ Văn 10', current: 8.2, trend: 'up', delta: '+0.2' },
  { id: 'english', name: 'Tiếng Anh 10', current: 7.9, trend: 'flat', delta: '0' },
  { id: 'physics', name: 'Vật lý 10', current: 7.5, trend: 'down', delta: '-0.3' },
  { id: 'chemistry', name: 'Hoá học 10', current: 8.4, trend: 'up', delta: '+0.5' },
  { id: 'biology', name: 'Sinh học 10', current: 8.0, trend: 'flat', delta: '0' },
];

export default function StudentGradesPage() {
  const avg = (SUBJECTS.reduce((s, x) => s + x.current, 0) / SUBJECTS.length).toFixed(2);

  return (
    <StudentMobileShell title="Điểm số" subtitle="Học kỳ I · 2025-2026">
      <Card className="border-primary/30 bg-gradient-to-br from-teal-500 to-emerald-600 text-white">
        <CardContent className="p-5">
          <div className="text-sm opacity-90">Điểm trung bình</div>
          <div className="mt-1 text-5xl font-bold">{avg}</div>
          <div className="mt-2 flex items-center gap-1 text-xs opacity-90">
            <TrendingUp className="h-3 w-3" aria-hidden /> Tăng 0.2 so với tháng trước
          </div>
        </CardContent>
      </Card>

      <Card className="mt-4">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm">Theo môn học</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <ul>
            {SUBJECTS.map((s, idx) => (
              <li key={s.id}>
                <Link
                  href={`/student/grades/${s.id}`}
                  className={`flex items-center gap-3 px-4 py-3 transition-colors hover:bg-accent/50 ${idx > 0 ? 'border-t border-border' : ''}`}
                >
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-medium">{s.name}</div>
                    <div className="text-xs text-muted-foreground">{s.delta} so với kỳ trước</div>
                  </div>
                  <div className={`text-lg font-bold ${gradeColor(s.current)}`}>
                    {s.current.toFixed(1)}
                  </div>
                  <ChevronRight className="h-4 w-4 text-muted-foreground" aria-hidden />
                </Link>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </StudentMobileShell>
  );
}

function gradeColor(g: number): string {
  if (g >= 8.5) return 'text-emerald-600';
  if (g >= 7.0) return 'text-primary';
  if (g >= 5.0) return 'text-amber-600';
  return 'text-destructive';
}
