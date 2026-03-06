/**
 * Attendance overview page - shows attendance summary across all classes.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { Calendar, Users, TrendingUp, Clock } from 'lucide-react';
import { DashboardLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useAllActiveClasses } from '@/hooks/use-classes';

export default function AttendanceOverviewPage() {
  const { data: activeClasses = [], isLoading } = useAllActiveClasses();

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Điểm danh</h1>
            <p className="text-muted-foreground">
              Quản lý điểm danh học viên
            </p>
          </div>
          <Link href="/attendance/reports">
            <Button variant="outline">
              <TrendingUp className="mr-2 h-4 w-4" />
              Báo cáo
            </Button>
          </Link>
        </div>

        {/* Stats Cards */}
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">
                Lớp đang học
              </CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{activeClasses.length}</div>
              <p className="text-xs text-muted-foreground">
                Cần điểm danh hàng ngày
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">
                Tổng học viên
              </CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {activeClasses.reduce((sum: number, c) => sum + c.currentEnrolled, 0)}
              </div>
              <p className="text-xs text-muted-foreground">
                Đang tham gia học
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">
                Buổi học hôm nay
              </CardTitle>
              <Calendar className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">0</div>
              <p className="text-xs text-muted-foreground">
                Đã/Cần điểm danh
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">
                Tỷ lệ có mặt
              </CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">--</div>
              <p className="text-xs text-muted-foreground">
                Trung bình tháng này
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Classes List */}
        <Card>
          <CardHeader>
            <CardTitle>Lớp học đang hoạt động</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Tên lớp</TableHead>
                  <TableHead>Mã lớp</TableHead>
                  <TableHead>Số học viên</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Ngày bắt đầu</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading ? (
                  <TableRow>
                    <TableCell
                      colSpan={6}
                      className="py-12 text-center text-muted-foreground"
                    >
                      <p>Đang tải...</p>
                    </TableCell>
                  </TableRow>
                ) : activeClasses.length > 0 ? (
                  activeClasses.map((classItem) => (
                    <TableRow key={classItem.id}>
                      <TableCell className="font-medium">
                        {classItem.name}
                      </TableCell>
                      <TableCell>
                        <code className="rounded bg-muted px-2 py-1 text-sm">
                          {classItem.classCode || 'N/A'}
                        </code>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Users className="h-4 w-4 text-muted-foreground" />
                          {classItem.currentEnrolled}/{classItem.maxStudents}
                        </div>
                      </TableCell>
                      <TableCell>
                        <span
                          className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${
                            classItem.status === 'IN_PROGRESS'
                              ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300'
                              : 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300'
                          }`}
                        >
                          {classItem.status === 'IN_PROGRESS'
                            ? 'Đang học'
                            : 'Sắp học'}
                        </span>
                      </TableCell>
                      <TableCell>
                        {classItem.startDate || 'N/A'}
                      </TableCell>
                      <TableCell className="text-right">
                        <Link href={`/classes/${classItem.id}/attendance`}>
                          <Button size="sm">
                            <Clock className="mr-2 h-4 w-4" />
                            Điểm danh
                          </Button>
                        </Link>
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell
                      colSpan={6}
                      className="py-12 text-center text-muted-foreground"
                    >
                      <Calendar className="mx-auto h-12 w-12 opacity-20" />
                      <p className="mt-4">Không có lớp học nào đang hoạt động</p>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
