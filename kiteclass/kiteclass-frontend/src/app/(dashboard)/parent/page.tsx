/**
 * Parent portal home — mobile-first PWA shell with hero attendance metric,
 * children list (Wave 18b1 hook preserved), quick stats, and recent activity.
 *
 * Wave 49 Bucket A (GAP-267): re-skinned the Wave 18b1 children-list page
 * (was a desktop card grid) into the kc-parent kit's mobile shell while
 * preserving the `useMyChildren` + `useParentMe` hooks and the `Xem học bạ`
 * link to `/parent/transcript/[childId]`. The skeleton shows the hero +
 * child cards + activity feed; auxiliary tabs (attendance, billing, grades,
 * settings) live as sibling routes.
 *
 * Empty / loading / error states match `home-empty.html`, `home-loading.html`,
 * `home-error.html` from `ui_kits/kiteclass-parent/screens/`.
 *
 * @author KiteClass Team
 * @since 2.49.0 (Wave 49 — GAP-267 Bucket A; supersedes 2.18.0 Wave 18b1
 *                desktop layout but preserves all hook calls and the
 *                transcript drill-in route).
 */

'use client';

export const dynamic = 'force-dynamic';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  CheckCircle2,
  Clock,
  GraduationCap,
  MessageSquare,
  Receipt,
  Shield,
  Wallet,
} from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { UserType } from '@/types/auth';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { ErrorAlert } from '@/components/common/error-alert';
import { useMyChildren, useParentMe } from '@/hooks/use-parent';
import { ParentShell } from '@/components/parent/parent-shell';
import { HeroMetric } from '@/components/parent/hero-metric';
import { ChildCard } from '@/components/parent/child-card';
import { ActivityRow } from '@/components/parent/activity-row';
import { MOCK_ACTIVITIES } from '@/components/parent/parent-mock-data';

const ICON_FOR_KEY = {
  check: CheckCircle2,
  shield: Shield,
  clock: Clock,
  message: MessageSquare,
} as const;

export default function ParentDashboardPage() {
  const router = useRouter();
  const userType = useAuthStore((state) => state.user?.userType);

  // GAP-758: explicit PARENT-only REQUIRE (not !== bypass). Bounces
  // undefined-userType (KH JWT shape mismatch per GAP-725) + non-Parent
  // userType back to /dashboard. Prevents Owner JWT seeing parent portal
  // broken state in Phase 1 BETA.
  useEffect(() => {
    if (userType !== UserType.PARENT) {
      router.replace('/dashboard');
    }
  }, [userType, router]);

  // Hard short-circuit render — page returns null until userType verified.
  if (userType !== UserType.PARENT) {
    return null;
  }

  const { data: profile, isLoading: loadingMe } = useParentMe();
  const {
    data: children,
    isLoading: loadingKids,
    isError,
    error,
    refetch,
  } = useMyChildren();

  const greeting = profile
    ? `Chào ${profile.fullName}`
    : 'Cổng phụ huynh';
  const subtitle = `Tháng ${new Date().getMonth() + 1} — Năm học ${currentSchoolYear()}`;

  if (loadingMe || loadingKids) {
    return (
      <ParentShell title={greeting} subtitle={subtitle}>
        <div
          className="flex min-h-[60vh] items-center justify-center"
          data-testid="parent-home-loading"
        >
          <LoadingSpinner size="lg" text="Đang tải dữ liệu phụ huynh..." />
        </div>
      </ParentShell>
    );
  }

  if (isError) {
    return (
      <ParentShell title={greeting} subtitle={subtitle}>
        <div className="p-4" data-testid="parent-home-error">
          <ErrorAlert
            title="Không tải được danh sách con"
            message={error instanceof Error ? error.message : 'Lỗi hệ thống'}
            onRetry={() => refetch()}
          />
        </div>
      </ParentShell>
    );
  }

  const list = children ?? [];

  return (
    <ParentShell title={greeting} subtitle={subtitle} unreadCount={3}>
      <HeroMetric
        label="Tỷ lệ đi học của con tháng này"
        value={92}
        suffix="%"
        delta={{ direction: 'up', label: 'Tăng 4% so với tháng trước' }}
        data-testid="parent-home-hero"
      />

      {/* Children list — Wave 18b1 logic preserved. */}
      {list.length === 0 ? (
        <div
          className="mx-4 mt-6 rounded-2xl border bg-card p-6 text-center"
          data-testid="parent-home-empty"
        >
          <GraduationCap
            className="mx-auto mb-3 h-10 w-10 text-muted-foreground"
            aria-hidden
          />
          <p className="text-sm font-semibold">Chưa có con nào được liên kết</p>
          <p className="mt-1 text-xs text-muted-foreground">
            Liên hệ nhà trường để được gửi lại lời mời liên kết tài khoản phụ
            huynh.
          </p>
        </div>
      ) : (
        <ul className="space-y-0" data-testid="parent-home-children">
          {list.map((child) => (
            <li key={child.studentId}>
              <ChildCard child={child} />
            </li>
          ))}
        </ul>
      )}

      {/* Quick-stat tiles. */}
      <section className="mx-4 mt-4 grid grid-cols-2 gap-3">
        <Link
          href="/parent/grades"
          className="rounded-2xl border bg-card p-4 text-left shadow-sm transition-colors hover:bg-muted/40 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          data-testid="parent-home-stat-gpa"
        >
          <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
            Điểm trung bình
          </p>
          <p className="mt-1 text-3xl font-extrabold tracking-tight">8.4</p>
          <p className="mt-2 inline-flex items-center rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">
            Học lực Giỏi
          </p>
        </Link>
        <Link
          href="/parent/billing"
          className="rounded-2xl border bg-card p-4 text-left shadow-sm transition-colors hover:bg-muted/40 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          data-testid="parent-home-stat-billing"
        >
          <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
            Học phí
          </p>
          <p className="mt-1 text-lg font-bold">Đã đóng</p>
          <p className="mt-2 inline-flex items-center rounded-full bg-sky-100 px-2 py-0.5 text-[11px] font-semibold text-sky-700">
            Kỳ I · 2025-2026
          </p>
        </Link>
      </section>

      {/* Recent activity. */}
      <section className="mx-4 mt-6">
        <header className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-bold">Hoạt động gần đây</h2>
          <Link
            href="/parent/grades"
            className="text-xs font-semibold text-primary hover:underline"
          >
            Xem tất cả
          </Link>
        </header>
        <ul
          className="rounded-2xl border bg-card px-4"
          data-testid="parent-home-activity"
        >
          {MOCK_ACTIVITIES.map((act) => {
            const Icon = ICON_FOR_KEY[act.iconKey];
            return (
              <ActivityRow
                key={act.id}
                icon={<Icon className="h-4 w-4" aria-hidden />}
                title={act.title}
                sub={act.sub}
                trailing={act.trailing}
                variant={act.variant}
              />
            );
          })}
        </ul>
      </section>

      {/* Pay shortcut — duplicate of bottom-tab Học phí for one-tap access. */}
      <section className="mx-4 mt-6">
        <Link
          href="/parent/billing"
          className="flex items-center justify-between rounded-2xl bg-primary p-4 text-primary-foreground shadow-sm transition-colors hover:bg-primary/90 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <div className="flex items-center gap-3">
            <Wallet className="h-5 w-5" aria-hidden />
            <span className="text-sm font-semibold">Đóng học phí ngay</span>
          </div>
          <Receipt className="h-5 w-5 opacity-90" aria-hidden />
        </Link>
      </section>
    </ParentShell>
  );
}

function currentSchoolYear(): string {
  const now = new Date();
  const year = now.getFullYear();
  // VN academic year runs Sep–Jun; before September, return previous-year span.
  return now.getMonth() >= 8 ? `${year}–${year + 1}` : `${year - 1}–${year}`;
}
