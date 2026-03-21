'use client';

import { use } from 'react';
import Link from 'next/link';
import { useInstance, useTrialStatus } from '@/hooks/use-instances';
import { StatusBadge } from '@/components/common/StatusBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { TrialCountdown } from '@/components/common/TrialCountdown';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { formatDate } from '@/lib/utils';
import { Building2, ExternalLink, CreditCard, Palette, ArrowLeft, Info, Clock } from 'lucide-react';
import { getTenantUrl, getTenantDisplayUrl } from '@/lib/tenant-url';

export default function InstanceDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const { data: instance, isLoading, error } = useInstance(id);
  const { data: trialStatus } = useTrialStatus(
    instance?.isOnTrial ? instance.id : undefined
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }
  if (error) return <ErrorAlert message="Không thể tải thông tin instance" />;
  if (!instance) return null;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center gap-4 mb-3">
          <Button variant="ghost" size="sm" asChild>
            <Link href="/dashboard">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Dashboard
            </Link>
          </Button>
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-primary/10 p-3 text-primary">
              <Building2 className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">{instance.organizationName}</h1>
              <p className="text-muted-foreground">
                <code className="text-sm">{getTenantDisplayUrl(instance.subdomain)}</code>
              </p>
            </div>
          </div>
          <StatusBadge status={instance.status} />
        </div>
      </div>

      {/* Content Grid */}
      <div className="grid gap-6 md:grid-cols-2">
        {/* Instance Info */}
        <Card className="shadow-soft">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <div className="rounded-lg bg-primary/10 p-2 text-primary">
                <Info className="h-4 w-4" />
              </div>
              Thông tin
            </CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="space-y-3">
              <div className="flex justify-between">
                <dt className="text-sm text-muted-foreground">Subdomain</dt>
                <dd className="text-sm font-medium">{instance.subdomain}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-sm text-muted-foreground">Email</dt>
                <dd className="text-sm font-medium">{instance.contactEmail ?? '-'}</dd>
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
          </CardContent>
        </Card>

        {/* Trial Status */}
        {trialStatus && (
          <Card className="shadow-soft">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <div className="rounded-lg bg-amber-500/10 p-2 text-amber-600">
                  <Clock className="h-4 w-4" />
                </div>
                Trial
              </CardTitle>
            </CardHeader>
            <CardContent>
              <TrialCountdown trial={trialStatus} />
              {trialStatus.daysRemaining <= 3 && !trialStatus.expired && (
                <div className="mt-4 rounded-xl bg-orange-50 dark:bg-orange-950/30 p-3 text-sm text-orange-700 dark:text-orange-300 border border-orange-200 dark:border-orange-800">
                  Trial sắp hết hạn. Nâng cấp gói để tiếp tục sử dụng.
                </div>
              )}
            </CardContent>
          </Card>
        )}
      </div>

      {/* Action Buttons */}
      <div className="flex flex-wrap gap-3">
        <Button asChild>
          <a
            href={getTenantUrl(instance.subdomain)}
            target="_blank"
            rel="noopener noreferrer"
          >
            <ExternalLink className="mr-2 h-4 w-4" />
            Truy cập KiteClass
          </a>
        </Button>
        <Button variant="outline" asChild>
          <Link href="/billing">
            <CreditCard className="mr-2 h-4 w-4" />
            Nâng cấp gói
          </Link>
        </Button>
        <Button variant="outline" asChild>
          <Link href="/branding">
            <Palette className="mr-2 h-4 w-4" />
            AI Branding
          </Link>
        </Button>
      </div>
    </div>
  );
}
