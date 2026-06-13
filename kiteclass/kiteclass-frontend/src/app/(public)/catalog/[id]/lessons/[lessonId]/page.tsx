/**
 * Guest trial lesson viewer (GAP-1113 Increment A, Bucket B).
 *
 * Renders a single TRIAL lesson for anonymous prospects (no auth). The backend guest
 * endpoint returns trial lessons only; a paid lesson id resolves to 403/404 → we show
 * a paywall CTA, NEVER a raw error (per wave plan + `pre-handoff-self-test` §2.2).
 *
 * Theme via `--theme-*` (ThemeSync in public layout). Anti-fabrication: content shown
 * only from real data; missing pieces hide themselves.
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave RBAC-LMS-FE Increment A)
 */

import Link from 'next/link';
import { Metadata } from 'next';
import { ArrowLeft, Lock, PlayCircle, BookOpen } from 'lucide-react';
import { publicApi, type PublicLesson } from '@/lib/api/public';

const getTrialLesson = async (lessonId: string): Promise<PublicLesson | null> => {
  const id = parseInt(lessonId, 10);
  if (isNaN(id)) return null;
  try {
    const lesson = await publicApi.getTrialLesson(id);
    // Defensive: only surface lessons the backend marked trial (guest scope).
    if (lesson && lesson.isTrial === false) return null;
    return lesson ?? null;
  } catch {
    // 403/404 (paid lesson / not found) → treat as locked, show paywall.
    return null;
  }
};

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string; lessonId: string }>;
}): Promise<Metadata> {
  const { lessonId } = await params;
  const lesson = await getTrialLesson(lessonId);
  return { title: lesson ? `Học thử: ${lesson.title}` : 'Bài học thử' };
}

export default async function TrialLessonPage({
  params,
}: {
  params: Promise<{ id: string; lessonId: string }>;
}) {
  const { id, lessonId } = await params;
  const lesson = await getTrialLesson(lessonId);

  return (
    <div className="container mx-auto max-w-3xl px-4 py-10">
      <nav aria-label="Breadcrumb" className="mb-6 text-sm text-muted-foreground">
        <Link href="/catalog" className="hover:text-theme-primary">Khóa học</Link>
        {' › '}
        <Link href={`/catalog/${id}`} className="hover:text-theme-primary">Chi tiết khóa học</Link>
        {' › '}
        <span className="text-foreground">{lesson ? lesson.title : 'Bài học'}</span>
      </nav>

      {lesson ? (
        <article className="space-y-6">
          <header>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-theme-primary/10 px-3 py-1 text-xs font-bold text-theme-primary">
              <PlayCircle className="h-3.5 w-3.5" aria-hidden="true" /> Học thử miễn phí
            </span>
            <h1 className="mt-3 flex items-center gap-2 text-3xl font-extrabold">
              <BookOpen className="h-7 w-7 text-theme-primary" aria-hidden="true" />
              {lesson.title}
            </h1>
            {lesson.estimatedDuration != null && (
              <p className="mt-1 text-sm text-muted-foreground">Thời lượng: {lesson.estimatedDuration} phút</p>
            )}
          </header>

          {lesson.videoUrl && (
            <a
              href={lesson.videoUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-xl border bg-white px-4 py-3 font-semibold text-theme-primary hover:border-theme-primary"
            >
              <PlayCircle className="h-5 w-5" aria-hidden="true" /> Xem video bài học
            </a>
          )}

          {lesson.content ? (
            <div className="prose max-w-none whitespace-pre-line rounded-xl border bg-white p-6 text-foreground">
              {lesson.content}
            </div>
          ) : !lesson.videoUrl ? (
            <p className="rounded-xl border bg-white p-6 text-muted-foreground">
              Bài học thử này chưa có nội dung văn bản. Đăng ký để truy cập đầy đủ khóa học.
            </p>
          ) : null}

          {/* CTA to enroll */}
          <div className="rounded-2xl border bg-theme-primary/5 p-6 text-center">
            <p className="mb-3 font-semibold">Thích bài học này? Đăng ký để mở khóa toàn bộ khóa học.</p>
            <Link
              href="/contact"
              className="inline-block rounded-xl bg-theme-cta px-6 py-3 font-bold text-white"
            >
              Đăng ký học thử miễn phí
            </Link>
          </div>
        </article>
      ) : (
        /* Paywall — locked lesson OR not found. Never a raw 403. */
        <div className="rounded-2xl border bg-white p-8 text-center">
          <Lock className="mx-auto mb-3 h-10 w-10 text-muted-foreground" aria-hidden="true" />
          <h1 className="text-2xl font-bold">Bài học này cần đăng ký</h1>
          <p className="mt-2 text-muted-foreground">
            Đây là bài học trả phí hoặc chỉ dành cho học viên đã đăng ký. Hãy đăng ký để
            mở khóa toàn bộ nội dung khóa học.
          </p>
          <div className="mt-5 flex flex-wrap justify-center gap-3">
            <Link href="/contact" className="rounded-xl bg-theme-cta px-6 py-3 font-bold text-white">
              Đăng ký học thử miễn phí
            </Link>
            <Link
              href={`/catalog/${id}`}
              className="inline-flex items-center gap-1.5 rounded-xl border px-6 py-3 font-bold text-theme-primary hover:border-theme-primary"
            >
              <ArrowLeft className="h-4 w-4" aria-hidden="true" /> Quay lại khóa học
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
