/**
 * Student PWA — Lịch sử điểm danh (attendance log).
 * Wave 49 Bucket C (GAP-269). Mirrors `kiteclass-student/screens/attendance.html`.
 *
 * Uses `AttendanceCalendar` (G8) from `@kite/shared-ui` — Track 2 Phase 3
 * port (Wave 28). Read-only month view; cycle-status editor stays in the
 * teacher route group.
 */
'use client';

import { AttendanceCalendar, calculateStreak } from '@kite/shared-ui';
import { Card, CardContent } from '@/components/ui/card';
import { StudentMobileShell } from '@/components/student/mobile-shell';

// April 2026 — sample VN school month (T2-T7 = M-Sat)
const DAYS = [
  { dayOfMonth: 1, status: 'PRESENT' as const },
  { dayOfMonth: 2, status: 'PRESENT' as const },
  { dayOfMonth: 3, status: 'PRESENT' as const },
  { dayOfMonth: 4, status: 'NO_CLASS' as const },
  { dayOfMonth: 5, status: 'NO_CLASS' as const },
  { dayOfMonth: 6, status: 'PRESENT' as const },
  { dayOfMonth: 7, status: 'EXCUSED' as const },
  { dayOfMonth: 8, status: 'PRESENT' as const },
  { dayOfMonth: 9, status: 'LATE' as const },
  { dayOfMonth: 10, status: 'PRESENT' as const },
  { dayOfMonth: 11, status: 'NO_CLASS' as const },
  { dayOfMonth: 12, status: 'NO_CLASS' as const },
  { dayOfMonth: 13, status: 'PRESENT' as const },
  { dayOfMonth: 14, status: 'PRESENT' as const },
  { dayOfMonth: 15, status: 'PRESENT' as const },
];

const MONTH = { year: 2026, month: 4, days: DAYS };

export default function StudentAttendancePage() {
  const present = DAYS.filter((d) => d.status === 'PRESENT').length;
  const total = DAYS.filter((d) => d.status !== 'NO_CLASS').length;
  const pct = total > 0 ? ((present / total) * 100).toFixed(0) : '0';
  const streak = calculateStreak(DAYS.map((d) => d.status));

  return (
    <StudentMobileShell title="Điểm danh" subtitle="Tháng 04 / 2026">
      <Card className="bg-primary/5">
        <CardContent className="grid grid-cols-3 gap-3 p-4 text-center">
          <Stat label="Có mặt" value={`${present}`} />
          <Stat label="Tỉ lệ" value={`${pct}%`} highlight />
          <Stat label="Chuỗi" value={`${streak.count}`} />
        </CardContent>
      </Card>

      <Card className="mt-4">
        <CardContent className="p-2 sm:p-4">
          <AttendanceCalendar month={MONTH} streak={streak} />
        </CardContent>
      </Card>
    </StudentMobileShell>
  );
}

function Stat({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div>
      <div
        className={`text-2xl font-bold ${highlight ? 'text-primary' : 'text-foreground'}`}
      >
        {value}
      </div>
      <div className="text-xs text-muted-foreground">{label}</div>
    </div>
  );
}
