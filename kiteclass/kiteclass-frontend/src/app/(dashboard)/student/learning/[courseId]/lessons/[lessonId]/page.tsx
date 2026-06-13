/**
 * Student PWA — lesson player (markdown + video + mark-complete, LMS Increment B).
 *
 * Renders a lesson's content (markdown) + external video embed (YouTube/Vimeo,
 * not self-hosted), a mark-complete action (gamification toast), and the lesson's
 * completion state + progress. Paid lessons the student isn't enrolled in arrive
 * content-stripped (GAP-1115) → a paywall CTA renders instead of a raw 403.
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave rbac-lms-student-fe — Increment B)
 */
'use client';

import { use } from 'react';
import Link from 'next/link';
import { ArrowLeft, CheckCircle2, FileText, Lock, Paperclip } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { useStudentLesson, useLessonProgress, useCompleteLesson } from '@/hooks/use-lms';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { StudentMobileShell } from '@/components/student/mobile-shell';
import { LessonContent } from '@/components/student/lesson-content';
import { LessonVideo } from '@/components/student/lesson-video';

interface PageProps {
  params: Promise<{ courseId: string; lessonId: string }>;
}

export default function StudentLessonPlayerPage({ params }: PageProps) {
  const { courseId: courseIdStr, lessonId: lessonIdStr } = use(params);
  const courseId = Number(courseIdStr);
  const lessonId = Number(lessonIdStr);
  const userId = useAuthStore((s) => s.user?.id);

  const { data: lesson, isLoading, error } = useStudentLesson(lessonId, userId);
  const { data: progress } = useLessonProgress(lessonId, userId);
  const complete = useCompleteLesson(courseId, userId);

  const backHref = `/student/learning/${courseId}`;
  const completed = progress?.completed ?? false;

  // Paid lesson, content delivered empty = backend stripped it (not enrolled, GAP-1115).
  const paywalled = lesson != null && !lesson.isTrial && !lesson.content && !lesson.videoUrl;

  return (
    <StudentMobileShell
      title={lesson?.title ?? 'Bài học'}
      subtitle={lesson?.isTrial ? 'Học thử' : undefined}
      headerRight={
        <Link
          href={backHref}
          aria-label="Quay lại khóa học"
          className="flex h-9 w-9 items-center justify-center rounded-full text-muted-foreground hover:bg-accent"
        >
          <ArrowLeft className="h-5 w-5" aria-hidden />
        </Link>
      }
    >
      {isLoading ? (
        <div className="flex justify-center py-12">
          <LoadingSpinner size="lg" />
        </div>
      ) : error || !lesson ? (
        <div className="rounded-lg border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive">
          Không tải được bài học. Vui lòng thử lại.
        </div>
      ) : paywalled ? (
        // Paywall CTA — never expose a raw 403.
        <Card className="border-amber-300 bg-amber-50 dark:border-amber-700 dark:bg-amber-950/30">
          <CardContent className="space-y-3 p-6 text-center">
            <Lock className="mx-auto h-10 w-10 text-amber-600" aria-hidden />
            <h2 className="text-base font-semibold">Bài học trả phí</h2>
            <p className="text-sm text-muted-foreground">
              Bài học này thuộc nội dung trả phí. Hãy đăng ký khóa học để mở khóa toàn bộ
              bài giảng và tài liệu.
            </p>
            <Button asChild className="min-h-[44px] w-full">
              <Link href={backHref}>Xem thông tin khóa học</Link>
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {/* Completed banner */}
          {completed ? (
            <div className="flex items-center gap-2 rounded-lg border border-emerald-300 bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:border-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-300">
              <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden />
              Bạn đã hoàn thành bài học này
            </div>
          ) : null}

          {/* Video */}
          {lesson.videoUrl ? <LessonVideo url={lesson.videoUrl} title={lesson.title} /> : null}

          {/* Content */}
          {lesson.content ? (
            <Card>
              <CardContent className="p-4">
                <LessonContent content={lesson.content} />
              </CardContent>
            </Card>
          ) : !lesson.videoUrl ? (
            <div className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
              <FileText className="mx-auto mb-2 h-6 w-6 opacity-50" aria-hidden />
              Bài học này chưa có nội dung.
            </div>
          ) : null}

          {/* Resources */}
          {lesson.resources && lesson.resources.length > 0 ? (
            <Card>
              <CardContent className="p-4">
                <h3 className="mb-2 flex items-center gap-1.5 text-sm font-semibold">
                  <Paperclip className="h-4 w-4" aria-hidden /> Tài liệu
                </h3>
                <ul className="space-y-2">
                  {lesson.resources.map((r) => (
                    <li key={r.id}>
                      <a
                        href={r.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-2 text-sm text-primary underline underline-offset-2"
                      >
                        <Badge variant="outline" className="h-5 px-1.5 text-[10px]">
                          {r.type}
                        </Badge>
                        <span className="truncate">{r.title}</span>
                      </a>
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          ) : null}

          {/* Mark complete */}
          <Button
            onClick={() => complete.mutate(lessonId)}
            disabled={completed || complete.isPending}
            className="min-h-[44px] w-full"
            variant={completed ? 'secondary' : 'default'}
          >
            <CheckCircle2 className="mr-1.5 h-4 w-4" aria-hidden />
            {completed ? 'Đã hoàn thành' : complete.isPending ? 'Đang lưu…' : 'Đánh dấu hoàn thành'}
          </Button>
        </div>
      )}
    </StudentMobileShell>
  );
}
