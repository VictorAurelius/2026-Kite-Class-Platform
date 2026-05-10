/**
 * Student PWA — Trang hôm nay (today). Wave 49 Bucket C (GAP-269).
 *
 * Mirrors `kiteclass-student/screens/today.html` (kit score 117/128).
 * Sections: greeting + attendance streak hero + next-class card + today's
 * schedule + pending tasks. Realistic VN data; backend wiring deferred to
 * follow-up gap (per Wave 49 plan §3 Bucket C — focus is shell + UI).
 */
'use client';

import Link from 'next/link';
import { Bell, BookOpen, ChevronRight, Clock, User } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { StudentMobileShell } from '@/components/student/mobile-shell';

interface ClassRow {
  id: string;
  swatch: string;
  name: string;
  time: string;
  room: string;
  teacher: string;
  state: 'upcoming' | 'later';
}

interface TaskRow {
  id: string;
  title: string;
  due: string;
  meta: string;
}

const TODAY_CLASSES: readonly ClassRow[] = [
  { id: 'c1', swatch: 'T', name: 'Toán nâng cao', time: '10:00', room: 'Phòng 305', teacher: 'Cô Hương', state: 'upcoming' },
  { id: 'c2', swatch: 'V', name: 'Ngữ Văn 10', time: '13:30', room: 'Phòng 207', teacher: 'Thầy Tuấn', state: 'later' },
  { id: 'c3', swatch: 'E', name: 'Tiếng Anh 10', time: '15:30', room: 'Phòng 411', teacher: 'Cô Linh', state: 'later' },
];

const PENDING_TASKS: readonly TaskRow[] = [
  { id: 't1', title: 'Bài luận "Quê hương" — Văn 10', due: 'Hạn 18/04/2026 · Còn 3 ngày', meta: '~600 từ' },
  { id: 't2', title: 'Bài tập chương 4 — Toán nâng cao', due: 'Hạn hôm nay · Còn 6 giờ', meta: '8 câu' },
  { id: 't3', title: 'Reading worksheet — English 10', due: 'Hạn 22/04/2026 · Còn 1 tuần', meta: 'PDF' },
];

export default function StudentTodayPage() {
  return (
    <StudentMobileShell
      title="Chào An 👋"
      subtitle="Thứ Hai · 15/04/2026"
      headerRight={
        <Button variant="ghost" size="icon" asChild aria-label="Thông báo, 5 chưa đọc">
          <Link href="/student/notifications">
            <Bell className="h-5 w-5" aria-hidden />
          </Link>
        </Button>
      }
    >
      <section
        aria-labelledby="hero-streak"
        className="rounded-2xl bg-gradient-to-br from-teal-500 to-emerald-600 p-5 text-white shadow-lg"
      >
        <div id="hero-streak" className="text-sm opacity-90">
          Chuỗi đi học liên tiếp 🎯
        </div>
        <div className="mt-1 text-4xl font-bold">
          12 <span className="text-base font-semibold opacity-90">ngày</span>
        </div>
        <div className="mt-2 text-xs opacity-90">
          Thêm 3 ngày nữa để nhận huy hiệu vàng
        </div>
      </section>

      <Card className="mt-4 border-primary/30 bg-primary/5">
        <CardContent className="space-y-3 p-4">
          <Badge variant="secondary" className="bg-primary/10 text-primary">
            <Clock className="mr-1 h-3 w-3" aria-hidden /> Còn 25 phút nữa
          </Badge>
          <h2 className="text-lg font-semibold leading-tight">
            Toán nâng cao — Lớp 10A2
          </h2>
          <ul className="space-y-1.5 text-sm text-muted-foreground">
            <li className="flex items-center gap-2">
              <Clock className="h-4 w-4" aria-hidden /> 10:00 – 11:30 · Phòng 305
            </li>
            <li className="flex items-center gap-2">
              <User className="h-4 w-4" aria-hidden /> Cô Trần Thị Hương
            </li>
            <li className="flex items-center gap-2">
              <BookOpen className="h-4 w-4" aria-hidden /> Chương 4 — Hàm số bậc hai
            </li>
          </ul>
          <Button asChild className="w-full">
            <Link href="/student/my-classes/c1">
              Mở chi tiết lớp <ChevronRight className="ml-1 h-4 w-4" aria-hidden />
            </Link>
          </Button>
        </CardContent>
      </Card>

      <SectionHeading title="Lịch học hôm nay" linkHref="/student/my-classes" linkLabel="Xem tất cả" />
      <ul className="space-y-2">
        {TODAY_CLASSES.map((c) => (
          <li key={c.id}>
            <Link
              href={`/student/my-classes/${c.id}`}
              className="flex items-center gap-3 rounded-lg border border-border bg-card p-3 transition-colors hover:bg-accent/50"
            >
              <span
                aria-hidden
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/15 text-sm font-bold text-primary"
              >
                {c.swatch}
              </span>
              <div className="min-w-0 flex-1">
                <div className="truncate text-sm font-medium">{c.name}</div>
                <div className="truncate text-xs text-muted-foreground">
                  {c.time} · {c.room} · {c.teacher}
                </div>
              </div>
              <Badge variant={c.state === 'upcoming' ? 'default' : 'secondary'}>
                {c.state === 'upcoming' ? 'Sắp tới' : 'Chiều'}
              </Badge>
            </Link>
          </li>
        ))}
      </ul>

      <SectionHeading
        title="Bài tập chờ nộp"
        linkHref="/student/assignments"
        linkLabel={`Xem tất cả (${PENDING_TASKS.length})`}
      />
      <ul className="space-y-2">
        {PENDING_TASKS.map((t) => (
          <li key={t.id}>
            <Link
              href={`/student/assignments/${t.id}`}
              className="flex items-center gap-3 rounded-lg border border-border bg-card p-3 transition-colors hover:bg-accent/50"
            >
              <div className="min-w-0 flex-1">
                <div className="text-sm font-medium leading-tight">{t.title}</div>
                <div className="mt-0.5 text-xs text-muted-foreground">{t.due}</div>
              </div>
              <Badge variant="outline">{t.meta}</Badge>
            </Link>
          </li>
        ))}
      </ul>
    </StudentMobileShell>
  );
}

function SectionHeading({
  title,
  linkHref,
  linkLabel,
}: {
  title: string;
  linkHref: string;
  linkLabel: string;
}) {
  return (
    <div className="mt-6 mb-2 flex items-center justify-between">
      <h2 className="text-sm font-semibold">{title}</h2>
      <Link href={linkHref} className="text-xs text-primary hover:underline">
        {linkLabel}
      </Link>
    </div>
  );
}
