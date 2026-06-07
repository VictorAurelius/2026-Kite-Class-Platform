/**
 * BatchInvoiceDrawer — Tạo hóa đơn học phí hàng tháng cho cả trung tâm (GAP-297).
 *
 * Owner mở drawer → chọn tháng (mặc định tháng hiện tại) → xem trước số hóa đơn +
 * tổng doanh thu + danh sách per-học viên (POST batch-generate, KHÔNG persist) →
 * bấm "Xác nhận tạo" (POST batch-confirm) → toast + refresh danh sách hóa đơn.
 *
 * Flow ≤3 click: nút "Tạo hóa đơn tháng" → drawer auto-preview → "Xác nhận tạo".
 * Mobile-friendly: drawer full-width trên 375px, list cuộn được.
 *
 * Per api-contract.md §3.11/§3.12.
 *
 * @since GAP-297 (Wave p0-ux-1 Bucket D)
 */

'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, FileText, Loader2, Users, Wallet } from 'lucide-react';
import type { AxiosError } from 'axios';
import { formatVNCurrency } from '@kite/shared-ui';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import { useToast } from '@/hooks/use-toast';
import { invoicesApi } from '@/lib/api/invoices';

interface BatchInvoiceDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** YYYY-MM của tháng hiện tại (local). */
function currentMonth(): string {
  return new Date().toISOString().slice(0, 7);
}

/** "2026-05" → "tháng 5/2026". */
function formatMonthVi(month: string): string {
  const [year, m] = month.split('-');
  if (!year || !m) return month;
  return `tháng ${parseInt(m, 10)}/${year}`;
}

/**
 * Trích thông điệp lỗi thực tế từ backend (KHÔNG nuốt bằng catch generic).
 * Hỗ trợ cả 2 shape: ApiResponse `{ error: { message } }` + raw `{ message }`.
 */
function extractErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const axiosErr = err as AxiosError<{
      message?: string;
      error?: { message?: string };
    }>;
    return (
      axiosErr.response?.data?.error?.message ||
      axiosErr.response?.data?.message ||
      fallback
    );
  }
  return err instanceof Error ? err.message : fallback;
}

export function BatchInvoiceDrawer({
  open,
  onOpenChange,
}: BatchInvoiceDrawerProps) {
  const [month, setMonth] = useState<string>(currentMonth);
  const queryClient = useQueryClient();
  const { toast } = useToast();

  // Preview (batch-generate) — auto-fetch khi drawer mở + đổi tháng. Không persist.
  const preview = useQuery({
    queryKey: ['batch-invoice-preview', month],
    queryFn: () => invoicesApi.batchGenerate(month),
    enabled: open && !!month,
    retry: false,
  });

  // Confirm (batch-confirm) — persist + emit InvoiceCreated events.
  const confirm = useMutation({
    mutationFn: () => invoicesApi.batchConfirm(month),
    onSuccess: (data) => {
      toast({
        title: 'Thành công',
        description: `Đã tạo ${data.createdCount} hóa đơn, đang gửi cha mẹ`,
      });
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
      onOpenChange(false);
    },
    // Lỗi được toast bởi global interceptor + hiển thị inline bên dưới (§error state).
  });

  const data = preview.data;
  const isEmpty = !!data && data.invoiceCount === 0;
  const previewError = preview.error
    ? extractErrorMessage(
        preview.error,
        'Không thể xem trước hóa đơn. Vui lòng thử lại.'
      )
    : null;
  const confirmError = confirm.error
    ? extractErrorMessage(
        confirm.error,
        'Không thể tạo hóa đơn. Vui lòng thử lại.'
      )
    : null;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        className="flex w-full flex-col gap-0 overflow-y-auto p-0 sm:max-w-lg"
      >
        <SheetHeader className="border-b p-6">
          <SheetTitle>Tạo hóa đơn tháng</SheetTitle>
          <SheetDescription>
            Tạo hóa đơn học phí hàng tháng cho toàn bộ học viên đang học. Xem
            trước trước khi xác nhận.
          </SheetDescription>
        </SheetHeader>

        <div className="flex-1 space-y-6 p-6">
          {/* Chọn tháng */}
          <div className="space-y-2">
            <Label htmlFor="batch-month">Tháng tính phí</Label>
            <input
              id="batch-month"
              type="month"
              value={month}
              onChange={(e) => setMonth(e.target.value || currentMonth())}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            />
          </div>

          {/* Loading */}
          {preview.isLoading && (
            <div className="flex flex-col items-center justify-center gap-3 py-12 text-muted-foreground">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <p className="text-sm">Đang tính toán hóa đơn {formatMonthVi(month)}…</p>
            </div>
          )}

          {/* Error (preview) — hiển thị lý do thật từ backend */}
          {!preview.isLoading && previewError && (
            <div
              className="flex items-start gap-2 rounded-lg border border-destructive/50 p-4 text-sm text-destructive"
              role="alert"
            >
              <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" />
              <div className="space-y-2">
                <p>{previewError}</p>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => preview.refetch()}
                >
                  Thử lại
                </Button>
              </div>
            </div>
          )}

          {/* Empty — không có enrollment đang hoạt động */}
          {!preview.isLoading && !previewError && isEmpty && (
            <div className="flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed py-12 text-center">
              <FileText className="mb-1 h-12 w-12 text-muted-foreground" />
              <p className="text-base font-medium">
                Không có học viên nào đang học trong {formatMonthVi(month)}
              </p>
              <p className="text-sm text-muted-foreground">
                Chưa có enrollment đang hoạt động — không có hóa đơn nào để tạo.
                Hãy chọn tháng khác hoặc ghi danh học viên trước.
              </p>
            </div>
          )}

          {/* Preview data */}
          {!preview.isLoading && !previewError && data && !isEmpty && (
            <div className="space-y-4">
              {/* Summary tiles */}
              <div className="grid grid-cols-2 gap-3" data-testid="batch-summary">
                <div className="rounded-xl border p-4">
                  <div className="flex items-center justify-between pb-1">
                    <span className="text-sm text-muted-foreground">
                      Số hóa đơn
                    </span>
                    <Users className="h-4 w-4 text-muted-foreground" aria-hidden />
                  </div>
                  <p className="text-2xl font-bold">{data.invoiceCount}</p>
                </div>
                <div className="rounded-xl border p-4">
                  <div className="flex items-center justify-between pb-1">
                    <span className="text-sm text-muted-foreground">
                      Tổng doanh thu
                    </span>
                    <Wallet className="h-4 w-4 text-muted-foreground" aria-hidden />
                  </div>
                  <p className="text-2xl font-bold">
                    {formatVNCurrency(data.totalRevenue)}
                  </p>
                </div>
              </div>

              {/* Danh sách per-học viên */}
              <div className="space-y-2">
                <p className="text-sm font-medium text-muted-foreground">
                  Chi tiết theo học viên
                </p>
                <ul className="divide-y rounded-xl border" data-testid="batch-line-items">
                  {data.invoices.map((item) => (
                    <li
                      key={item.enrollmentId}
                      className="flex items-center justify-between gap-3 p-3"
                    >
                      <div className="min-w-0">
                        <p className="truncate font-medium">
                          {item.classNameVi}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          Học viên #{item.studentId}
                          {item.discountPercent > 0 && (
                            <> · Giảm {item.discountPercent}%</>
                          )}
                          {item.prorated && (
                            <>
                              {' '}
                              · Tính theo {item.billableDays}/{item.daysInMonth}{' '}
                              ngày
                            </>
                          )}
                        </p>
                      </div>
                      <span className="shrink-0 font-medium">
                        {formatVNCurrency(item.total)}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          )}

          {/* Error (confirm) — lý do thật từ backend */}
          {confirmError && (
            <div
              className="flex items-start gap-2 rounded-lg border border-destructive/50 p-4 text-sm text-destructive"
              role="alert"
            >
              <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" />
              <p>{confirmError}</p>
            </div>
          )}
        </div>

        {/* Footer actions */}
        <div className="flex flex-col-reverse gap-2 border-t p-6 sm:flex-row sm:justify-end">
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={confirm.isPending}
          >
            Hủy
          </Button>
          <Button
            onClick={() => confirm.mutate()}
            disabled={
              preview.isLoading ||
              !!previewError ||
              isEmpty ||
              !data ||
              confirm.isPending
            }
          >
            {confirm.isPending ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Đang tạo…
              </>
            ) : (
              <>Xác nhận tạo {data ? `${data.invoiceCount} hóa đơn` : ''}</>
            )}
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  );
}
