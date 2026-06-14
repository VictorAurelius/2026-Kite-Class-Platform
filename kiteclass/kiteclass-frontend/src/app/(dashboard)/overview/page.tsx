/**
 * Dashboard home (route-group index) — KC pro v2 foundation showcase.
 *
 * Wave 30 Bucket A: refactored to apply kiteclass-pro-v2 design tokens via
 * the new dashboard-foundation primitives.
 *
 * GAP-805 Bucket C (2026-05-28): replaced hardcoded literal KPI values with
 * real-data derivation. The previous version shipped static literals ("428
 * học viên · 24 lớp · ₫42M doanh thu") plus a hardcoded subtitle ("Hôm nay có
 * 12 buổi học · 3 lớp chờ điểm danh") that did NOT match any tenant DB — an
 * outside-in audit flagged this as obviously fake to a demo reviewer.
 *
 * Real-data strategy (no dedicated /dashboard/stats endpoint exists yet):
 *   - "Học viên"      → real total from GET /api/v1/students (PaginatedResponse.totalElements)
 *   - "Khóa học"      → real total from GET /api/v1/courses  (PaginatedResponse.totalElements)
 *   - Metrics without a backing endpoint (Giáo viên / Điểm danh / Doanh thu /
 *     Tỷ lệ giữ chân) render a neutral "—" placeholder with a "Sắp có" hint
 *     rather than a misleading literal. Sparklines/deltas are omitted for these
 *     so no fabricated trend is shown.
 *
 * When a future GAP ships GET /api/v1/dashboard/stats, swap the two list-count
 * derivations + the four placeholders for the aggregated response.
 *
 * Source spec: kiteclass-pro-v2/screens/dashboard-default.html.
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import {
  ArrowRight,
  BookOpen,
  CalendarCheck,
  CreditCard,
  GraduationCap,
  Inbox,
  TrendingUp,
  Users,
} from 'lucide-react';
import { KPICard, type KPIData } from '@/_shared/dashboard-foundation';
import { DashboardLayout } from '@/components/layout';
import { useStudents } from '@/hooks/use-students';
import { useCourses } from '@/hooks/use-courses';

/** Placeholder shown for metrics that have no backing endpoint yet. */
const PLACEHOLDER_VALUE = '—';

export default function DashboardHomePage() {
  // Fetch a single page only — we just need the `totalElements` count, not the
  // rows. size=1 keeps the payload minimal.
  const studentsQuery = useStudents({ page: 0, size: 1 });
  const coursesQuery = useCourses({ page: 0, size: 1 });

  const studentTotal = studentsQuery.data?.totalElements;
  const courseTotal = coursesQuery.data?.totalElements;

  // Format a count for display: real number when loaded, "…" while loading,
  // placeholder dash when the request failed (avoid showing a fake number).
  const formatCount = (
    value: number | undefined,
    isLoading: boolean,
    isError: boolean,
  ): string => {
    if (isLoading) return '…';
    if (isError || value === undefined) return PLACEHOLDER_VALUE;
    return value.toLocaleString('vi-VN');
  };

  const kpiRow: KPIData[] = [
    {
      label: 'Học viên',
      value: formatCount(studentTotal, studentsQuery.isLoading, studentsQuery.isError),
      tone: 'neutral',
      icon: <Users className="h-4 w-4" />,
    },
    {
      label: 'Khóa học',
      value: formatCount(courseTotal, coursesQuery.isLoading, coursesQuery.isError),
      tone: 'neutral',
      icon: <BookOpen className="h-4 w-4" />,
    },
    {
      label: 'Giáo viên',
      value: PLACEHOLDER_VALUE,
      tone: 'neutral',
      icon: <GraduationCap className="h-4 w-4" />,
    },
    {
      label: 'Điểm danh hôm nay',
      value: PLACEHOLDER_VALUE,
      tone: 'neutral',
      icon: <CalendarCheck className="h-4 w-4" />,
    },
    {
      label: 'Doanh thu tuần',
      value: PLACEHOLDER_VALUE,
      tone: 'neutral',
      icon: <CreditCard className="h-4 w-4" />,
    },
    {
      label: 'Tỷ lệ giữ chân',
      value: PLACEHOLDER_VALUE,
      tone: 'neutral',
      icon: <TrendingUp className="h-4 w-4" />,
    },
  ];

  // Subtitle derived from real data instead of a hardcoded "Hôm nay có 12 buổi
  // học" sentence that never matched the DB.
  const subtitle =
    studentTotal !== undefined && courseTotal !== undefined
      ? `Trung tâm hiện có ${studentTotal.toLocaleString('vi-VN')} học viên · ${courseTotal.toLocaleString('vi-VN')} khóa học.`
      : 'Đang tải số liệu trung tâm…';

  return (
    <DashboardLayout>
    <div className="space-y-6 p-6">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Tổng quan</h1>
          <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>
        </div>
      </header>

      <section
        aria-label="Chỉ số trung tâm"
        className="grid gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-3 xl:grid-cols-6"
      >
        {kpiRow.map((kpi) => (
          <KPICard key={kpi.label} {...kpi} />
        ))}
      </section>

      {/* GAP-1379: recent-activity placeholder upgraded from a plain explanatory
          paragraph to an empty-state (icon + message + onboarding CTA) so the
          first screen after login reads as "ready, add your data" rather than
          "unfinished". The 4 KPI tiles stay honest "—" (GAP-805 anti-fake-data)
          until GET /api/v1/dashboard/stats ships. */}
      <section
        aria-label="Hoạt động gần đây"
        className="rounded-xl border border-border bg-card p-6"
      >
        <h2 className="text-sm font-semibold text-foreground">Hoạt động gần đây</h2>
        <div className="mt-4 flex flex-col items-center justify-center rounded-lg border border-dashed border-border py-10 text-center">
          <div className="rounded-full bg-muted p-3 text-muted-foreground">
            <Inbox className="h-6 w-6" aria-hidden="true" />
          </div>
          <p className="mt-3 text-sm font-medium text-foreground">
            Chưa có hoạt động để hiển thị
          </p>
          <p className="mt-1 max-w-md text-sm text-muted-foreground">
            Bắt đầu bằng cách thêm học viên và tạo lớp học — các hoạt động gần đây
            sẽ xuất hiện tại đây khi trung tâm vận hành.
          </p>
          <div className="mt-5 flex flex-wrap items-center justify-center gap-3">
            <Link
              href="/students"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
            >
              Quản lý học viên <ArrowRight className="h-4 w-4" aria-hidden="true" />
            </Link>
            <Link
              href="/courses"
              className="inline-flex items-center gap-2 rounded-lg border border-border px-4 py-2 text-sm font-semibold transition-colors hover:bg-accent/40"
            >
              Quản lý khóa học
            </Link>
          </div>
        </div>
      </section>
    </div>
    </DashboardLayout>
  );
}
