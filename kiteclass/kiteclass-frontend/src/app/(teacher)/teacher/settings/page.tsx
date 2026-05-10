/**
 * Teacher settings — profile + preferences + theme + payroll quick link.
 *
 * Maps to `kiteclass-teacher/settings-default.html` + dark variant.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { TEACHER_PROFILE } from '@/components/teacher/teacher-mock-data';

export default function TeacherSettingsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Cài đặt</h1>
        <p className="text-muted-foreground">Hồ sơ giáo viên + tùy chỉnh</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Hồ sơ</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <Field label="Họ và tên" value={TEACHER_PROFILE.fullName} />
          <Field
            label="GVCN"
            value={`${TEACHER_PROFILE.homeroomClass} (${TEACHER_PROFILE.subject})`}
          />
          <Field label="Email" value={TEACHER_PROFILE.email} />
          <Field label="Điện thoại" value={TEACHER_PROFILE.phone} />
          <Field
            label="Năm học"
            value={`${TEACHER_PROFILE.semester} ${TEACHER_PROFILE.schoolYear}`}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Tùy chỉnh</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <Toggle label="Thông báo email khi có học sinh vắng" />
          <Toggle label="Tự động đồng bộ lịch dạy với calendar" defaultOn />
          <Toggle label="Bật chế độ tối (dark mode)" />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Lương + thù lao</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-muted-foreground">
            Xem chi tiết bảng lương + tiết dạy theo tháng.
          </p>
          <Button asChild variant="outline">
            <Link href="/teacher/settings/payroll">Xem bảng lương</Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="font-medium">{value}</div>
    </div>
  );
}

function Toggle({ label, defaultOn = false }: { label: string; defaultOn?: boolean }) {
  return (
    <label className="flex cursor-pointer items-center justify-between rounded-md border border-border bg-card px-3 py-2 text-sm">
      <span>{label}</span>
      <input type="checkbox" defaultChecked={defaultOn} className="h-4 w-4" />
    </label>
  );
}
