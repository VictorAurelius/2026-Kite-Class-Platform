/**
 * Student PWA — Lớp học của tôi (my-classes list).
 * Wave 49 Bucket C (GAP-269). Mirrors `kiteclass-student/screens/my-classes.html`.
 */
'use client';

import Link from 'next/link';
import { ChevronRight, GraduationCap } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { StudentMobileShell } from '@/components/student/mobile-shell';

interface ClassSummary {
  id: string;
  name: string;
  teacher: string;
  schedule: string;
  progress: number;
  unread: number;
}

const CLASSES: readonly ClassSummary[] = [
  { id: 'c1', name: 'Toán nâng cao — 10A2', teacher: 'Cô Trần Thị Hương', schedule: 'T2 / T4 / T6 · 10:00', progress: 64, unread: 2 },
  { id: 'c2', name: 'Ngữ Văn 10', teacher: 'Thầy Nguyễn Anh Tuấn', schedule: 'T2 / T5 · 13:30', progress: 58, unread: 0 },
  { id: 'c3', name: 'Tiếng Anh 10', teacher: 'Cô Phạm Thuỳ Linh', schedule: 'T2 / T4 / T6 · 15:30', progress: 72, unread: 1 },
  { id: 'c4', name: 'Vật lý 10', teacher: 'Thầy Lê Văn Hậu', schedule: 'T3 / T5 · 09:30', progress: 51, unread: 0 },
  { id: 'c5', name: 'Hoá học 10', teacher: 'Cô Đặng Mỹ Hạnh', schedule: 'T3 / T6 · 14:00', progress: 67, unread: 0 },
];

export default function StudentClassesPage() {
  return (
    <StudentMobileShell title="Lớp học của tôi" subtitle={`${CLASSES.length} môn đang học`}>
      <ul className="space-y-3">
        {CLASSES.map((c) => (
          <li key={c.id}>
            <Link href={`/student/my-classes/${c.id}`} className="block">
              <Card className="transition-colors hover:bg-accent/50">
                <CardContent className="p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <GraduationCap className="h-4 w-4 text-primary" aria-hidden />
                        <h3 className="truncate text-sm font-semibold">{c.name}</h3>
                      </div>
                      <p className="mt-1 text-xs text-muted-foreground">{c.teacher}</p>
                      <p className="text-xs text-muted-foreground">{c.schedule}</p>
                    </div>
                    {c.unread > 0 ? (
                      <Badge variant="destructive">{c.unread} mới</Badge>
                    ) : null}
                    <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden />
                  </div>
                  <div className="mt-3 flex items-center gap-2">
                    <div
                      className="h-2 flex-1 overflow-hidden rounded-full bg-muted"
                      role="progressbar"
                      aria-valuenow={c.progress}
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-label={`Tiến độ ${c.progress}%`}
                    >
                      <div
                        className="h-full bg-primary"
                        style={{ width: `${c.progress}%` }}
                      />
                    </div>
                    <span className="text-xs font-medium text-muted-foreground">
                      {c.progress}%
                    </span>
                  </div>
                </CardContent>
              </Card>
            </Link>
          </li>
        ))}
      </ul>
    </StudentMobileShell>
  );
}
