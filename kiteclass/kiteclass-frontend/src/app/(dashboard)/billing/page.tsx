/**
 * Invoice list page — KC pro v2 token-styled overview.
 *
 * Wave 30 Bucket D — applies kiteclass-pro-v2 design tokens (soft shadow,
 * larger radius, semantic palette via Tailwind/shadcn) to the existing
 * filter+table layout. Adds owner-facing summary tiles using
 * `formatVNCurrency` re-exported from `@kite/shared-ui` (G6 utils per Wave 27).
 *
 * @author KiteClass Team
 * @since 1.0.0 — token application Wave 30 (GAP-266)
 */

'use client';

import { useState, useMemo } from 'react';
import Link from 'next/link';
import { Plus, AlertCircle, FileText, Receipt, Wallet, AlertTriangle, CalendarPlus } from 'lucide-react';
import { formatVNCurrency } from '@kite/shared-ui';
import { DashboardLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { InvoiceStatusBadge } from '@/components/billing/invoice-status-badge';
import { BatchInvoiceDrawer } from '@/components/billing/batch-invoice-drawer';
import { useInvoices } from '@/hooks/use-invoices';
import { InvoiceStatus } from '@/types/invoice';
import { DataTable } from '@/components/common/dynamic-data-table';
import { formatDate } from '@/lib/utils';
import type { ColumnDef } from '@tanstack/react-table';
import type { Invoice } from '@/types/invoice';

export default function BillingPage() {
  const [searchParams, setSearchParams] = useState({
    page: 0,
    size: 20,
    status: undefined as InvoiceStatus | undefined,
    studentId: undefined as number | undefined,
  });

  const [batchOpen, setBatchOpen] = useState(false);

  const { data, isLoading, error } = useInvoices(searchParams);

  // Owner KPIs derived from current page (pro v2 design — surface money KPIs
  // up-front so owners see scope before scanning rows).
  const summary = useMemo(() => {
    const rows = data?.content ?? [];
    const totalOutstanding = rows.reduce((acc, inv) => acc + (inv.balanceDue ?? 0), 0);
    const totalPaid = rows.reduce((acc, inv) => acc + (inv.amountPaid ?? 0), 0);
    const overdueCount = rows.filter((inv) => inv.status === InvoiceStatus.OVERDUE).length;
    return { totalOutstanding, totalPaid, overdueCount, count: rows.length };
  }, [data]);

  const columns: ColumnDef<Invoice>[] = [
    {
      accessorKey: 'invoiceNumber',
      header: 'Số hóa đơn',
      cell: ({ row }) => (
        <Link
          href={`/billing/${row.original.id}`}
          className="font-medium text-primary hover:underline"
        >
          {row.original.invoiceNumber}
        </Link>
      ),
    },
    {
      accessorKey: 'studentId',
      header: 'Học viên',
      cell: ({ row }) => `#${row.original.studentId}`,
    },
    {
      accessorKey: 'total',
      header: 'Tổng tiền',
      cell: ({ row }) => formatVNCurrency(row.original.total),
    },
    {
      accessorKey: 'balanceDue',
      header: 'Còn lại',
      cell: ({ row }) => (
        <span
          className={row.original.balanceDue > 0 ? 'font-medium text-destructive' : 'text-muted-foreground'}
        >
          {formatVNCurrency(row.original.balanceDue)}
        </span>
      ),
    },
    {
      accessorKey: 'status',
      header: 'Trạng thái',
      cell: ({ row }) => <InvoiceStatusBadge status={row.original.status} />,
    },
    {
      accessorKey: 'dueDate',
      header: 'Hạn thanh toán',
      cell: ({ row }) => formatDate(row.original.dueDate),
    },
  ];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Hóa đơn</h1>
            <p className="text-muted-foreground">Quản lý hóa đơn và thanh toán</p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button
              variant="outline"
              className="w-full sm:w-auto"
              onClick={() => setBatchOpen(true)}
            >
              <CalendarPlus className="mr-2 h-4 w-4" />
              Tạo hóa đơn tháng
            </Button>
            <Link href="/billing/new">
              <Button className="w-full sm:w-auto">
                <Plus className="mr-2 h-4 w-4" />
                Tạo hóa đơn
              </Button>
            </Link>
          </div>
        </div>

        <BatchInvoiceDrawer open={batchOpen} onOpenChange={setBatchOpen} />

        {/* Summary KPIs — pro v2 token-style cards */}
        <div className="grid gap-4 sm:grid-cols-3" data-testid="billing-summary">
          <Card className="rounded-xl shadow-sm">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Còn phải thu
              </CardTitle>
              <Wallet className="h-4 w-4 text-muted-foreground" aria-hidden />
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{formatVNCurrency(summary.totalOutstanding)}</p>
              <p className="text-xs text-muted-foreground">{summary.count} hóa đơn trong trang</p>
            </CardContent>
          </Card>
          <Card className="rounded-xl shadow-sm">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Đã thanh toán
              </CardTitle>
              <Receipt className="h-4 w-4 text-muted-foreground" aria-hidden />
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{formatVNCurrency(summary.totalPaid)}</p>
              <p className="text-xs text-muted-foreground">Tích lũy trang hiện tại</p>
            </CardContent>
          </Card>
          <Card className="rounded-xl shadow-sm">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Quá hạn
              </CardTitle>
              <AlertTriangle className="h-4 w-4 text-destructive" aria-hidden />
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">
                {summary.overdueCount}
                <span className="ml-2 text-sm font-normal text-muted-foreground">hóa đơn</span>
              </p>
              <p className="text-xs text-muted-foreground">Cần liên hệ phụ huynh</p>
            </CardContent>
          </Card>
        </div>

        <div className="flex flex-col gap-4 sm:flex-row">
          <Input
            type="number"
            placeholder="ID học viên..."
            className="max-w-xs"
            onChange={(e) =>
              setSearchParams({
                ...searchParams,
                studentId: e.target.value ? parseInt(e.target.value) : undefined,
              })
            }
          />
          <Select
            value={searchParams.status || 'all'}
            onValueChange={(value) =>
              setSearchParams({
                ...searchParams,
                status: value === 'all' ? undefined : (value as InvoiceStatus),
              })
            }
          >
            <SelectTrigger className="w-48">
              <SelectValue placeholder="Tất cả trạng thái" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả</SelectItem>
              <SelectItem value={InvoiceStatus.PENDING}>Chờ thanh toán</SelectItem>
              <SelectItem value={InvoiceStatus.PAID}>Đã thanh toán</SelectItem>
              <SelectItem value={InvoiceStatus.OVERDUE}>Quá hạn</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {isLoading && (
          <div className="flex justify-center py-12">
            <div
              className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"
              aria-label="Đang tải"
            />
          </div>
        )}
        {error && (
          <div className="flex items-center gap-2 rounded-lg border border-destructive/50 p-4 text-destructive">
            <AlertCircle className="h-5 w-5" />
            <p>Không thể tải dữ liệu. Vui lòng thử lại.</p>
          </div>
        )}
        {!isLoading && !error && !data && (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed py-12 text-center">
            <FileText className="mb-4 h-12 w-12 text-muted-foreground" />
            <p className="text-lg font-medium">Chưa có hóa đơn nào</p>
            <p className="mt-1 text-sm text-muted-foreground">Tạo hóa đơn mới để bắt đầu.</p>
          </div>
        )}
        {data && <DataTable columns={columns} data={data.content} />}
      </div>
    </DashboardLayout>
  );
}
