/**
 * Student PWA — Bài tập (assignments list, real-data + filterable).
 *
 * Wave rbac-lms-student-fe (GAP-1113 Increment B / PART 3): rewired from mock to
 * the real KiteClass assignment API. GAP-1285: now enrollment-scoped — lists
 * PUBLISHED assignments across the classes the student is ENROLLED in (resolved
 * via `/api/v1/enrollments/me`, not the tenant-wide `/api/v1/classes` SHARED
 * READ), enriched with the student's own submission status (`getMySubmissions`).
 *
 * @author KiteClass Team
 * @since Wave 49 Bucket C (GAP-269) mock; Wave rbac-lms-student-fe real-data; GAP-1285 enrollment-scoped
 */
'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { ChevronRight, FileText } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { enrollmentsApi } from '@/lib/api/enrollments';
import { assignmentsApi } from '@/lib/api/assignments';
import { useMySubmissions } from '@/hooks/use-assignments';
import type { Assignment } from '@/types/assignment';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { StudentMobileShell } from '@/components/student/mobile-shell';

type Filter = 'pending' | 'submitted' | 'graded';

interface AssignmentWithClass extends Assignment {
  className: string;
}

function formatDue(iso?: string | null): string {
  if (!iso) return 'Không có hạn';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export default function StudentAssignmentsPage() {
  const userId = useAuthStore((s) => s.user?.id);
  const [filter, setFilter] = useState<Filter>('pending');

  // Published assignments across the classes the student is ENROLLED in
  // (enrollment-scoped via /api/v1/enrollments/me — GAP-1285).
  const { data: assignments, isLoading: loadingAssignments } = useQuery({
    queryKey: ['student', 'assignments', 'my-enrolled'],
    queryFn: async (): Promise<AssignmentWithClass[]> => {
      const page = await enrollmentsApi.getMine({ size: 100 });
      // Dedupe enrolled classes (a student could have multiple enrollments).
      const classById = new Map<number, string>();
      for (const e of page.content ?? []) {
        if (!classById.has(e.classId)) {
          classById.set(e.classId, e.className ?? `Lớp #${e.classId}`);
        }
      }
      const perClass = await Promise.all(
        [...classById.entries()].map(([classId, className]) =>
          assignmentsApi
            .getPublishedByClass(classId)
            .then((list) => list.map((a) => ({ ...a, className })))
            .catch(() => [] as AssignmentWithClass[]),
        ),
      );
      return perClass.flat();
    },
  });

  const { data: mySubmissions, isLoading: loadingSubs } = useMySubmissions(userId);

  // Map assignmentId → submission status for fast lookup.
  const statusByAssignment = useMemo(() => {
    const map = new Map<number, Filter>();
    for (const s of mySubmissions ?? []) {
      map.set(s.assignmentId, s.status === 'PENDING' ? 'submitted' : 'graded');
    }
    return map;
  }, [mySubmissions]);

  const gradeByAssignment = useMemo(() => {
    const map = new Map<number, number>();
    for (const s of mySubmissions ?? []) {
      const score = s.adjustedScore ?? s.score;
      if (s.status !== 'PENDING' && score != null) map.set(s.assignmentId, score);
    }
    return map;
  }, [mySubmissions]);

  const isLoading = loadingAssignments || loadingSubs;

  const filtered = useMemo(() => {
    return (assignments ?? []).filter(
      (a) => (statusByAssignment.get(a.id) ?? 'pending') === filter,
    );
  }, [assignments, statusByAssignment, filter]);

  return (
    <StudentMobileShell title="Bài tập" subtitle="Theo dõi tiến độ và nộp bài">
      <Tabs value={filter} onValueChange={(v) => setFilter(v as Filter)}>
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="pending">Chờ nộp</TabsTrigger>
          <TabsTrigger value="submitted">Đã nộp</TabsTrigger>
          <TabsTrigger value="graded">Đã chấm</TabsTrigger>
        </TabsList>
      </Tabs>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <LoadingSpinner size="lg" />
        </div>
      ) : (
        <ul className="mt-4 space-y-2">
          {filtered.length === 0 ? (
            <li className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
              Không có bài tập nào.
            </li>
          ) : (
            filtered.map((a) => {
              const status = statusByAssignment.get(a.id) ?? 'pending';
              const grade = gradeByAssignment.get(a.id);
              return (
                <li key={a.id}>
                  <Link href={`/student/assignments/${a.id}`}>
                    <Card className="transition-colors hover:bg-accent/50">
                      <CardContent className="flex items-center gap-3 p-3">
                        <FileText className="h-5 w-5 shrink-0 text-muted-foreground" aria-hidden />
                        <div className="min-w-0 flex-1">
                          <div className="truncate text-sm font-medium">{a.title}</div>
                          <div className="truncate text-xs text-muted-foreground">
                            {a.className} · Hạn {formatDue(a.dueDate)}
                          </div>
                        </div>
                        {status === 'graded' && grade != null ? (
                          <Badge variant="default" className="bg-emerald-600">
                            {grade}
                            {a.maxScore != null ? `/${a.maxScore}` : ''}
                          </Badge>
                        ) : status === 'submitted' ? (
                          <Badge variant="secondary">Đã nộp</Badge>
                        ) : (
                          <Badge variant="outline">Chờ nộp</Badge>
                        )}
                        <ChevronRight className="h-4 w-4 text-muted-foreground" aria-hidden />
                      </CardContent>
                    </Card>
                  </Link>
                </li>
              );
            })
          )}
        </ul>
      )}
    </StudentMobileShell>
  );
}
