'use client';

import { useAdminPendingPayments } from '@/hooks/use-admin';
import { AdminPaymentsTable } from '@/components/admin/AdminPaymentsTable';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { RefreshCw, CreditCard, Info } from 'lucide-react';

export default function AdminPaymentsPage() {
  const { data: payments, isLoading, error, refetch, isFetching } = useAdminPendingPayments();

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-primary/10 p-3 text-primary">
              <CreditCard className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Xác nhận thanh toán</h1>
              <p className="text-muted-foreground">
                Xác nhận hoặc từ chối các thanh toán đang chờ xử lý
              </p>
            </div>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching}
          >
            <RefreshCw className={`mr-2 h-4 w-4 ${isFetching ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>
      </div>

      {/* Info Banner */}
      <div className="flex items-start gap-3 p-4 rounded-xl bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800">
        <div className="rounded-lg bg-blue-500/10 p-2 text-blue-600">
          <Info className="h-4 w-4" />
        </div>
        <p className="text-sm text-blue-700 dark:text-blue-300">
          <strong>Lưu ý:</strong> Trang này tự động làm mới mỗi 30 giây. Khi xác nhận
          thanh toán, subscription của khách hàng sẽ được kích hoạt tự động.
        </p>
      </div>

      {/* Content */}
      {isLoading && (
        <div className="flex justify-center py-12">
          <LoadingSpinner />
        </div>
      )}

      {error && (
        <ErrorAlert
          message="Không thể tải danh sách thanh toán"
          onRetry={() => refetch()}
        />
      )}

      {payments && <AdminPaymentsTable payments={payments} />}
    </div>
  );
}
