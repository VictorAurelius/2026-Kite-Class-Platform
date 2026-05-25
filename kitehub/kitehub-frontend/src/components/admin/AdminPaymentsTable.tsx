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
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import { Check, X, QrCode, Loader2 } from 'lucide-react';
import { useConfirmPayment, useRejectPayment } from '@/hooks/use-admin';
import type { AdminPayment } from '@/types/admin';
import type { PaymentMethod } from '@/types/payment';
import { toast } from 'sonner';

interface AdminPaymentsTableProps {
  payments: AdminPayment[];
}

const methodLabels: Record<PaymentMethod, string> = {
  VIETQR: 'VietQR',
  MOMO: 'MoMo',
  VNPAY: 'VNPay',
  BANK_TRANSFER: 'Chuyển khoản',
  MANUAL: 'Thủ công',
};

export function AdminPaymentsTable({ payments }: AdminPaymentsTableProps) {
  // Selection state
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  // Confirm dialog state
  const [confirmPayment, setConfirmPayment] = useState<AdminPayment | null>(null);
  const [transactionId, setTransactionId] = useState('');

  // Reject dialog state
  const [rejectPayment, setRejectPayment] = useState<AdminPayment | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  // QR preview state
  const [previewQR, setPreviewQR] = useState<string | null>(null);

  const confirmMutation = useConfirmPayment();
  const rejectMutation = useRejectPayment();

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

  const toggleSelect = (id: string) => {
    const newSelected = new Set(selectedIds);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    setSelectedIds(newSelected);
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === payments.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(payments.map((p) => p.id)));
    }
  };

  const handleConfirm = async () => {
    if (!confirmPayment || !transactionId.trim()) return;

    try {
      await confirmMutation.mutateAsync({
        paymentId: confirmPayment.id,
        request: { transactionId: transactionId.trim() },
      });
      toast.success('Đã xác nhận thanh toán thành công');
      setConfirmPayment(null);
      setTransactionId('');
    } catch {
      toast.error('Có lỗi xảy ra. Vui lòng thử lại.');
    }
  };

  const handleReject = async () => {
    if (!rejectPayment || !rejectReason.trim()) return;

    try {
      await rejectMutation.mutateAsync({
        paymentId: rejectPayment.id,
        request: { reason: rejectReason.trim() },
      });
      toast.success('Đã từ chối thanh toán');
      setRejectPayment(null);
      setRejectReason('');
    } catch {
      toast.error('Có lỗi xảy ra. Vui lòng thử lại.');
    }
  };

  const handleBulkConfirm = async () => {
    if (selectedIds.size === 0) return;

    const sampleTxId = `BULK_${Date.now()}`;
    const results = await Promise.allSettled(
      Array.from(selectedIds).map((id) =>
        confirmMutation.mutateAsync({
          paymentId: id,
          request: { transactionId: sampleTxId },
        })
      )
    );

    const successCount = results.filter((r) => r.status === 'fulfilled').length;
    const failCount = results.filter((r) => r.status === 'rejected').length;

    if (successCount > 0) {
      toast.success(`Đã xác nhận ${successCount} thanh toán`);
    }
    if (failCount > 0) {
      toast.error(`${failCount} thanh toán không thể xác nhận`);
    }

    setSelectedIds(new Set());
  };

  return (
    <div className="space-y-4">
      {/* Bulk actions */}
      {selectedIds.size > 0 && (
        <div className="flex items-center gap-4 p-4 bg-muted rounded-lg">
          <span className="text-sm">
            Đã chọn <strong>{selectedIds.size}</strong> thanh toán
          </span>
          <Button size="sm" onClick={handleBulkConfirm}>
            <Check className="mr-2 h-4 w-4" />
            Xác nhận tất cả
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => setSelectedIds(new Set())}
          >
            Bỏ chọn
          </Button>
        </div>
      )}

      {/* Table */}
      <div className="border rounded-lg">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-12">
                <Checkbox
                  checked={payments.length > 0 && selectedIds.size === payments.length}
                  onCheckedChange={toggleSelectAll}
                />
              </TableHead>
              <TableHead>Mã thanh toán</TableHead>
              <TableHead>Số tiền</TableHead>
              <TableHead>Phương thức</TableHead>
              <TableHead>Nội dung</TableHead>
              <TableHead>Ngày tạo</TableHead>
              <TableHead>QR</TableHead>
              <TableHead className="text-right">Hành động</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {payments.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-muted-foreground py-8">
                  Không có thanh toán nào đang chờ xác nhận
                </TableCell>
              </TableRow>
            ) : (
              payments.map((payment) => (
                <TableRow key={payment.id}>
                  <TableCell>
                    <Checkbox
                      checked={selectedIds.has(payment.id)}
                      onCheckedChange={() => toggleSelect(payment.id)}
                    />
                  </TableCell>
                  <TableCell className="font-mono text-sm">
                    {payment.id.substring(0, 8).toUpperCase()}
                  </TableCell>
                  <TableCell className="font-medium">
                    {formatAmount(payment.amountVnd)}
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">
                      {methodLabels[payment.paymentMethod]}
                    </Badge>
                  </TableCell>
                  <TableCell className="max-w-[200px] truncate">
                    {payment.paymentContent || '-'}
                  </TableCell>
                  <TableCell>{formatDate(payment.createdAt)}</TableCell>
                  <TableCell>
                    {payment.qrCodeUrl ? (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setPreviewQR(payment.qrCodeUrl)}
                      >
                        <QrCode className="h-4 w-4" />
                      </Button>
                    ) : (
                      '-'
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button
                        size="sm"
                        variant="default"
                        onClick={() => setConfirmPayment(payment)}
                      >
                        <Check className="mr-1 h-4 w-4" />
                        Xác nhận
                      </Button>
                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => setRejectPayment(payment)}
                      >
                        <X className="mr-1 h-4 w-4" />
                        Từ chối
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Summary */}
      <div className="flex justify-between items-center text-sm text-muted-foreground">
        <span>{payments.length} thanh toán đang chờ xác nhận</span>
        <span>
          Tổng giá trị:{' '}
          <strong className="text-foreground">
            {formatAmount(payments.reduce((sum, p) => sum + p.amountVnd, 0))}
          </strong>
        </span>
      </div>

      {/* Confirm Dialog */}
      <Dialog open={!!confirmPayment} onOpenChange={() => setConfirmPayment(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xác nhận thanh toán</DialogTitle>
            <DialogDescription>
              Xác nhận thanh toán{' '}
              <strong>{formatAmount(confirmPayment?.amountVnd ?? 0)}</strong>
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="transactionId">Mã giao dịch ngân hàng</Label>
              <Input
                id="transactionId"
                value={transactionId}
                onChange={(e) => setTransactionId(e.target.value)}
                placeholder="Nhập mã giao dịch từ ngân hàng"
              />
            </div>
            {confirmPayment?.paymentContent && (
              <div className="space-y-2">
                <Label>Nội dung chuyển khoản</Label>
                <p className="text-sm bg-muted p-2 rounded font-mono">
                  {confirmPayment.paymentContent}
                </p>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmPayment(null)}>
              Hủy
            </Button>
            <Button
              onClick={handleConfirm}
              disabled={!transactionId.trim() || confirmMutation.isPending}
            >
              {confirmMutation.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Xác nhận
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog open={!!rejectPayment} onOpenChange={() => setRejectPayment(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Từ chối thanh toán</DialogTitle>
            <DialogDescription>
              Từ chối thanh toán{' '}
              <strong>{formatAmount(rejectPayment?.amountVnd ?? 0)}</strong>
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="rejectReason">Lý do từ chối</Label>
              <Textarea
                id="rejectReason"
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Nhập lý do từ chối thanh toán"
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRejectPayment(null)}>
              Hủy
            </Button>
            <Button
              variant="destructive"
              onClick={handleReject}
              disabled={!rejectReason.trim() || rejectMutation.isPending}
            >
              {rejectMutation.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Từ chối
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* QR Preview Dialog */}
      <Dialog open={!!previewQR} onOpenChange={() => setPreviewQR(null)}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Mã QR thanh toán</DialogTitle>
          </DialogHeader>
          <div className="flex justify-center py-4">
            {previewQR && (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={previewQR}
                alt="QR Code"
                className="max-w-full h-auto"
              />
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
