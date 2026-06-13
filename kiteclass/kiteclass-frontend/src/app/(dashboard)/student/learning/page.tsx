/**
 * Student PWA — Học tập (LMS course list, Increment B entry point).
 *
 * Lists the courses the student is ENROLLED in (enrollment-scoped via
 * `/api/v1/enrollments/me` — GAP-1285), derived from the student's own
 * enrollments. Opening a course → the player resolves full lesson content if the
 * student is enrolled, else shows a paywall CTA (backend strips paid content per
 * GAP-1115).
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave rbac-lms-student-fe — Increment B); GAP-1285 enrollment-scoped
 */
'use client';

import { useMemo } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { BookOpen, ChevronRight } from 'lucide-react';
import { enrollmentsApi } from '@/lib/api/enrollments';
import { Card, CardContent } from '@/components/ui/card';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { StudentMobileShell } from '@/components/student/mobile-shell';

export default function StudentLearningPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['student', 'learning', 'my-enrollments'],
    queryFn: () => enrollmentsApi.getMine({ size: 100 }),
  });

  // Derive the unique enrolled courses from the student's enrollments.
  const courses = useMemo(() => {
    const byId = new Map<number, { id: number; name: string }>();
    for (const e of data?.content ?? []) {
      if (e.courseId != null && !byId.has(e.courseId)) {
        byId.set(e.courseId, { id: e.courseId, name: e.courseName ?? `Khóa #${e.courseId}` });
      }
    }
    return [...byId.values()];
  }, [data]);

  return (
    <StudentMobileShell title="Học tập" subtitle="Khóa học của bạn">
      {isLoading ? (
        <div className="flex justify-center py-12">
          <LoadingSpinner size="lg" />
        </div>
      ) : error ? (
        <div className="rounded-lg border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive">
          Không tải được danh sách khóa học. Vui lòng thử lại.
        </div>
      ) : courses.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
          <BookOpen className="mx-auto mb-2 h-8 w-8 opacity-50" aria-hidden />
          Bạn chưa ghi danh khóa học nào.
        </div>
      ) : (
        <ul className="space-y-3">
          {courses.map((c) => (
            <li key={c.id}>
              <Link href={`/student/learning/${c.id}`} className="block">
                <Card className="transition-colors hover:bg-accent/50">
                  <CardContent className="p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <BookOpen className="h-4 w-4 shrink-0 text-primary" aria-hidden />
                          <h3 className="truncate text-sm font-semibold">{c.name}</h3>
                        </div>
                      </div>
                      <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden />
                    </div>
                  </CardContent>
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </StudentMobileShell>
  );
}
