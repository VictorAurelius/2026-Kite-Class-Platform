/**
 * Student PWA — Chi tiết điểm theo môn (grade detail).
 * Wave 49 Bucket C (GAP-269). Mirrors `kiteclass-student/screens/grade-detail.html`.
 */
'use client';

import { use } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { StudentMobileShell } from '@/components/student/mobile-shell';

interface PageProps {
  params: Promise<{ subjectId: string }>;
}

const HISTORY = [
  { date: '02/04/2026', kind: 'Kiểm tra 15 phút', score: 9.0, weight: 'x1' },
  { date: '15/03/2026', kind: 'Kiểm tra 1 tiết', score: 8.5, weight: 'x2' },
  { date: '08/03/2026', kind: 'Kiểm tra miệng', score: 8.0, weight: 'x1' },
  { date: '22/02/2026', kind: 'Bài tập về nhà', score: 9.5, weight: 'x1' },
  { date: '10/02/2026', kind: 'Kiểm tra 15 phút', score: 7.5, weight: 'x1' },
];

export default function StudentGradeDetailPage({ params }: PageProps) {
  const { subjectId } = use(params);

  return (
    <StudentMobileShell title="Toán nâng cao" subtitle={`Mã môn: ${subjectId}`}>
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm">Lịch sử điểm</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <ul>
            {HISTORY.map((h, idx) => (
              <li
                key={idx}
                className={`flex items-center gap-3 px-4 py-3 ${idx > 0 ? 'border-t border-border' : ''}`}
              >
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-medium">{h.kind}</div>
                  <div className="text-xs text-muted-foreground">{h.date}</div>
                </div>
                <Badge variant="outline" className="font-mono">{h.weight}</Badge>
                <div className="text-lg font-bold text-primary">{h.score.toFixed(1)}</div>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </StudentMobileShell>
  );
}
