/**
 * Student PWA — course player (module/lesson tree + progress, LMS Increment B).
 *
 * Lists the course structure (modules + ordered lessons) with an aggregate
 * progress bar. Each lesson links to the lesson player. Paid lessons the student
 * isn't enrolled in are marked "Trả phí" (the player page handles the paywall CTA
 * — backend strips content per GAP-1115).
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave rbac-lms-student-fe — Increment B)
 */
'use client';

import { use } from 'react';
import Link from 'next/link';
import { ChevronRight, Clock, Lock, PlayCircle, Sparkles } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { useCourseStructure, useCourseProgress } from '@/hooks/use-lms';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { StudentMobileShell } from '@/components/student/mobile-shell';

interface PageProps {
  params: Promise<{ courseId: string }>;
}

export default function StudentCoursePlayerPage({ params }: PageProps) {
  const { courseId: courseIdStr } = use(params);
  const courseId = Number(courseIdStr);
  const userId = useAuthStore((s) => s.user?.id);

  const { data: modules, isLoading, error } = useCourseStructure(courseId, userId);
  const { data: progress } = useCourseProgress(courseId, userId);

  const percent = progress?.progressPercent ?? 0;

  return (
    <StudentMobileShell
      title="Nội dung khóa học"
      subtitle={
        progress
          ? `Hoàn thành ${progress.completedLessons}/${progress.totalLessons} bài`
          : 'Bài học của bạn'
      }
    >
      {isLoading ? (
        <div className="flex justify-center py-12">
          <LoadingSpinner size="lg" />
        </div>
      ) : error ? (
        <div className="rounded-lg border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive">
          Không tải được nội dung khóa học. Vui lòng thử lại.
        </div>
      ) : (
        <>
          {/* Aggregate progress */}
          {progress ? (
            <Card className="mb-4">
              <CardContent className="p-4">
                <div className="mb-2 flex items-center justify-between text-sm">
                  <span className="flex items-center gap-1.5 font-medium">
                    <Sparkles className="h-4 w-4 text-primary" aria-hidden />
                    Tiến độ học tập
                  </span>
                  <span className="font-semibold text-primary">{Math.round(percent)}%</span>
                </div>
                <div
                  className="h-2 w-full overflow-hidden rounded-full bg-muted"
                  role="progressbar"
                  aria-valuenow={Math.round(percent)}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-label={`Tiến độ ${Math.round(percent)}%`}
                >
                  <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${percent}%` }} />
                </div>
              </CardContent>
            </Card>
          ) : null}

          {(modules ?? []).length === 0 ? (
            <div className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
              Khóa học này chưa có bài học.
            </div>
          ) : (
            <div className="space-y-4">
              {(modules ?? []).map((mod) => (
                <section key={mod.id}>
                  <h2 className="mb-2 text-sm font-semibold">{mod.title}</h2>
                  <ul className="space-y-2">
                    {mod.lessons.map((lesson) => {
                      // Paid lesson with no content delivered = not enrolled (GAP-1115 strip).
                      const locked = !lesson.isTrial && !lesson.content && !lesson.videoUrl;
                      return (
                        <li key={lesson.id}>
                          <Link
                            href={`/student/learning/${courseId}/lessons/${lesson.id}`}
                            className="block"
                          >
                            <Card className="transition-colors hover:bg-accent/50">
                              <CardContent className="flex items-center gap-3 p-3">
                                {locked ? (
                                  <Lock className="h-5 w-5 shrink-0 text-muted-foreground" aria-hidden />
                                ) : (
                                  <PlayCircle className="h-5 w-5 shrink-0 text-primary" aria-hidden />
                                )}
                                <div className="min-w-0 flex-1">
                                  <div className="truncate text-sm font-medium">{lesson.title}</div>
                                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                                    {lesson.estimatedDuration ? (
                                      <span className="flex items-center gap-1">
                                        <Clock className="h-3 w-3" aria-hidden />
                                        {lesson.estimatedDuration} phút
                                      </span>
                                    ) : null}
                                    {lesson.isTrial ? (
                                      <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">
                                        Học thử
                                      </Badge>
                                    ) : locked ? (
                                      <Badge variant="outline" className="h-5 px-1.5 text-[10px]">
                                        Trả phí
                                      </Badge>
                                    ) : null}
                                  </div>
                                </div>
                                <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden />
                              </CardContent>
                            </Card>
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                </section>
              ))}
            </div>
          )}
        </>
      )}
    </StudentMobileShell>
  );
}
