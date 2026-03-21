'use client';

import { useAdminInstances } from '@/hooks/use-admin';
import { AdminInstancesTable } from '@/components/admin/AdminInstancesTable';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Button } from '@/components/ui/button';
import { RefreshCw, Building2 } from 'lucide-react';

export default function AdminInstancesPage() {
  const { data: instances, isLoading, error, refetch, isFetching } = useAdminInstances();

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-primary/10 p-3 text-primary">
              <Building2 className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Quản lý Instances</h1>
              <p className="text-muted-foreground">
                Xem và quản lý tất cả instances trên nền tảng
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
