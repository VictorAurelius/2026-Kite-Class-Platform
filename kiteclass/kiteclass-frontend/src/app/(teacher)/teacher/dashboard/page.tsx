/**
 * Teacher Dashboard ("Today") — entry point for the (teacher) route group.
 *
 * Mirrors `kiteclass-teacher` Round-2 today-screen layout: greeting + 4-stat
 * grid + class queue for the day with current-period highlighted.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import { useMemo } from 'react';
import Link from 'next/link';
import { CalendarDays, ClipboardCheck, Clock, Users } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  TEACHER_PROFILE,
  TEACHER_TODAY_QUEUE,
} from '@/components/teacher/teacher-mock-data';

export default function TeacherDashboardPage() {
  const queue = TEACHER_TODAY_QUEUE;

  const stats = useMemo(() => {
    const totalClasses = queue.length;
    const completed = queue.filter((q) => q.attendanceMarked).length;
    const pending = totalClasses - completed;
    const totalStudents = queue.reduce((sum, q) => sum + q.totalStudents, 0);
    return { totalClasses, completed, pending, totalStudents };
  }, [queue]);

  const now = useMemo(() => new Date(), []);
  const greeting = useMemo(() => {
    const hour = now.getHours();
    if (hour < 12) return 'Chào buổi sáng';
    if (hour < 18) return 'Chào buổi chiều';
    return 'Chào buổi tối';
  }, [now]);

  const dateLabel = now.toLocaleDateString('vi-VN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">
          {greeting}, {TEACHER_PROFILE.shortName}
        </h1>
        <p className="text-muted-foreground">
          {dateLabel} · {TEACHER_PROFILE.semester} {TEACHER_PROFILE.schoolYear}
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Lớp hôm nay</CardTitle>
            <CalendarDays className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold tabular-nums">
              {stats.totalClasses}
            </div>
            <p className="text-xs text-muted-foreground">Tổng số lớp giảng dạy</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Chưa điểm danh</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold tabular-nums text-orange-600">
              {stats.pending}
            </div>
            <p className="text-xs text-muted-foreground">Cần điểm danh</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Đã hoàn thành</CardTitle>
            <ClipboardCheck className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold tabular-nums text-green-600">
              {stats.completed}
            </div>
            <p className="text-xs text-muted-foreground">Đã điểm danh xong</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Tổng học viên</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold tabular-nums">
              {stats.totalStudents}
            </div>
            <p className="text-xs text-muted-foreground">Tham gia lớp hôm nay</p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Danh sách lớp hôm nay</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {queue.map((cls) => {
            const rate =
              cls.totalStudents === 0
                ? 0
                : Math.round((cls.presentCount / cls.totalStudents) * 100);
            return (
              <div
                key={cls.id}
                className="flex flex-col gap-3 rounded-xl border border-border bg-card p-4 md:flex-row md:items-center md:justify-between"
              >
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-medium">
                      Tiết {cls.periodNo} · {cls.className}
                    </span>
                    <span className="text-sm text-muted-foreground">
                      {cls.subject}
                    </span>
                    {cls.attendanceMarked ? (
                      <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">
                        Đã điểm danh ({rate}%)
                      </span>
                    ) : (
                      <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-700">
                        Chưa điểm danh
                      </span>
                    )}
                  </div>
                  <div className="mt-1 text-sm text-muted-foreground">
                    {cls.startTime} – {cls.endTime} · Phòng {cls.room} ·{' '}
                    {cls.totalStudents} học sinh
                  </div>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button asChild variant="outline" size="sm">
                    <Link href={`/teacher/attendance/${cls.id}`}>
                      Điểm danh
                    </Link>
                  </Button>
                  <Button asChild variant="outline" size="sm">
                    <Link href={`/teacher/grades/${cls.id}`}>Vào điểm</Link>
                  </Button>
                </div>
              </div>
            );
          })}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Thao tác nhanh</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Button asChild variant="outline" className="h-20 flex-col gap-1.5">
              <Link href="/teacher/attendance">
                <ClipboardCheck className="h-5 w-5" />
                Điểm danh
              </Link>
            </Button>
            <Button asChild variant="outline" className="h-20 flex-col gap-1.5">
              <Link href="/teacher/grades">
                <span className="text-xl font-bold">10</span>
                Vào điểm
              </Link>
            </Button>
            <Button asChild variant="outline" className="h-20 flex-col gap-1.5">
              <Link href="/teacher/schedule">
                <CalendarDays className="h-5 w-5" />
                Lịch dạy
              </Link>
            </Button>
            <Button asChild variant="outline" className="h-20 flex-col gap-1.5">
              <Link href="/teacher/reports">
                <Users className="h-5 w-5" />
                Báo cáo
              </Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
