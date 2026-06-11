/**
 * Public per-tenant course catalog (GAP-274 phase-2 — ported to kiteclass-public kit).
 *
 * Audience: phụ huynh tìm lớp cho con. Persona: P1 Solo Teacher tenant.
 * - Search (client) + grade-level + trình-độ filter chips + sort.
 * - Persona-based recommendations ("Gợi ý cho con anh/chị") mapping the parent's
 *   situation → a real course in the tenant catalog (level + name heuristic).
 * - Theme: `--theme-*` CSS vars injected per-tenant by ThemeSync (public layout).
 *
 * Data honesty (GAP-958): courses come from publicApi.getCourses (PUBLISHED only).
 * The backend /api/v1/courses endpoint does NOT accept level/category params and the
 * Course model has no grade-level field, so level + grade filtering + reco mapping are
 * client-side heuristics over the fetched page (search + sort use the real query/sort
 * params; one page sized generously covers a solo-teacher catalog).
 *
 * @author KiteClass Team
 */

'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { Search, Loader2, BookOpen, Lightbulb } from 'lucide-react';
import { publicApi } from '@/lib/api/public';
import type { Course } from '@/types/course';

const PAGE_SIZE = 60;

type GradeKey = 'all' | 'lop4' | 'lop5' | 'onthi';
type LevelKey = 'all' | 'co-ban' | 'nang-cao';
type SortKey = 'newest' | 'price-asc' | 'price-desc' | 'name';
type RecoKey = 'mat-goc' | 'theo-kip' | 'thi-vao-6';

const GRADE_CHIPS: { key: GradeKey; label: string }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'lop4', label: 'Lớp 4' },
  { key: 'lop5', label: 'Lớp 5' },
  { key: 'onthi', label: 'Ôn thi vào 6' },
];

const LEVEL_CHIPS: { key: LevelKey; label: string }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'co-ban', label: 'Cơ bản' },
  { key: 'nang-cao', label: 'Nâng cao' },
];

const RECO_OPTIONS: { key: RecoKey; label: string; sub: string }[] = [
  { key: 'mat-goc', label: 'Con đang mất gốc', sub: 'Cần lấy lại nền tảng' },
  { key: 'theo-kip', label: 'Học theo kịp lớp', sub: 'Củng cố đều đặn' },
  { key: 'thi-vao-6', label: 'Chuẩn bị thi vào 6', sub: 'Mục tiêu trường tốt' },
];

function formatPrice(price?: number | null): string {
  if (price === null || price === undefined || price === 0) return 'Miễn phí';
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

const isBasic = (c: Course) => c.level === 'Beginner';
const isAdvanced = (c: Course) => c.level === 'Advanced' || c.level === 'Intermediate';
const haystack = (c: Course) => `${c.name} ${c.description ?? ''}`.toLowerCase();

function matchesGrade(c: Course, grade: GradeKey): boolean {
  const h = haystack(c);
  switch (grade) {
    case 'lop4':
      return /lớp\s*4|lop\s*4/.test(h);
    case 'lop5':
      return /lớp\s*5|lop\s*5/.test(h);
    case 'onthi':
      return /ôn thi|vào 6|vào lớp 6|chuyển cấp/.test(h);
    default:
      return true;
  }
}

function matchesLevel(c: Course, level: LevelKey): boolean {
  if (level === 'co-ban') return isBasic(c);
  if (level === 'nang-cao') return isAdvanced(c);
  return true;
}

/** Map the parent's situation → a real course in the catalog (heuristic over level + name). */
function recommend(courses: Course[], reco: RecoKey): Course | null {
  if (courses.length === 0) return null;
  if (reco === 'mat-goc') return courses.find(isBasic) ?? courses[0] ?? null;
  if (reco === 'theo-kip')
    return courses.find((c) => c.level === 'Intermediate') ?? courses.find(isBasic) ?? courses[0] ?? null;
  // thi-vao-6
  return (
    courses.find((c) => /ôn thi|vào 6|nâng cao/.test(haystack(c))) ??
    courses.find(isAdvanced) ??
    courses[0] ??
    null
  );
}

export default function CatalogPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [grade, setGrade] = useState<GradeKey>('all');
  const [level, setLevel] = useState<LevelKey>('all');
  const [sortBy, setSortBy] = useState<SortKey>('newest');
  const [reco, setReco] = useState<RecoKey>('mat-goc');

  const { data, isLoading, error } = useQuery({
    queryKey: ['publicCourses', 'catalog'],
    queryFn: () => publicApi.getCourses({ page: 0, size: PAGE_SIZE }),
    retry: 1,
  });

  const allCourses = useMemo(() => data?.content ?? [], [data]);

  const visible = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    const filtered = allCourses.filter(
      (c) =>
        matchesGrade(c, grade) &&
        matchesLevel(c, level) &&
        (!term || haystack(c).includes(term))
    );
    return [...filtered].sort((a, b) => {
      if (sortBy === 'price-asc') return (a.price ?? 0) - (b.price ?? 0);
      if (sortBy === 'price-desc') return (b.price ?? 0) - (a.price ?? 0);
      if (sortBy === 'name') return a.name.localeCompare(b.name, 'vi');
      // newest
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });
  }, [allCourses, searchTerm, grade, level, sortBy]);

  const recommended = useMemo(() => recommend(allCourses, reco), [allCourses, reco]);

  const resetFilters = () => {
    setSearchTerm('');
    setGrade('all');
    setLevel('all');
  };

  return (
    <div>
      {/* Page head */}
      <div className="bg-gradient-to-br from-theme-primary to-theme-secondary text-white">
        <div className="container mx-auto px-4 py-12">
          <span className="mb-3 inline-flex rounded-full bg-white/15 px-3 py-1.5 text-xs font-extrabold uppercase tracking-wider">
            Khóa học đang mở
          </span>
          <h1 className="text-3xl font-extrabold md:text-4xl">Tìm lớp học phù hợp cho con</h1>
          <p className="mt-2 max-w-2xl text-white/90">
            Các khóa học đang được trực tiếp giảng dạy — sĩ số nhỏ, báo cáo tiến độ thường xuyên.
          </p>
        </div>
      </div>

      <div className="container mx-auto px-4 pb-16">
        {/* Toolbar */}
        <form
          role="search"
          onSubmit={(e) => e.preventDefault()}
          className="-mt-7 grid gap-4 rounded-2xl border bg-white p-4 shadow-sm"
        >
          <div className="flex flex-wrap items-center gap-3">
            <label className="flex flex-1 items-center gap-2 rounded-xl border bg-muted/40 px-3 focus-within:border-theme-primary focus-within:bg-white">
              <Search className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
              <input
                type="search"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm theo tên khóa, lớp, chủ đề…"
                aria-label="Tìm khóa học"
                autoComplete="off"
                className="flex-1 bg-transparent py-3 text-sm outline-none"
              />
            </label>
            <div className="flex items-center gap-2">
              <label htmlFor="sort" className="text-sm font-bold text-muted-foreground">
                Sắp xếp
              </label>
              <select
                id="sort"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as SortKey)}
                className="rounded-xl border bg-white px-3 py-2.5 text-sm font-semibold"
              >
                <option value="newest">Mới khai giảng</option>
                <option value="price-asc">Giá thấp → cao</option>
                <option value="price-desc">Giá cao → thấp</option>
                <option value="name">Theo tên</option>
              </select>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
            <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Lọc theo cấp lớp">
              <span className="text-xs font-extrabold uppercase tracking-wide text-muted-foreground">
                Cấp lớp
              </span>
              {GRADE_CHIPS.map((chip) => (
                <button
                  key={chip.key}
                  type="button"
                  aria-pressed={grade === chip.key}
                  onClick={() => setGrade(chip.key)}
                  className={`rounded-full border px-4 py-1.5 text-sm font-bold transition ${
                    grade === chip.key
                      ? 'border-theme-primary bg-theme-primary text-white'
                      : 'bg-white hover:border-theme-primary'
                  }`}
                >
                  {chip.label}
                </button>
              ))}
            </div>
            <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Lọc theo trình độ">
              <span className="text-xs font-extrabold uppercase tracking-wide text-muted-foreground">
                Trình độ
              </span>
              {LEVEL_CHIPS.map((chip) => (
                <button
                  key={chip.key}
                  type="button"
                  aria-pressed={level === chip.key}
                  onClick={() => setLevel(chip.key)}
                  className={`rounded-full border px-4 py-1.5 text-sm font-bold transition ${
                    level === chip.key
                      ? 'border-theme-primary bg-theme-primary text-white'
                      : 'bg-white hover:border-theme-primary'
                  }`}
                >
                  {chip.label}
                </button>
              ))}
            </div>
            {!isLoading && !error && (
              <span className="text-sm font-bold text-muted-foreground" aria-live="polite">
                Hiển thị <b className="text-theme-primary">{visible.length}</b> khóa học
              </span>
            )}
          </div>
        </form>

        {/* Loading */}
        {isLoading && (
          <div className="flex items-center justify-center py-16 text-muted-foreground">
            <Loader2 className="h-7 w-7 animate-spin text-theme-primary" aria-hidden="true" />
            <span className="ml-2">Đang tải khóa học...</span>
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="py-16 text-center">
            <p className="mb-4 text-destructive">Không thể tải danh sách khóa học</p>
            <button
              onClick={() => window.location.reload()}
              className="rounded-xl bg-theme-primary px-5 py-2.5 font-bold text-white"
            >
              Thử lại
            </button>
          </div>
        )}

        {/* Course grid */}
        {!isLoading && !error && visible.length > 0 && (
          <section className="py-9" aria-label="Danh sách khóa học">
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {visible.map((course) => {
                const lvl = levelLabel(course.level);
                return (
                  <article
                    key={course.id}
                    className="flex flex-col overflow-hidden rounded-2xl border bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-md"
                  >
                    <div className="relative flex aspect-video items-center justify-center bg-gradient-to-br from-theme-primary to-theme-secondary">
                      <BookOpen className="h-12 w-12 text-white/90" aria-hidden="true" />
                      <span className="absolute left-3 top-3 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-extrabold text-emerald-800">
                        Đang tuyển sinh
                      </span>
                      <span className="absolute right-3 top-3 rounded bg-black/55 px-2 py-1 text-[11px] font-bold tracking-wide text-white">
                        {course.code}
                      </span>
                    </div>
                    <div className="flex flex-1 flex-col gap-2.5 p-5">
                      <div className="flex flex-wrap gap-2 text-xs">
                        {lvl && (
                          <span className="rounded bg-muted px-2 py-0.5 font-bold text-muted-foreground">{lvl}</span>
                        )}
                        {course.durationWeeks && (
                          <span className="rounded bg-muted px-2 py-0.5 font-bold text-muted-foreground">
                            {course.durationWeeks} tuần
                          </span>
                        )}
                      </div>
                      <h3 className="text-lg font-extrabold">{course.name}</h3>
                      <p className="line-clamp-3 flex-1 text-sm text-muted-foreground">
                        {course.description || 'Khóa học chất lượng cao, sĩ số nhỏ.'}
                      </p>
                      <div className="mt-1 flex items-center justify-between gap-3 border-t pt-3.5">
                        <span className="text-lg font-black text-theme-cta">
                          {formatPrice(course.price)}
                          {course.durationWeeks ? (
                            <small className="block text-xs font-semibold text-muted-foreground">
                              / khóa {course.durationWeeks} tuần
                            </small>
                          ) : null}
                        </span>
                        <Link
                          href={`/catalog/${course.id}`}
                          className="rounded-xl bg-theme-primary px-4 py-2 text-sm font-bold text-white"
                        >
                          Xem chi tiết
                        </Link>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          </section>
        )}

        {/* Empty state */}
        {!isLoading && !error && visible.length === 0 && (
          <div
            role="status"
            className="my-9 rounded-2xl border-2 border-dashed bg-muted/40 px-5 py-14 text-center"
          >
            <div className="text-5xl" aria-hidden="true">
              🔍
            </div>
            <h3 className="mb-2 mt-3 text-xl font-extrabold">Chưa có khóa nào khớp bộ lọc</h3>
            <p className="mx-auto mb-5 max-w-md text-muted-foreground">
              Hãy thử bỏ bớt bộ lọc, hoặc liên hệ trực tiếp để được tư vấn lộ trình phù hợp cho con.
            </p>
            <div className="flex flex-wrap justify-center gap-3">
              {allCourses.length > 0 && (
                <button
                  type="button"
                  onClick={resetFilters}
                  className="rounded-xl border px-5 py-2.5 font-bold hover:border-theme-primary"
                >
                  Xóa bộ lọc
                </button>
              )}
              <Link href="/contact" className="rounded-xl bg-theme-cta px-5 py-2.5 font-bold text-white">
                Nhắn tư vấn lộ trình
              </Link>
            </div>
          </div>
        )}

        {/* Persona recommendations (GAP-274 AC) */}
        {!isLoading && !error && allCourses.length > 0 && (
          <section
            aria-labelledby="reco-title"
            className="mt-8 rounded-2xl border bg-muted/40 p-6"
          >
            <div className="mb-1 flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-theme-primary/10 text-theme-primary">
                <Lightbulb className="h-5 w-5" aria-hidden="true" />
              </span>
              <h2 id="reco-title" className="text-xl font-extrabold">
                Gợi ý cho con anh/chị
              </h2>
            </div>
            <p className="mb-4 text-sm text-muted-foreground">
              Chọn tình huống của con để được gợi ý khóa phù hợp nhất theo trình độ &amp; mục tiêu:
            </p>
            <div className="mb-4 flex flex-wrap gap-2.5" role="group" aria-label="Chọn tình huống học viên">
              {RECO_OPTIONS.map((opt) => (
                <button
                  key={opt.key}
                  type="button"
                  aria-pressed={reco === opt.key}
                  onClick={() => setReco(opt.key)}
                  className={`rounded-xl border px-4 py-2.5 text-left text-sm font-bold leading-tight transition ${
                    reco === opt.key
                      ? 'border-theme-primary bg-theme-primary/5 ring-1 ring-theme-primary'
                      : 'bg-white hover:border-theme-primary'
                  }`}
                >
                  {opt.label}
                  <small className="block text-xs font-semibold text-muted-foreground">{opt.sub}</small>
                </button>
              ))}
            </div>
            <div
              aria-live="polite"
              className="flex flex-col gap-3 rounded-xl border bg-white p-4 sm:flex-row sm:items-center"
            >
              {recommended ? (
                <>
                  <BookOpen className="h-7 w-7 shrink-0 text-theme-primary" aria-hidden="true" />
                  <div className="flex-1">
                    <b className="text-base">{recommended.name}</b>
                    <p className="mt-0.5 text-sm text-muted-foreground">
                      {recommended.description || 'Khóa phù hợp với tình huống đã chọn.'}
                    </p>
                  </div>
                  <Link
                    href={`/catalog/${recommended.id}`}
                    className="shrink-0 rounded-xl bg-theme-primary px-4 py-2 text-sm font-bold text-white"
                  >
                    Xem khóa
                  </Link>
                </>
              ) : (
                <>
                  <div className="flex-1 text-sm text-muted-foreground">
                    Chưa có khóa phù hợp sẵn — hãy để lại lời nhắn, giáo viên sẽ tư vấn lộ trình riêng.
                  </div>
                  <Link
                    href="/contact"
                    className="shrink-0 rounded-xl bg-theme-cta px-4 py-2 text-sm font-bold text-white"
                  >
                    Liên hệ tư vấn
                  </Link>
                </>
              )}
            </div>
          </section>
        )}

        {/* Not-found CTA */}
        {!isLoading && !error && (
          <section className="mt-8 flex flex-wrap items-center justify-between gap-5 rounded-2xl bg-gradient-to-br from-theme-primary to-theme-secondary p-8 text-white">
            <div>
              <h2 className="text-xl font-extrabold">Không tìm thấy khóa phù hợp?</h2>
              <p className="mt-1 text-white/90">
                Nhận tư vấn lộ trình riêng &amp; mở lớp theo nhu cầu — phản hồi trong ngày.
              </p>
            </div>
            <Link
              href="/contact"
              className="rounded-xl bg-white px-5 py-2.5 font-bold text-theme-primary"
            >
              Liên hệ tư vấn miễn phí
            </Link>
          </section>
        )}
      </div>
    </div>
  );
}
