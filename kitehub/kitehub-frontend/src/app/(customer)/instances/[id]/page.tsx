'use client';

import { use } from 'react';
import Link from 'next/link';
import { useInstance, useTrialStatus } from '@/hooks/use-instances';
import { StatusBadge } from '@/components/common/StatusBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { TrialCountdown } from '@/components/common/TrialCountdown';
import { formatDate } from '@/lib/utils';

export default function InstanceDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const instanceId = Number(id);
  const { data: instance, isLoading, error } = useInstance(instanceId);
  const { data: trialStatus } = useTrialStatus(
    instance?.status === 'TRIAL' ? instanceId : undefined
  );

  if (isLoading) return <LoadingSpinner className="mt-12" />;
  if (error) return <ErrorAlert message="Không thể tải thông tin instance" />;
  if (!instance) return null;

  return (
    <div>
      <div className="flex items-center gap-2">
        <Link href="/dashboard" className="text-sm text-muted-foreground hover:text-foreground">
          Dashboard
        </Link>
        <span className="text-muted-foreground">/</span>
        <span className="text-sm font-medium">{instance.organizationName}</span>
      </div>

      <div className="mt-6 flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold">{instance.organizationName}</h1>
          <p className="mt-1 text-muted-foreground">
            {instance.subdomain}.kiteclass.com
          </p>
        </div>
        <StatusBadge status={instance.status} />
      </div>

      <div className="mt-8 grid gap-6 md:grid-cols-2">
        {/* Instance Info */}
        <div className="rounded-lg border p-6">
          <h2 className="text-lg font-semibold">Thông tin</h2>
          <dl className="mt-4 space-y-3">
            <div className="flex justify-between">
              <dt className="text-sm text-muted-foreground">Subdomain</dt>
              <dd className="text-sm font-medium">{instance.subdomain}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-sm text-muted-foreground">Email</dt>
              <dd className="text-sm font-medium">{instance.ownerEmail}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-sm text-muted-foreground">Gói</dt>
              <dd className="text-sm font-medium">{instance.tier}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-sm text-muted-foreground">Ngày tạo</dt>
              <dd className="text-sm font-medium">{formatDate(instance.createdAt)}</dd>
            </div>
          </dl>
        </div>

        {/* Trial Status */}
        {trialStatus && (
          <div className="rounded-lg border p-6">
            <h2 className="text-lg font-semibold">Trial</h2>
            <div className="mt-4">
              <TrialCountdown trial={trialStatus} />
            </div>
            {trialStatus.daysRemaining <= 3 && !trialStatus.expired && (
              <div className="mt-4 rounded bg-orange-50 p-3 text-sm text-orange-700 dark:bg-orange-950 dark:text-orange-300">
                Trial sắp hết hạn. Nâng cấp gói để tiếp tục sử dụng.
              </div>
            )}
          </div>
        )}
      </div>

      {/* Action Buttons */}
      <div className="mt-8 flex flex-wrap gap-3">
        <a
          href={`https://${instance.subdomain}.kiteclass.com`}
          target="_blank"
          rel="noopener noreferrer"
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Truy cập KiteClass
        </a>
        <Link
          href="/billing"
          className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
        >
          Nâng cấp gói
        </Link>
        <Link
          href="/branding"
          className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
        >
          AI Branding
        </Link>
      </div>
    </div>
  );
}
