/**
 * Teacher dashboard page.
 * Shows today's classes, quick stats, and attendance actions.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

'use client';

import { useMemo } from 'react';
import Link from 'next/link';
import { Calendar, CheckCircle, Clock, Users, TrendingUp } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { TodayClassesWidget } from '@/components/attendance/today-classes-widget';
import { useTodayClassSessions } from '@/hooks/use-attendance';

export default function TeacherDashboardPage() {
  // Fetch today's class sessions
  const { data: sessions, isLoading, error } = useTodayClassSessions();

  // Calculate quick stats
  const stats = useMemo(() => {
    if (!sessions) {
      return {
        totalClasses: 0,
        pendingCount: 0,
        completedCount: 0,
        totalStudents: 0,
      };
    }

    return {
      totalClasses: sessions.length,
      pendingCount: sessions.filter((s) => !s.attendanceMarked).length,
      completedCount: sessions.filter((s) => s.attendanceMarked).length,
      totalStudents: sessions.reduce((sum, s) => sum + s.totalStudents, 0),
    };
  }, [sessions]);

  // Get current time info — memoized to avoid re-render on every cycle
  const now = useMemo(() => new Date(), []);
  const greeting = useMemo(() => {
    const hour = now.getHours();
    if (hour < 12) return 'Chào buổi sáng';
    if (hour < 18) return 'Chào buổi chiều';
    return 'Chào buổi tối';
  }, [now]);

  const currentDate = now.toLocaleDateString('vi-VN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <div className="container mx-auto space-y-6 py-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold">{greeting}, Giáo viên</h1>
        <p className="text-muted-foreground">{currentDate}</p>
      </div>

      {/* Error State */}
      {error && (
        <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
          <p className="text-sm text-destructive">Không thể tải dữ liệu. Vui lòng thử lại.</p>
        </div>
      )}

      {/* Quick Stats */}
      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-32 animate-pulse rounded-lg bg-muted" />
          ))}
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                Lớp học hôm nay
              </CardTitle>
              <Calendar className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stats.totalClasses}</div>
              <p className="text-xs text-muted-foreground">
                Tổng số lớp cần giảng dạy
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                Chưa điểm danh
              </CardTitle>
              <Clock className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-orange-600">
                {stats.pendingCount}
              </div>
              <p className="text-xs text-muted-foreground">
                Cần điểm danh sớm nhất
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                Đã hoàn thành
              </CardTitle>
              <CheckCircle className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-600">
                {stats.completedCount}
              </div>
              <p className="text-xs text-muted-foreground">
                Đã điểm danh xong
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                Tổng học viên
              </CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stats.totalStudents}</div>
              <p className="text-xs text-muted-foreground">
                Tham gia lớp hôm nay
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Today's Classes Widget */}
      <TodayClassesWidget sessions={sessions || []} isLoading={isLoading} />

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Thao tác nhanh</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Link href="/attendance">
              <Button variant="outline" className="h-24 w-full flex-col gap-2">
                <CheckCircle className="h-6 w-6" />
                <span>Tổng quan điểm danh</span>
              </Button>
            </Link>

            <Link href="/classes">
              <Button variant="outline" className="h-24 w-full flex-col gap-2">
                <Calendar className="h-6 w-6" />
                <span>Quản lý lớp học</span>
              </Button>
            </Link>

            <Link href="/attendance">
              <Button variant="outline" className="h-24 w-full flex-col gap-2">
                <TrendingUp className="h-6 w-6" />
                <span>Xem thống kê</span>
              </Button>
            </Link>
          </div>
        </CardContent>
      </Card>

      {/* Recent Activity / Announcements */}
      <Card>
        <CardHeader>
          <CardTitle>Thông báo</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {stats.pendingCount > 0 && (
              <div className="flex items-start gap-3 rounded-lg border border-orange-200 bg-orange-50 p-4 dark:border-orange-900 dark:bg-orange-950">
                <Clock className="mt-0.5 h-5 w-5 text-orange-600" />
                <div className="flex-1">
                  <h4 className="font-medium text-orange-900 dark:text-orange-100">
                    Có {stats.pendingCount} lớp chưa điểm danh
                  </h4>
                  <p className="text-sm text-orange-700 dark:text-orange-300">
                    Vui lòng điểm danh để hoàn tất buổi học hôm nay
                  </p>
                </div>
              </div>
            )}

            {stats.completedCount === stats.totalClasses && stats.totalClasses > 0 && (
              <div className="flex items-start gap-3 rounded-lg border border-green-200 bg-green-50 p-4 dark:border-green-900 dark:bg-green-950">
                <CheckCircle className="mt-0.5 h-5 w-5 text-green-600" />
                <div className="flex-1">
                  <h4 className="font-medium text-green-900 dark:text-green-100">
                    Hoàn thành tốt!
                  </h4>
                  <p className="text-sm text-green-700 dark:text-green-300">
                    Bạn đã điểm danh xong tất cả các lớp hôm nay
                  </p>
                </div>
              </div>
            )}

            {stats.totalClasses === 0 && (
              <div className="py-8 text-center">
                <div className="mb-2 text-4xl">📅</div>
                <p className="text-sm text-muted-foreground">
                  Không có lớp học nào được lên lịch hôm nay
                </p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
