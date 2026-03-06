/**
 * Invoice list page with filtering and pagination.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Plus, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { InvoiceStatusBadge } from '@/components/billing/invoice-status-badge';
import { useInvoices } from '@/hooks/use-invoices';
import { InvoiceStatus } from '@/types/invoice';
import { DataTable } from '@/components/common/data-table';
import { formatCurrency, formatDate } from '@/lib/utils';
import type { ColumnDef } from '@tanstack/react-table';
import type { Invoice } from '@/types/invoice';

export default function BillingPage() {
  const [searchParams, setSearchParams] = useState({
    page: 0,
    size: 20,
    status: undefined as InvoiceStatus | undefined,
    studentId: undefined as number | undefined,
  });

  const { data, isLoading, error } = useInvoices(searchParams);

  const columns: ColumnDef<Invoice>[] = [
    {
      accessorKey: 'invoiceNumber',
      header: 'Số hóa đơn',
      cell: ({ row }) => (
        <Link
          href={`/billing/${row.original.id}`}
          className="font-medium hover:underline"
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
      cell: ({ row }) => formatCurrency(row.original.total),
    },
    {
      accessorKey: 'balanceDue',
      header: 'Còn lại',
      cell: ({ row }) => (
        <span
          className={row.original.balanceDue > 0 ? 'text-destructive' : ''}
        >
          {formatCurrency(row.original.balanceDue)}
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
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Hóa đơn</h1>
          <p className="text-muted-foreground">
            Quản lý hóa đơn và thanh toán
          </p>
        </div>
        <Link href="/billing/new">
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            Tạo hóa đơn
          </Button>
        </Link>
      </div>

      <div className="flex gap-4">
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

      {isLoading && <div>Đang tải...</div>}
      {error && (
        <div className="flex items-center gap-2 rounded-lg border border-destructive/50 p-4 text-destructive">
          <AlertCircle className="h-5 w-5" />
          <p>Không thể tải dữ liệu</p>
        </div>
      )}
      {data && <DataTable columns={columns} data={data.content} />}
    </div>
  );
}
