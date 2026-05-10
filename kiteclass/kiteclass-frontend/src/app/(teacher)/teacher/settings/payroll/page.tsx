/**
 * Teacher payroll — monthly compensation breakdown.
 *
 * Maps to `kiteclass-teacher/settings-payroll.html`.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

'use client';

import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { PAYROLL_SAMPLE, TEACHER_PROFILE } from '@/components/teacher/teacher-mock-data';

function vnd(n: number): string {
  return n.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' });
}

export default function TeacherPayrollPage() {
  const p = PAYROLL_SAMPLE;
  const periodPay = p.periodsThisMonth * p.ratePerPeriod;

  return (
    <div className="space-y-4">
      <nav className="text-sm text-muted-foreground" aria-label="Breadcrumb">
        <Link href="/teacher/settings" className="hover:text-foreground">
          Cài đặt
        </Link>{' '}
        / <span className="text-foreground">Bảng lương</span>
      </nav>

      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Bảng lương — {p.month}</h1>
          <p className="text-muted-foreground">{TEACHER_PROFILE.fullName}</p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Chi tiết</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="divide-y divide-border">
            <Row label="Lương cơ bản" value={vnd(p.baseSalary)} />
            <Row
              label={`Tiền dạy theo tiết (${p.periodsThisMonth} tiết × ${vnd(p.ratePerPeriod)})`}
              value={vnd(periodPay)}
            />
            <Row label="Thưởng" value={vnd(p.bonus)} positive />
            <Row label="Khấu trừ (BHXH, thuế)" value={`-${vnd(p.deductions)}`} />
            <Row label="Tổng cộng" value={vnd(p.total)} bold />
          </dl>
        </CardContent>
      </Card>
    </div>
  );
}

function Row({
  label,
  value,
  positive,
  bold,
}: {
  label: string;
  value: string;
  positive?: boolean;
  bold?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-3">
      <dt className={`text-sm ${bold ? 'font-semibold' : 'text-muted-foreground'}`}>
        {label}
      </dt>
      <dd
        className={`tabular-nums ${bold ? 'text-lg font-bold' : ''} ${positive ? 'text-green-700' : ''}`}
      >
        {value}
      </dd>
    </div>
  );
}
