'use client';

import { useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Eye } from 'lucide-react';
import { useRouter } from 'next/navigation';
import type { Payment, PaymentStatus } from '@/types/payment';

interface PaymentHistoryTableProps {
  payments: Payment[];
}

export function PaymentHistoryTable({ payments }: PaymentHistoryTableProps) {
  const router = useRouter();
  const [statusFilter, setStatusFilter] = useState<PaymentStatus | 'ALL'>('ALL');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');

  const statusConfig: Record<PaymentStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }> = {
    PENDING: { label: 'Đang chờ', variant: 'secondary' },
    COMPLETED: { label: 'Thành công', variant: 'default' },
    FAILED: { label: 'Thất bại', variant: 'destructive' },
    EXPIRED: { label: 'Hết hạn', variant: 'outline' },
  };

  const formatAmount = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  };

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Filter payments
  const filteredPayments = payments.filter((payment) => {
    if (statusFilter === 'ALL') return true;
    return payment.status === statusFilter;
  });

  // Sort payments
  const sortedPayments = [...filteredPayments].sort((a, b) => {
    const dateA = new Date(a.createdAt).getTime();
    const dateB = new Date(b.createdAt).getTime();
    return sortOrder === 'desc' ? dateB - dateA : dateA - dateB;
  });

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium">Trạng thái:</label>
          <Select
            value={statusFilter}
            onValueChange={(value) => setStatusFilter(value as PaymentStatus | 'ALL')}
          >
            <SelectTrigger className="w-[180px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Tất cả</SelectItem>
              <SelectItem value="COMPLETED">Thành công</SelectItem>
              <SelectItem value="PENDING">Đang chờ</SelectItem>
              <SelectItem value="FAILED">Thất bại</SelectItem>
              <SelectItem value="EXPIRED">Hết hạn</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="flex items-center gap-2">
          <label className="text-sm font-medium">Sắp xếp:</label>
          <Select
            value={sortOrder}
            onValueChange={(value) => setSortOrder(value as 'asc' | 'desc')}
          >
            <SelectTrigger className="w-[180px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="desc">Mới nhất</SelectItem>
              <SelectItem value="asc">Cũ nhất</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Table */}
      <div className="border rounded-lg">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mã giao dịch</TableHead>
              <TableHead>Ngày tạo</TableHead>
              <TableHead>Số tiền</TableHead>
              <TableHead>Phương thức</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Hành động</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {sortedPayments.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                  Không có giao dịch nào
                </TableCell>
              </TableRow>
            ) : (
              sortedPayments.map((payment) => (
                <TableRow key={payment.id}>
                  <TableCell className="font-mono text-sm">
                    {payment.id.substring(0, 8).toUpperCase()}
                  </TableCell>
                  <TableCell>{formatDate(payment.createdAt)}</TableCell>
                  <TableCell className="font-medium">
                    {formatAmount(payment.amountVnd)}
                  </TableCell>
                  <TableCell>
                    {payment.paymentMethod === 'VIETQR' ? 'VietQR' : payment.paymentMethod}
                  </TableCell>
                  <TableCell>
                    <Badge variant={statusConfig[payment.status].variant}>
                      {statusConfig[payment.status].label}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => router.push(`/billing/payment/${payment.id}`)}
                    >
                      <Eye className="h-4 w-4 mr-1" />
                      Xem
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Summary */}
      <div className="flex justify-between items-center text-sm text-muted-foreground">
        <span>
          Hiển thị {sortedPayments.length} / {payments.length} giao dịch
        </span>
        <span>
          Tổng giá trị:{' '}
          <strong className="text-foreground">
            {formatAmount(
              sortedPayments.reduce((sum, p) => sum + p.amountVnd, 0)
            )}
          </strong>
        </span>
      </div>
    </div>
  );
}
