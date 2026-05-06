'use client';

import Link from 'next/link';
import { useAuthStore } from '@/stores/auth-store';
import { useOwnerInstances } from '@/hooks/use-instances';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorAlert } from '@/components/common/ErrorAlert';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Server, ExternalLink, Building2 } from 'lucide-react';
import { getTenantDisplayUrl } from '@/lib/tenant-url';
import { formatDate } from '@/lib/utils';
import { InstanceLifecycleStatus } from '@kite/shared-ui';
import { mockLifecycleStateFor } from './_lifecycle-mock';

/**
 * KH pro v2 instances list — Wave 31 Bucket D.
 *
 * Spec: `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/instance-*.html`
 * Each row exposes the owner's instance with a compact G9
 * InstanceLifecycleStatus pill so the lifecycle state of every instance is
 * visible at a glance (per Wave 29 G9 contract).
 */
export default function InstancesPage() {
  const user = useAuthStore((state) => state.user);
  const { data: instances, isLoading, error } = useOwnerInstances(user?.id);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return <ErrorAlert message="Không thể tải danh sách phiên bản. Vui lòng thử lại." />;
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="rounded-2xl bg-gradient-to-r from-primary/10 via-primary/5 to-accent/10 border p-6">
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-primary/10 p-3 text-primary">
            <Server className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Phiên bản</h1>
            <p className="text-muted-foreground">
              Quản lý vòng đời các phiên bản KiteClass của bạn
            </p>
          </div>
        </div>
      </div>

      {/* Empty state */}
      {(!instances || instances.length === 0) && (
        <Card className="shadow-soft">
          <CardContent className="flex flex-col items-center justify-center py-12 text-center">
            <Server className="h-12 w-12 text-muted-foreground mb-3" />
            <h2 className="text-lg font-semibold">Chưa có phiên bản nào</h2>
            <p className="text-muted-foreground mt-1 max-w-md">
              Tạo phiên bản mới để bắt đầu sử dụng KiteClass cho trung tâm của bạn.
            </p>
          </CardContent>
        </Card>
      )}

      {/* Instances list */}
      {instances && instances.length > 0 && (
        <div className="space-y-3">
          {instances.map((instance) => {
            const lifecycleState = mockLifecycleStateFor(instance.id);
            return (
              <Card
                key={instance.id}
                className="shadow-soft hover:shadow-md transition-shadow"
                data-testid={`instance-row-${instance.id}`}
              >
                <CardContent className="p-5">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div className="flex items-start gap-3 min-w-0">
                      <div className="rounded-xl bg-primary/10 p-3 text-primary shrink-0">
                        <Building2 className="h-5 w-5" />
                      </div>
                      <div className="min-w-0">
                        <h2 className="text-lg font-semibold truncate">
                          {instance.organizationName}
                        </h2>
                        <p className="text-sm text-muted-foreground truncate">
                          <code>{getTenantDisplayUrl(instance.subdomain)}</code>
                        </p>
                        <div className="mt-1 text-xs text-muted-foreground">
                          Gói {instance.tier} · Tạo {formatDate(instance.createdAt)}
                        </div>
                      </div>
                    </div>

                    {/* G9 InstanceLifecycleStatus — compact mode (no events
                        timeline on list view; just the state pill). */}
                    <div className="shrink-0" data-testid={`g9-pill-${instance.id}`}>
                      <InstanceLifecycleStatus
                        instanceId={instance.id}
                        instanceName={instance.organizationName}
                        state={lifecycleState}
                        events={[]}
                        liveUrl={
                          lifecycleState === 'DEPLOYED'
                            ? getTenantDisplayUrl(instance.subdomain)
                            : undefined
                        }
                      />
                    </div>
                  </div>

                  <div className="mt-4 flex flex-wrap gap-2">
                    <Button asChild size="sm" variant="outline">
                      <Link href={`/instances/${instance.id}`}>
                        Xem chi tiết
                      </Link>
                    </Button>
                    {lifecycleState === 'DEPLOYED' && (
                      <Button asChild size="sm" variant="ghost">
                        <a
                          href={`https://${getTenantDisplayUrl(instance.subdomain)}`}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          <ExternalLink className="mr-1.5 h-3.5 w-3.5" />
                          Mở KiteClass
                        </a>
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
