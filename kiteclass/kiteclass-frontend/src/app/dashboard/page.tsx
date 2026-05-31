/**
 * Enhanced dashboard with real data and recent activities.
 *
 * @author KiteClass Team
 * @since 3.14.0
 */

'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import {
  Users,
  BookOpen,
  GraduationCap,
  Calendar,
  DollarSign,
  TrendingUp,
  Plus,
  ArrowRight,
} from 'lucide-react';
import { DashboardLayout } from '@/components/layout';
import { DashboardWelcome } from '@/components/onboarding/DashboardWelcome';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';
import { formatCurrency, formatDate } from '@/lib/utils';

// Types for dashboard data
interface Student {
  id: number;
  fullName: string;
  email: string;
  createdAt: string;
}

interface Invoice {
  id: number;
  invoiceNumber: string;
  total: number;
  status: string;
}

// Vietnamese labels for invoice statuses
const invoiceStatusLabels: Record<string, string> = {
  DRAFT: 'Nháp',
  PENDING: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  OVERDUE: 'Quá hạn',
  CANCELLED: 'Đã hủy',
};

// API calls for dashboard stats — wrapped in try-catch to prevent
// unhandled rejections that trigger Next.js dev error overlay (GAP-077)
const getDashboardStats = async () => {
  // Per-call resilience: one failing endpoint (e.g. /api/v1/classes 404) must NOT
  // zero out the other counts. Each count resolves independently via its own catch.
  const countOf = (path: string) =>
    apiClient
      .get(path, { params: { page: 0, size: 1 } })
      .then((r) => r.data?.data?.totalElements ?? 0)
      .catch(() => 0);

  const [studentsCount, teachersCount, coursesCount, classesCount] = await Promise.all([
    countOf('/api/v1/students'),
    countOf('/api/v1/teachers'),
    countOf('/api/v1/courses'),
    countOf('/api/v1/classes'),
  ]);

  return { studentsCount, teachersCount, coursesCount, classesCount };
};

const getRecentActivities = async () => {
  try {
    const [students, invoices] = await Promise.all([
      apiClient.get('/api/v1/students', {
        params: { page: 0, size: 5, sort: 'createdAt,desc' },
      }).catch(() => ({ data: { data: { content: [] } } })),
      apiClient.get('/api/v1/invoices', {
        params: { page: 0, size: 5, sort: 'createdAt,desc' },
      }).catch(() => ({ data: { data: { content: [] } } })),
    ]);

    return {
      recentStudents: students.data.data.content || [],
      recentInvoices: invoices.data.data.content || [],
    };
  } catch {
    return {
      recentStudents: [],
      recentInvoices: [],
    };
  }
};

export default function DashboardPage() {
  const { data: stats, isLoading: statsLoading, isError: statsError } = useQuery({
    queryKey: ['dashboardStats'],
    queryFn: getDashboardStats,
  });

  const { data: activities } = useQuery({
    queryKey: ['recentActivities'],
    queryFn: getRecentActivities,
  });

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Onboarding Welcome Banner */}
        <DashboardWelcome />

        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Dashboard</h1>
            <p className="text-muted-foreground">
              Tổng quan hệ thống quản lý KiteClass
            </p>
          </div>
          <div className="flex gap-2">
            <Link href="/students/new">
              <Button>
                <Plus className="mr-2 h-4 w-4" />
                Thêm học viên
              </Button>
            </Link>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Tổng học viên</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              {statsLoading ? (
                <div className="h-7 w-12 animate-pulse rounded bg-muted" />
              ) : (
                <>
                  <div className="text-2xl font-bold">{statsError ? '--' : stats?.studentsCount}</div>
                  <Link href="/students">
                    <p className="text-xs text-muted-foreground hover:underline">
                      Xem tất cả →
                    </p>
                  </Link>
                </>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Giáo viên</CardTitle>
              <GraduationCap className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              {statsLoading ? (
                <div className="h-7 w-12 animate-pulse rounded bg-muted" />
              ) : (
                <>
                  <div className="text-2xl font-bold">{statsError ? '--' : stats?.teachersCount}</div>
                  <Link href="/teachers">
                    <p className="text-xs text-muted-foreground hover:underline">
                      Xem tất cả →
                    </p>
                  </Link>
                </>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Khóa học</CardTitle>
              <BookOpen className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              {statsLoading ? (
                <div className="h-7 w-12 animate-pulse rounded bg-muted" />
              ) : (
                <>
                  <div className="text-2xl font-bold">{statsError ? '--' : stats?.coursesCount}</div>
                  <Link href="/courses">
                    <p className="text-xs text-muted-foreground hover:underline">
                      Xem tất cả →
                    </p>
                  </Link>
                </>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Lớp học</CardTitle>
              <Calendar className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              {statsLoading ? (
                <div className="h-7 w-12 animate-pulse rounded bg-muted" />
              ) : (
                <>
                  <div className="text-2xl font-bold">{statsError ? '--' : stats?.classesCount}</div>
                  <Link href="/classes">
                    <p className="text-xs text-muted-foreground hover:underline">
                      Xem tất cả →
                    </p>
                  </Link>
                </>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Recent Activities */}
        <div className="grid gap-4 md:grid-cols-2">
          {/* Recent Students */}
          <Card>
            <CardHeader>
              <CardTitle>Học viên mới nhất</CardTitle>
              <CardDescription>5 học viên đăng ký gần đây</CardDescription>
            </CardHeader>
            <CardContent>
              {activities?.recentStudents.length === 0 ? (
                <p className="text-sm text-muted-foreground">Chưa có học viên nào</p>
              ) : (
                <div className="space-y-3">
                  {activities?.recentStudents.map((student: Student) => (
                    <Link
                      key={student.id}
                      href={`/students/${student.id}`}
                      className="flex items-center justify-between p-2 rounded-lg hover:bg-accent transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                          <Users className="h-5 w-5 text-primary" />
                        </div>
                        <div>
                          <p className="font-medium">{student.fullName}</p>
                          <p className="text-sm text-muted-foreground">
                            {student.email}
                          </p>
                        </div>
                      </div>
                      <Badge variant="secondary">
                        {formatDate(student.createdAt)}
                      </Badge>
                    </Link>
                  ))}
                </div>
              )}
              <Link href="/students">
                <Button variant="ghost" className="w-full mt-4">
                  Xem tất cả
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </Link>
            </CardContent>
          </Card>

          {/* Recent Invoices */}
          <Card>
            <CardHeader>
              <CardTitle>Hóa đơn gần đây</CardTitle>
              <CardDescription>5 hóa đơn mới nhất</CardDescription>
            </CardHeader>
            <CardContent>
              {activities?.recentInvoices.length === 0 ? (
                <p className="text-sm text-muted-foreground">Chưa có hóa đơn nào</p>
              ) : (
                <div className="space-y-3">
                  {activities?.recentInvoices.map((invoice: Invoice) => (
                    <Link
                      key={invoice.id}
                      href={`/billing/${invoice.id}`}
                      className="flex items-center justify-between p-2 rounded-lg hover:bg-accent transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-full bg-green-100 flex items-center justify-center">
                          <DollarSign className="h-5 w-5 text-green-600" />
                        </div>
                        <div>
                          <p className="font-medium">{invoice.invoiceNumber}</p>
                          <p className="text-sm text-muted-foreground">
                            {formatCurrency(invoice.total)}
                          </p>
                        </div>
                      </div>
                      <Badge
                        variant={
                          invoice.status === 'PAID' ? 'success' : 'default'
                        }
                      >
                        {invoiceStatusLabels[invoice.status] || invoice.status}
                      </Badge>
                    </Link>
                  ))}
                </div>
              )}
              <Link href="/billing">
                <Button variant="ghost" className="w-full mt-4">
                  Xem tất cả
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </Link>
            </CardContent>
          </Card>
        </div>

        {/* Quick Actions */}
        <Card>
          <CardHeader>
            <CardTitle>Thao tác nhanh</CardTitle>
            <CardDescription>Truy cập các chức năng thường dùng</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
              <Link href="/students/new">
                <Button variant="outline" className="w-full h-20 flex-col gap-2">
                  <Users className="h-6 w-6" />
                  <span>Thêm học viên</span>
                </Button>
              </Link>

              <Link href="/teachers/new">
                <Button variant="outline" className="w-full h-20 flex-col gap-2">
                  <GraduationCap className="h-6 w-6" />
                  <span>Thêm giáo viên</span>
                </Button>
              </Link>

              <Link href="/courses/new">
                <Button variant="outline" className="w-full h-20 flex-col gap-2">
                  <BookOpen className="h-6 w-6" />
                  <span>Tạo khóa học</span>
                </Button>
              </Link>

              <Link href="/attendance">
                <Button variant="outline" className="w-full h-20 flex-col gap-2">
                  <TrendingUp className="h-6 w-6" />
                  <span>Điểm danh</span>
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
