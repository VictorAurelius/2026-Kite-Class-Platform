/**
 * Parent grades overview — subjects table + GPA hero + level badge.
 *
 * Wave 49 Bucket A (GAP-267). Ports `grades-overview.html`. The detailed
 * transcript per child remains at `/parent/transcript/[childId]` (Wave 18b1
 * preserved). This screen surfaces a per-subject summary across the active
 * academic period.
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import {
  ChevronRight,
  FileText,
  Minus,
  TrendingDown,
  TrendingUp,
} from 'lucide-react';
import { useMyChildren } from '@/hooks/use-parent';
import { ParentShell } from '@/components/parent/parent-shell';
import { HeroMetric } from '@/components/parent/hero-metric';
import { MOCK_GRADES } from '@/components/parent/parent-mock-data';

const TREND_ICON = {
  up: TrendingUp,
  down: TrendingDown,
  flat: Minus,
} as const;

const TREND_CLASS = {
  up: 'text-emerald-600',
  down: 'text-red-600',
  flat: 'text-muted-foreground',
} as const;

const LEVEL_CLASS: Record<string, string> = {
  Giỏi: 'bg-emerald-100 text-emerald-700',
  Khá: 'bg-sky-100 text-sky-700',
  'Trung bình': 'bg-amber-100 text-amber-700',
  Yếu: 'bg-red-100 text-red-700',
};

export default function ParentGradesOverviewPage() {
  const { data: children } = useMyChildren();
  const list = children ?? [];

  if (list.length === 0) {
    return (
      <ParentShell title="Học bạ" subtitle="Tổng quan học lực">
        <div
          className="m-4 rounded-2xl border bg-card p-6 text-center"
          data-testid="parent-grades-empty"
        >
          <FileText
            className="mx-auto mb-3 h-10 w-10 text-muted-foreground"
            aria-hidden
          />
          <p className="text-sm font-semibold">Chưa có dữ liệu học bạ</p>
          <p className="mt-1 text-xs text-muted-foreground">
            Sau khi liên kết được con, học bạ sẽ hiển thị tại đây.
          </p>
        </div>
      </ParentShell>
    );
  }

  const avg = (
    MOCK_GRADES.reduce((s, g) => s + g.score, 0) / MOCK_GRADES.length
  ).toFixed(1);

  return (
    <ParentShell title="Học bạ" subtitle="Tổng quan học lực">
      <HeroMetric
        label="Điểm trung bình tất cả môn"
        value={avg}
        delta={{ direction: 'up', label: 'Tăng 0.3 so với học kỳ trước' }}
        data-testid="parent-grades-hero"
      />

      {/* Per-child links to existing Wave 18b1 transcript page. */}
      <section className="mx-4 mt-4">
        <h2 className="mb-2 text-sm font-bold">Học bạ chi tiết</h2>
        <ul className="rounded-2xl border bg-card">
          {list.map((child) => (
            <li
              key={child.studentId}
              className="border-b last:border-b-0"
              data-testid={`parent-grades-child-${child.studentId}`}
            >
              <Link
                href={`/parent/transcript/${child.studentId}`}
                className="flex items-center gap-3 p-4 transition-colors hover:bg-muted/40 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <span
                  className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary"
                  aria-hidden
                >
                  <FileText className="h-5 w-5" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">
                    {child.studentName}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Xem học bạ chi tiết theo học kỳ
                  </p>
                </div>
                <ChevronRight
                  className="h-5 w-5 text-muted-foreground"
                  aria-hidden
                />
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section className="mx-4 mt-6">
        <h2 className="mb-2 text-sm font-bold">Điểm theo môn (giữa kỳ)</h2>
        <ul
          className="rounded-2xl border bg-card"
          data-testid="parent-grades-subjects"
        >
          {MOCK_GRADES.map((g) => {
            const Trend = TREND_ICON[g.trend];
            return (
              <li
                key={g.subject}
                className="border-b last:border-b-0"
                data-testid={`parent-grades-subject-${g.subject}`}
              >
                <Link
                  href={`/parent/grades/${encodeURIComponent(g.subject)}`}
                  className="flex items-center gap-3 p-4 transition-colors hover:bg-muted/40 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold">{g.subject}</p>
                    <p className="mt-0.5 inline-flex items-center gap-1 text-[11px] text-muted-foreground">
                      <Trend
                        className={`h-3 w-3 ${TREND_CLASS[g.trend]}`}
                        aria-hidden
                      />
                      {g.trend === 'up'
                        ? 'Tăng so với kiểm tra trước'
                        : g.trend === 'down'
                          ? 'Giảm so với kiểm tra trước'
                          : 'Ổn định'}
                    </p>
                  </div>
                  <span
                    className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${LEVEL_CLASS[g.level] ?? 'bg-muted'}`}
                  >
                    {g.level}
                  </span>
                  <span className="ml-2 w-10 text-right text-lg font-bold tabular-nums">
                    {g.score.toFixed(1)}
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      </section>
    </ParentShell>
  );
}
