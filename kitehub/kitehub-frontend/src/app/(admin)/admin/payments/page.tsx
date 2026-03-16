'use client';

import { useAdminPendingPayments } from '@/hooks/use-admin';
import { AdminPaymentsTable } from '@/components/admin/AdminPaymentsTable';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { RefreshCw } from 'lucide-react';

export default function AdminPaymentsPage() {
  const { data: payments, isLoading, error, refetch, isFetching } = useAdminPendingPayments();

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold">Xác nhận thanh toán</h1>
          <p className="mt-1 text-muted-foreground">
            Xác nhận hoặc từ chối các thanh toán đang chờ xử lý
          </p>
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

      {/* Info Banner */}
      <div className="mb-6 p-4 bg-muted/50 rounded-lg border">
        <p className="text-sm">
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
