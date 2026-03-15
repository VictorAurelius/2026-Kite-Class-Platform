'use client';

import Link from 'next/link';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { StatusBadge } from '@/components/common/StatusBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { EmptyState } from '@/components/common/EmptyState';
import { formatDate } from '@/lib/utils';

export default function DashboardPage() {
  const { user } = useAuthStore();
  const { data: instances, isLoading, error, refetch } = useOwnerInstances(user?.id);

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Dashboard</h1>
          <p className="mt-1 text-muted-foreground">
            Xin chào, {user?.name ?? user?.email}
          </p>
        </div>
      </div>

      {isLoading && <LoadingSpinner className="mt-12" />}

      {error && (
        <ErrorAlert
          message="Không thể tải danh sách instance"
          onRetry={() => refetch()}
        />
      )}

      {instances && instances.length === 0 && (
        <EmptyState
          title="Chưa có instance nào"
          description="Tạo instance KiteClass đầu tiên để bắt đầu quản lý trung tâm giáo dục"
          action={{
            label: 'Tạo instance mới',
            onClick: () => window.location.href = '/register',
          }}
        />
      )}

      {instances && instances.length > 0 && (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {instances.map((instance) => (
            <Link
              key={instance.id}
              href={`/instances/${instance.id}`}
              className="rounded-lg border bg-card p-6 shadow-sm transition-shadow hover:shadow-md"
            >
              <div className="flex items-start justify-between">
                <h3 className="font-semibold">{instance.organizationName}</h3>
                <StatusBadge status={instance.status} />
              </div>
              <p className="mt-1 text-sm text-muted-foreground">
                {instance.subdomain}.kiteclass.com
              </p>
              <div className="mt-4 flex items-center justify-between text-sm">
                <span className="rounded bg-muted px-2 py-0.5 text-xs font-medium">
                  {instance.tier}
                </span>
                <span className="text-muted-foreground">
                  {formatDate(instance.createdAt)}
                </span>
              </div>
              {instance.status === 'TRIAL' && instance.trialEndDate && (
                <div className="mt-3 rounded bg-blue-50 px-3 py-1.5 text-xs text-blue-700 dark:bg-blue-950 dark:text-blue-300">
                  Trial hết hạn: {formatDate(instance.trialEndDate)}
                </div>
              )}
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
