/**
 * Attendance overview — class list for the day's roster.
 *
 * Maps to `kiteclass-teacher/attendance-default.html` + empty / error /
 * loading variants. Consumes the shared `@kite/shared-ui` G8 attendance
 * calendar where appropriate.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import Link from 'next/link';
import { ChevronRight } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { TEACHER_TODAY_QUEUE } from '@/components/teacher/teacher-mock-data';

export default function TeacherAttendanceOverviewPage() {
  const queue = TEACHER_TODAY_QUEUE;

  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-bold">Điểm danh</h1>
          <p className="text-muted-foreground">
            Chọn lớp để mở danh sách điểm danh
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Lớp dạy hôm nay</CardTitle>
        </CardHeader>
        <CardContent className="divide-y divide-border">
          {queue.map((cls) => {
            const rate =
              cls.totalStudents === 0
                ? 0
                : Math.round((cls.presentCount / cls.totalStudents) * 100);
            return (
              <Link
                key={cls.id}
                href={`/teacher/attendance/${cls.id}`}
                className="flex items-center justify-between py-3 hover:bg-muted/40"
              >
                <div>
                  <div className="font-medium">
                    Tiết {cls.periodNo} · {cls.className} · {cls.subject}
                  </div>
                  <div className="text-sm text-muted-foreground">
                    {cls.startTime} – {cls.endTime} · Phòng {cls.room}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  {cls.attendanceMarked ? (
                    <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">
                      Đã điểm danh {rate}%
                    </span>
                  ) : (
                    <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-700">
                      Chưa điểm danh
                    </span>
                  )}
                  <ChevronRight className="h-4 w-4 text-muted-foreground" />
                </div>
              </Link>
            );
          })}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Lịch điểm danh tháng này</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Để xem lịch tháng dạng calendar (G8 component), bấm vào một lớp
            cụ thể bên trên.
          </p>
          <Button asChild className="mt-3" variant="outline">
            <Link href="/teacher/reports">Xem thống kê chuyên cần</Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
