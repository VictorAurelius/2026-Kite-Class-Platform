/**
 * Student PWA — Học tập (LMS course list, Increment B entry point).
 *
 * Lists the tenant's PUBLISHED courses (student-accessible `/api/v1/courses`).
 * Opening a course → the player resolves full lesson content if the student is
 * enrolled, else shows a paywall CTA (backend strips paid content per GAP-1115).
 *
 * NOTE (GAP-1285): there is no student-self "my enrolled courses" endpoint yet
 * (`/api/v1/enrollments/student/{id}` is teacher/admin-guarded), so this lists
 * the tenant catalog rather than the student's enrolled subset. Enrollment-scoped
 * filtering lands when GAP-1285 ships a STUDENT-accessible endpoint.
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave rbac-lms-student-fe — Increment B)
 */
'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { BookOpen, ChevronRight } from 'lucide-react';
import { coursesApi } from '@/lib/api/courses';
import { CourseStatus } from '@/types/course';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { StudentMobileShell } from '@/components/student/mobile-shell';

export default function StudentLearningPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['student', 'learning', 'courses'],
    queryFn: () => coursesApi.getAll({ status: CourseStatus.PUBLISHED, size: 100 }),
  });

  const courses = data?.content ?? [];

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
          Chưa có khóa học nào được mở.
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
                        {c.description ? (
                          <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                            {c.description}
                          </p>
                        ) : null}
                        {c.level ? (
                          <Badge variant="outline" className="mt-2">
                            {c.level}
                          </Badge>
                        ) : null}
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
