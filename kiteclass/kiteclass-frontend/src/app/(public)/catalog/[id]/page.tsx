/**
 * Public per-tenant course detail (GAP-274 phase-2 — ported to kiteclass-public kit).
 *
 * Audience: phụ huynh xem chi tiết một khóa. Layout: hero nhỏ + price card (CTA học
 * thử) · mục tiêu · syllabus theo tuần · giáo viên phụ trách · lịch lớp đang mở ·
 * FAQ · aside cam kết · sticky mobile CTA.
 *
 * Anti-fabrication (GAP-958): every section renders ONLY from real data and HIDES
 * itself when the source is null/empty — no placeholder objectives, no fake schedule,
 * no invented teacher. Theme via `--theme-*` (ThemeSync in public layout).
 *
 * @author KiteClass Team
 */

import { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import {
  CheckCircle2,
  GraduationCap,
  CalendarDays,
  Target,
  BookOpen,
  ShieldCheck,
} from 'lucide-react';
import { publicApi, type PublicClass } from '@/lib/api/public';
import { getTenantLanding, landingStr, landingArray } from '@/lib/api/tenant-landing';
import type { Course } from '@/types/course';

interface CourseModule {
  id: number;
  title: string;
  orderNumber: number;
  lessons: { id: number; title: string; isTrial: boolean }[];
}

interface CourseDetailData extends Course {
  modules: CourseModule[];
  objectivesArray: string[];
}

function parseObjectives(raw?: string): string[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed.map(String).filter((s) => s.trim().length > 0);
  } catch {
    /* not JSON — fall through to split */
  }
  return raw
    .split(/[\n;]/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}

const getCourseData = async (id: string): Promise<CourseDetailData | null> => {
  const courseId = parseInt(id, 10);
  if (isNaN(courseId)) return null;
  try {
    const course = await publicApi.getCourseById(courseId);
    if (course.status !== 'PUBLISHED') return null;

    let modules: CourseModule[] = [];
    try {
      modules = (await publicApi.getCourseStructure(courseId)) || [];
    } catch {
      /* structure optional — continue without modules */
    }

    return { ...course, modules, objectivesArray: parseObjectives(course.objectives) };
  } catch {
    return null;
  }
};

const getOpenClasses = async (courseId: number): Promise<PublicClass[]> => {
  try {
    const page = await publicApi.getCourseClasses(courseId);
    return (page?.content ?? []).filter(
      (c) => c.status !== 'CANCELLED' && c.status !== 'COMPLETED' && c.status !== 'DRAFT'
    );
  } catch {
    return [];
  }
};

function formatPrice(price?: number | null): string | null {
  if (price === null || price === undefined || price === 0) return null;
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
}

function levelLabel(level?: string): string | null {
  switch (level) {
    case 'Beginner':
      return 'Cơ bản';
    case 'Intermediate':
      return 'Trung cấp';
    case 'Advanced':
      return 'Nâng cao';
    default:
      return level ?? null;
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const course = await getCourseData(id);
  if (!course) return { title: 'Không tìm thấy khóa học' };
  return { title: course.name, description: course.description };
}

export default async function CourseDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const course = await getCourseData(id);
  if (!course) notFound();

  const [classes, landing] = await Promise.all([
    getOpenClasses(course.id),
    getTenantLanding(),
  ]);

  const price = formatPrice(course.price);
  const lvl = levelLabel(course.level);
  const durationLabel = course.durationWeeks ? `${course.durationWeeks} tuần` : null;

  // Giáo viên phụ trách (real data only): teacherBio string OR first teacher entry.
  const teacherBio = landingStr(landing, 'teacherBio');
  const teachers = landingArray<{ name?: string; subject?: string; credentials?: string[] }>(
    landing,
    'teachers'
  );
  const teacher = teachers[0];
  const teacherName = teacher?.name?.trim() || null;
  const hasTeacher = Boolean(teacherBio || teacherName);

  const zaloUrl = landingStr(landing, 'zaloUrl');

  return (
    <div className="container mx-auto px-4 py-10 pb-28 lg:pb-12">
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="mb-6 text-sm text-muted-foreground">
        <Link href="/catalog" className="hover:text-theme-primary">
          Khóa học
        </Link>
        {' › '}
        <span className="text-foreground">{course.name}</span>
      </nav>

      {/* Hero nhỏ + price card */}
      <section className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <div className="mb-3 flex flex-wrap gap-2 text-xs">
            <span className="rounded bg-muted px-2 py-1 font-bold text-muted-foreground">Mã: {course.code}</span>
            {lvl && <span className="rounded bg-muted px-2 py-1 font-bold text-muted-foreground">{lvl}</span>}
            {durationLabel && (
              <span className="rounded bg-muted px-2 py-1 font-bold text-muted-foreground">{durationLabel}</span>
            )}
            {course.maxStudents && (
              <span className="rounded bg-muted px-2 py-1 font-bold text-muted-foreground">
                Sĩ số {course.maxStudents} HV
              </span>
            )}
          </div>
          <h1 className="mb-3 text-3xl font-extrabold md:text-4xl">{course.name}</h1>
          {course.description && (
            <p className="text-lg text-muted-foreground">{course.description}</p>
          )}
        </div>

        <aside className="lg:col-span-1">
          <div className="rounded-2xl border bg-white p-6 shadow-sm lg:sticky lg:top-20">
            <div className="text-3xl font-black text-theme-primary">{price ?? 'Liên hệ'}</div>
            <div className="mb-4 text-sm text-muted-foreground">
              {price
                ? durationLabel
                  ? `/ khóa ${durationLabel}`
                  : 'học phí trọn khóa'
                : 'Liên hệ để biết học phí'}
            </div>
            <Link
              href="/contact"
              className="mb-2 block rounded-xl bg-theme-cta py-3 text-center font-bold text-white"
            >
              Đăng ký học thử miễn phí
            </Link>
            {zaloUrl ? (
              <a
                href={zaloUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="block rounded-xl border py-3 text-center font-bold text-theme-primary hover:border-theme-primary"
              >
                Nhắn Zalo hỏi thêm
              </a>
            ) : (
              <Link
                href="/contact"
                className="block rounded-xl border py-3 text-center font-bold text-theme-primary hover:border-theme-primary"
              >
                Liên hệ tư vấn
              </Link>
            )}
          </div>
        </aside>
      </section>

      <div className="mt-10 grid gap-8 lg:grid-cols-3">
        <div className="space-y-8 lg:col-span-2">
          {/* Objectives — hidden when none (no fabricated fallback) */}
          {course.objectivesArray.length > 0 && (
            <section>
              <h2 className="mb-4 flex items-center gap-2 text-2xl font-bold">
                <Target className="h-6 w-6 text-theme-primary" aria-hidden="true" /> Con sẽ đạt được gì
              </h2>
              <ul className="space-y-3">
                {course.objectivesArray.map((obj, i) => (
                  <li key={i} className="flex items-start gap-3">
                    <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" aria-hidden="true" />
                    <span>{obj}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {/* Syllabus — modules preferred, else course.syllabus text; hidden when both empty */}
          {course.modules.length > 0 ? (
            <section>
              <h2 className="mb-4 flex items-center gap-2 text-2xl font-bold">
                <BookOpen className="h-6 w-6 text-theme-primary" aria-hidden="true" /> Nội dung khóa học
              </h2>
              <ol className="space-y-3">
                {course.modules.map((m) => (
                  <li key={m.id} className="rounded-xl border bg-white p-4">
                    <b className="block">{m.title}</b>
                    <small className="text-muted-foreground">{m.lessons?.length || 0} bài học</small>
                  </li>
                ))}
              </ol>
            </section>
          ) : course.syllabus ? (
            <section>
              <h2 className="mb-4 flex items-center gap-2 text-2xl font-bold">
                <BookOpen className="h-6 w-6 text-theme-primary" aria-hidden="true" /> Nội dung khóa học
              </h2>
              <p className="whitespace-pre-line rounded-xl border bg-white p-4 text-muted-foreground">
                {course.syllabus}
              </p>
            </section>
          ) : null}

          {/* Giáo viên phụ trách — hidden when tenant has no teacher data */}
          {hasTeacher && (
            <section>
              <h2 className="mb-4 flex items-center gap-2 text-2xl font-bold">
                <GraduationCap className="h-6 w-6 text-theme-primary" aria-hidden="true" /> Giáo viên phụ trách
              </h2>
              <div className="flex items-start gap-4 rounded-xl border bg-white p-4">
                <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-theme-primary/10 text-lg font-black text-theme-primary">
                  {(teacherName ?? 'GV').charAt(0)}
                </span>
                <div>
                  {teacherName && <b className="block">{teacherName}</b>}
                  {teacher?.subject && (
                    <div className="text-sm text-muted-foreground">{teacher.subject}</div>
                  )}
                  {teacherBio && <p className="mt-1 text-sm text-muted-foreground">{teacherBio}</p>}
                </div>
              </div>
            </section>
          )}

          {/* Lịch lớp đang mở — hidden when no open classes */}
          {classes.length > 0 && (
            <section>
              <h2 className="mb-4 flex items-center gap-2 text-2xl font-bold">
                <CalendarDays className="h-6 w-6 text-theme-primary" aria-hidden="true" /> Lịch lớp đang mở
              </h2>
              <div className="space-y-3" aria-live="polite">
                {classes.map((cls) => {
                  const seatsLeft =
                    cls.maxStudents != null && cls.currentEnrolled != null
                      ? cls.maxStudents - cls.currentEnrolled
                      : null;
                  const full = seatsLeft != null && seatsLeft <= 0;
                  return (
                    <div
                      key={cls.id}
                      className="flex items-center justify-between gap-3 rounded-xl border bg-white p-4"
                    >
                      <div>
                        <b>{cls.name}</b>
                        {(cls.schedule || cls.startDate) && (
                          <small className="block text-muted-foreground">
                            {[cls.schedule, cls.startDate ? `Khai giảng ${cls.startDate}` : null]
                              .filter(Boolean)
                              .join(' · ')}
                          </small>
                        )}
                      </div>
                      <span
                        className={`shrink-0 rounded-full px-3 py-1 text-xs font-extrabold ${
                          full ? 'bg-red-50 text-red-800' : 'bg-emerald-50 text-emerald-800'
                        }`}
                      >
                        {full
                          ? 'Đã đầy'
                          : seatsLeft != null
                            ? `Còn ${seatsLeft} chỗ`
                            : 'Đang tuyển sinh'}
                      </span>
                    </div>
                  );
                })}
              </div>
            </section>
          )}
        </div>

        {/* Aside — commitments (generic teacher commitments, not fabricated stats) */}
        <aside className="lg:col-span-1">
          <div className="rounded-2xl border bg-white p-6 shadow-sm">
            <h3 className="mb-4 flex items-center gap-2 font-bold">
              <ShieldCheck className="h-5 w-5 text-theme-primary" aria-hidden="true" /> Cam kết
            </h3>
            <ul className="space-y-2.5 text-sm">
              {['Học thử 1 buổi miễn phí', 'Sĩ số nhỏ, kèm sát từng học viên', 'Báo cáo tiến độ thường xuyên'].map(
                (t) => (
                  <li key={t} className="flex items-start gap-2">
                    <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" aria-hidden="true" />
                    <span>{t}</span>
                  </li>
                )
              )}
            </ul>
          </div>
        </aside>
      </div>

      {/* Sticky mobile CTA */}
      <div className="fixed inset-x-0 bottom-0 z-40 flex items-center justify-between gap-3 border-t bg-white px-4 py-3 shadow-[0_-4px_16px_rgba(0,0,0,0.08)] lg:hidden">
        <span className="font-black text-theme-primary">{price ?? 'Liên hệ'}</span>
        <Link href="/contact" className="rounded-xl bg-theme-cta px-5 py-2.5 font-bold text-white">
          Đăng ký học thử
        </Link>
      </div>
    </div>
  );
}
