/**
 * Admin Payroll page — Phase 1 read-only list view.
 *
 * GAP-057 Phase 1 (Wave 18a Bucket C). Shows paged payroll periods + configs.
 * Phase 2 (GAP-057b) will add:
 *   - Run payroll button (calls POST /api/v1/admin/payroll/runs)
 *   - Approve button per DRAFT period
 *   - Pay button per APPROVED period
 *   - Payslip PDF download (depends on GAP-047)
 *   - Bank export action
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */

'use client';

import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { usePayrollConfigs, usePayrollPeriods } from '@/hooks/use-payroll';
import { DashboardLayout } from '@/components/layout';
import type { PayrollStatus, PayrollType } from '@/types/payroll';

const VND_FORMATTER = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
});

const formatVnd = (n: number | null | undefined): string => {
  if (n === null || n === undefined) return '—';
  return VND_FORMATTER.format(n);
};

const formatHours = (h: number | null | undefined): string => {
  if (h === null || h === undefined) return '—';
  return `${h.toFixed(2)} h`;
};

const STATUS_BADGE: Record<PayrollStatus, { label: string; className: string }> = {
  DRAFT: { label: 'Bản nháp', className: 'bg-yellow-100 text-yellow-800' },
  APPROVED: { label: 'Đã duyệt', className: 'bg-blue-100 text-blue-800' },
  PAID: { label: 'Đã thanh toán', className: 'bg-green-100 text-green-800' },
};

const TYPE_LABEL: Record<PayrollType, string> = {
  HOURLY: 'Theo giờ',
  SALARY: 'Lương cố định',
  COMMISSION: 'Hoa hồng',
  HYBRID: 'Lương + thưởng',
};

export default function AdminPayrollPage() {
  const [teacherIdFilter, setTeacherIdFilter] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');

  const teacherId = teacherIdFilter ? Number(teacherIdFilter) : undefined;

  const {
    data: configs,
    isLoading: configsLoading,
    error: configsError,
  } = usePayrollConfigs({ size: 50 });

  const {
    data: periods,
    isLoading: periodsLoading,
    error: periodsError,
  } = usePayrollPeriods({
    teacherId,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
    size: 50,
    sort: 'startDate,desc',
  });

  return (
    <DashboardLayout>
    <div className="container mx-auto space-y-6 py-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold">Bảng lương giáo viên</h1>
        <p className="text-muted-foreground mt-1">
          Tổng quan cấu hình và kỳ lương theo giáo viên (Phase 1 — chỉ xem).
        </p>
        <p className="text-sm text-muted-foreground mt-2">
          <span className="font-medium">Phase 2 (GAP-057b)</span> sẽ thêm: chạy bảng
          lương hằng tháng, duyệt và đánh dấu đã thanh toán, tải payslip PDF,
          xuất file chuyển khoản ngân hàng, tính thuế TNCN và BHXH/BHYT.
        </p>
      </div>

      {/* Configs table */}
      <Card>
        <CardHeader>
          <CardTitle>Cấu hình lương theo giáo viên</CardTitle>
        </CardHeader>
        <CardContent>
          {configsError && (
            <p className="text-sm text-destructive">
              Không thể tải cấu hình lương. Vui lòng thử lại.
            </p>
          )}
          {configsLoading && <p className="text-sm text-muted-foreground">Đang tải...</p>}
          {configs && configs.content.length === 0 && (
            <p className="text-sm text-muted-foreground">
              Chưa có cấu hình lương nào. Phase 2 (GAP-057b) sẽ thêm UI tạo cấu hình.
            </p>
          )}
          {configs && configs.content.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Giáo viên (ID)</TableHead>
                  <TableHead>Loại</TableHead>
                  <TableHead className="text-right">Đơn giá / giờ</TableHead>
                  <TableHead className="text-right">Lương cơ bản</TableHead>
                  <TableHead className="text-right">% Hoa hồng</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {configs.content.map((c) => (
                  <TableRow key={c.id}>
                    <TableCell>{c.id}</TableCell>
                    <TableCell>{c.teacherId}</TableCell>
                    <TableCell>
                      <Badge
                        variant={c.type === 'HOURLY' ? 'default' : 'secondary'}
                        title={c.type !== 'HOURLY' ? 'Phase 2 GAP-057b' : undefined}
                      >
                        {TYPE_LABEL[c.type]}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">{formatVnd(c.hourlyRate)}</TableCell>
                    <TableCell
                      className="text-right text-muted-foreground"
                      title="Phase 2 GAP-057b sẽ tính"
                    >
                      {formatVnd(c.baseSalary)}
                    </TableCell>
                    <TableCell
                      className="text-right text-muted-foreground"
                      title="Phase 2 GAP-057b sẽ tính"
                    >
                      {c.commissionPercent !== null ? `${c.commissionPercent}%` : '—'}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Periods filter + table */}
      <Card>
        <CardHeader>
          <CardTitle>Kỳ lương</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Filters */}
          <div className="flex flex-wrap gap-4">
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium" htmlFor="teacherId">
                ID giáo viên:
              </label>
              <Input
                id="teacherId"
                type="number"
                placeholder="(tất cả)"
                value={teacherIdFilter}
                onChange={(e) => setTeacherIdFilter(e.target.value)}
                className="w-[140px]"
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium" htmlFor="startDate">
                Từ:
              </label>
              <Input
                id="startDate"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-[160px]"
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium" htmlFor="endDate">
                Đến:
              </label>
              <Input
                id="endDate"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-[160px]"
              />
            </div>
          </div>

          {/* Table */}
          {periodsError && (
            <p className="text-sm text-destructive">
              Không thể tải kỳ lương. Vui lòng thử lại.
            </p>
          )}
          {periodsLoading && <p className="text-sm text-muted-foreground">Đang tải...</p>}
          {periods && periods.content.length === 0 && (
            <p className="text-sm text-muted-foreground">
              Không có kỳ lương theo bộ lọc. Kỳ lương được tạo bởi
              <code className="mx-1 rounded bg-muted px-1 text-xs">
                PayrollService.calculate(...)
              </code>
              ; Phase 2 (GAP-057b) sẽ thêm nút chạy bảng lương.
            </p>
          )}
          {periods && periods.content.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Giáo viên (ID)</TableHead>
                  <TableHead>Kỳ</TableHead>
                  <TableHead className="text-right">Số giờ</TableHead>
                  <TableHead className="text-right">Tổng (gross)</TableHead>
                  <TableHead className="text-right">Khấu trừ</TableHead>
                  <TableHead className="text-right">Thực nhận</TableHead>
                  <TableHead>Trạng thái</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {periods.content.map((p) => {
                  const badge = STATUS_BADGE[p.status];
                  return (
                    <TableRow key={p.id}>
                      <TableCell>{p.id}</TableCell>
                      <TableCell>{p.teacherId}</TableCell>
                      <TableCell>
                        {p.startDate} → {p.endDate}
                      </TableCell>
                      <TableCell className="text-right">{formatHours(p.hoursWorked)}</TableCell>
                      <TableCell className="text-right">{formatVnd(p.grossAmount)}</TableCell>
                      <TableCell className="text-right">{formatVnd(p.deductions)}</TableCell>
                      <TableCell className="text-right font-medium">
                        {formatVnd(p.netAmount)}
                      </TableCell>
                      <TableCell>
                        <span className={`inline-flex rounded-full px-2 py-1 text-xs font-medium ${badge.className}`}>
                          {badge.label}
                        </span>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
    </DashboardLayout>
  );
}
