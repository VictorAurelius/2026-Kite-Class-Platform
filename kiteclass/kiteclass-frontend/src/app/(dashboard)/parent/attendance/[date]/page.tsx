/**
 * Parent attendance day detail — single-day breakdown of periods + status.
 *
 * Wave 49 Bucket A (GAP-267). Ports `attendance-day.html`.
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { ArrowLeft, Check, Clock, X } from 'lucide-react';
import { ParentShell } from '@/components/parent/parent-shell';

type PeriodStatus = 'PRESENT' | 'LATE' | 'ABSENT';

interface PeriodEntry {
  no: number;
  subject: string;
  teacher: string;
  status: PeriodStatus;
  note?: string;
}

const MOCK_DAY_PERIODS: PeriodEntry[] = [
  { no: 1, subject: 'Toán', teacher: 'Cô Trần Thị Hương', status: 'PRESENT' },
  { no: 2, subject: 'Ngữ văn', teacher: 'Thầy Nguyễn Văn Hải', status: 'LATE', note: 'Đến muộn 5 phút' },
  { no: 3, subject: 'Vật lý', teacher: 'Cô Lê Thị Nga', status: 'PRESENT' },
  { no: 4, subject: 'Tiếng Anh', teacher: 'Thầy Phạm Quốc Hùng', status: 'PRESENT' },
];

const STATUS_VIEW: Record<PeriodStatus, { label: string; cls: string; Icon: React.ComponentType<{ className?: string }> }> = {
  PRESENT: { label: 'Có mặt', cls: 'bg-emerald-100 text-emerald-700', Icon: Check },
  LATE: { label: 'Đi trễ', cls: 'bg-amber-100 text-amber-700', Icon: Clock },
  ABSENT: { label: 'Vắng', cls: 'bg-red-100 text-red-700', Icon: X },
};

function formatVNDate(iso: string): string {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

export default function ParentAttendanceDayPage() {
  const params = useParams<{ date: string }>();
  const date = params?.date ?? '';

  return (
    <ParentShell
      title={`Buổi học ngày ${formatVNDate(date)}`}
      subtitle="Chi tiết điểm danh"
    >
      <div className="px-4 pt-3">
        <Link
          href="/parent/attendance"
          className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
        >
          <ArrowLeft className="h-3.5 w-3.5" aria-hidden />
          Quay lại lịch tháng
        </Link>
      </div>

      <section className="mx-4 mt-3">
        <ul
          className="rounded-2xl border bg-card"
          data-testid="parent-attendance-day-periods"
        >
          {MOCK_DAY_PERIODS.map((p) => {
            const view = STATUS_VIEW[p.status];
            return (
              <li
                key={p.no}
                className="border-b last:border-b-0"
                data-testid={`parent-attendance-period-${p.no}`}
              >
                <div className="flex items-start gap-3 p-4">
                  <span
                    className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary"
                    aria-hidden
                  >
                    Tiết {p.no}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold">{p.subject}</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                      {p.teacher}
                    </p>
                    {p.note && (
                      <p className="mt-1 text-xs text-amber-700">{p.note}</p>
                    )}
                  </div>
                  <span
                    className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-[11px] font-semibold ${view.cls}`}
                  >
                    <view.Icon className="h-3 w-3" />
                    {view.label}
                  </span>
                </div>
              </li>
            );
          })}
        </ul>
      </section>
    </ParentShell>
  );
}
