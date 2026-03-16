'use client';

import { useAdminInstances } from '@/hooks/use-admin';
import { AdminInstancesTable } from '@/components/admin/AdminInstancesTable';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { RefreshCw } from 'lucide-react';

export default function AdminInstancesPage() {
  const { data: instances, isLoading, error, refetch, isFetching } = useAdminInstances();

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold">Quản lý Instances</h1>
          <p className="mt-1 text-muted-foreground">
            Xem và quản lý tất cả instances trên nền tảng
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

      {/* Content */}
      {isLoading && (
        <div className="flex justify-center py-12">
          <LoadingSpinner />
        </div>
      )}

      {error && (
        <ErrorAlert
          message="Không thể tải danh sách instances"
          onRetry={() => refetch()}
        />
      )}

      {instances && <AdminInstancesTable instances={instances} />}
    </div>
  );
}
