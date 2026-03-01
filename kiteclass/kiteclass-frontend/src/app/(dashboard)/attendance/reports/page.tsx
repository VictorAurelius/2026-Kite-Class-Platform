/**
 * Attendance reports page - shows attendance statistics and trends.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

'use client';

export const dynamic = 'force-dynamic';

import { ArrowLeft, Download, Calendar as CalendarIcon } from 'lucide-react';
import Link from 'next/link';
import { DashboardLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export default function AttendanceReportsPage() {
  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/attendance">
              <Button variant="outline" size="icon">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <div>
              <h1 className="text-3xl font-bold">Báo cáo điểm danh</h1>
              <p className="text-muted-foreground">
                Xem thống kê và xu hướng điểm danh
              </p>
            </div>
          </div>
          <Button>
            <Download className="mr-2 h-4 w-4" />
            Xuất báo cáo
          </Button>
        </div>

        {/* Placeholder */}
        <Card>
          <CardHeader>
            <CardTitle>Tính năng đang phát triển</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="py-12 text-center text-muted-foreground">
              <CalendarIcon className="mx-auto h-16 w-16 opacity-20" />
              <p className="mt-4 text-lg">
                Tính năng báo cáo điểm danh đang được phát triển
              </p>
              <p className="mt-2">
                Sẽ có các biểu đồ thống kê, xu hướng, và báo cáo chi tiết
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
