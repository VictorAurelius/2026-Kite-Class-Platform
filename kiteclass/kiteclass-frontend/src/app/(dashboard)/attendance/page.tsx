/**
 * Attendance overview page - shows attendance summary across all classes.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { Calendar, Users, TrendingUp } from 'lucide-react';
import { DashboardLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { DynamicActiveClassesTable } from '@/components/attendance';
import { useAllActiveClasses } from '@/hooks/use-classes';
import type { Class } from '@/types/class';

export default function AttendanceOverviewPage() {
  const { data: activeClasses = [], isLoading, error } = useAllActiveClasses();

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-3xl font-bold">Điểm danh</h1>
            <p className="text-muted-foreground">Quản lý điểm danh học viên</p>
          </div>
          <Link href="/attendance/reports">
            <Button variant="outline" className="w-full sm:w-auto">
              <TrendingUp className="mr-2 h-4 w-4" />
              Báo cáo
            </Button>
          </Link>
        </div>

        {/* Error State */}
        {error && (
          <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
            <p className="text-sm text-destructive">
              Không thể tải dữ liệu. Vui lòng thử lại.
            </p>
          </div>
        )}

        {/* Stats Cards */}
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Lớp đang học</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{activeClasses.length}</div>
              <p className="text-xs text-muted-foreground">Cần điểm danh hàng ngày</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Tổng học viên</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {activeClasses.reduce(
                  (sum: number, c: Class) => sum + c.currentEnrolled,
                  0,
                )}
              </div>
              <p className="text-xs text-muted-foreground">Đang tham gia học</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Buổi học hôm nay</CardTitle>
              <Calendar className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">0</div>
              <p className="text-xs text-muted-foreground">Đã/Cần điểm danh</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Tỷ lệ có mặt</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">--</div>
              <p className="text-xs text-muted-foreground">Trung bình tháng này</p>
            </CardContent>
          </Card>
        </div>

        {/* Classes List — table lazy-loaded below summary cards */}
        <Card>
          <CardHeader>
            <CardTitle>Lớp học đang hoạt động</CardTitle>
          </CardHeader>
          <CardContent>
            <DynamicActiveClassesTable
              classes={activeClasses}
              isLoading={isLoading}
            />
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
