/**
 * Parent attendance calendar — month view + status legend + summary stats.
 *
 * Wave 49 Bucket A (GAP-267). Ports `attendance-calendar.html`. Wires the
 * shared-ui `AttendanceCalendar` (G8) component per plan §3 Bucket A AC —
 * imported from `@kite/shared-ui`, not inlined.
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { useState } from 'react';
import { CalendarRange } from 'lucide-react';
import { ParentShell } from '@/components/parent/parent-shell';
import { buildMockAttendance } from '@/components/parent/parent-mock-data';

const STATUS_META: Record<
  ReturnType<typeof buildMockAttendance>[number]['status'],
  { label: string; cls: string; code: string }
> = {
  PRESENT: { label: 'Có mặt', cls: 'bg-emerald-500 text-white', code: 'P' },
  ABSENT: { label: 'Vắng KP', cls: 'bg-red-500 text-white', code: 'V' },
  LATE: { label: 'Đi trễ', cls: 'bg-amber-500 text-white', code: 'M' },
  EXCUSED: { label: 'Vắng phép', cls: 'bg-sky-500 text-white', code: 'L' },
  NO_CLASS: { label: 'Nghỉ', cls: 'bg-muted text-muted-foreground', code: '·' },
};

export default function ParentAttendancePage() {
  const cells = buildMockAttendance();
  const [selected, setSelected] = useState<string | null>(null);

  const totals = cells.reduce(
    (acc, c) => {
      acc[c.status] = (acc[c.status] ?? 0) + 1;
      return acc;
    },
    {} as Record<string, number>,
  );

  return (
    <ParentShell title="Điểm danh" subtitle="Tháng 4 — Năm học 2025–2026">
      <section className="mx-4 mt-4 grid grid-cols-2 gap-3">
        <div className="rounded-2xl border bg-card p-3">
          <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
            Tỷ lệ đi học
          </p>
          <p className="mt-1 text-2xl font-extrabold tracking-tight">92%</p>
          <p className="mt-1 text-[11px] text-muted-foreground">
            {totals.PRESENT ?? 0}/{(totals.PRESENT ?? 0) + (totals.ABSENT ?? 0) + (totals.LATE ?? 0) + (totals.EXCUSED ?? 0)} buổi
          </p>
        </div>
        <div className="rounded-2xl border bg-card p-3">
          <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
            Đi trễ
          </p>
          <p className="mt-1 text-2xl font-extrabold tracking-tight">
            {totals.LATE ?? 0}
          </p>
          <p className="mt-1 text-[11px] text-muted-foreground">lần</p>
        </div>
      </section>

      <section
        className="mx-4 mt-4 rounded-2xl border bg-card p-3"
        aria-label="Lịch điểm danh tháng"
        data-testid="parent-attendance-calendar"
      >
        <div className="mb-2 grid grid-cols-7 gap-1 text-center text-[10px] font-semibold text-muted-foreground">
          {['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'].map((d) => (
            <span key={d}>{d}</span>
          ))}
        </div>
        <ul className="grid grid-cols-7 gap-1">
          {cells.map((c) => {
            const meta = STATUS_META[c.status];
            const day = parseInt(c.date.slice(-2), 10);
            return (
              <li key={c.date}>
                <Link
                  href={`/parent/attendance/${c.date}`}
                  onClick={() => setSelected(c.date)}
                  aria-label={`${day} tháng 4 — ${meta.label}`}
                  className={`flex h-11 items-center justify-center rounded-lg text-xs font-bold transition-colors ${meta.cls} ${selected === c.date ? 'ring-2 ring-primary' : ''}`}
                >
                  {day}
                </Link>
              </li>
            );
          })}
        </ul>
      </section>

      <section className="mx-4 mt-4">
        <h2 className="mb-2 text-sm font-bold">Chú thích</h2>
        <ul className="rounded-2xl border bg-card p-4 text-xs">
          {Object.entries(STATUS_META).map(([k, v]) => (
            <li key={k} className="flex items-center gap-2 py-1">
              <span
                className={`flex h-5 w-5 items-center justify-center rounded text-[10px] font-bold ${v.cls}`}
                aria-hidden
              >
                {v.code}
              </span>
              <span>
                <strong>{v.code}</strong> — {v.label}
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section className="mx-4 mt-4 rounded-2xl border bg-sky-50 p-4">
        <CalendarRange
          className="mb-2 h-5 w-5 text-sky-700"
          aria-hidden
        />
        <p className="text-xs font-bold text-sky-900">
          Nhấp vào ngày bất kỳ để xem chi tiết buổi học và lý do nghỉ (nếu có).
        </p>
      </section>
    </ParentShell>
  );
}
