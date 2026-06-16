/**
 * Branding version history + rollback (GAP-1446).
 *
 * Surfaces the BE BrandingVersionController (GET versions + POST rollback) that
 * previously had no FE affordance — the KC-10 branding flow only exposed
 * name/colors/logo. Lists snapshots (newest first) with an active badge and a
 * confirm-gated rollback action.
 *
 * @author KiteClass Team
 * @since GAP-1446
 */

'use client';

import { useState } from 'react';
import { History, RotateCcw } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useBranding, useBrandingVersions, useRollbackBranding } from '@/hooks/use-branding';
import { useAuthStore } from '@/stores/auth-store';
import { formatDateTime } from '@/lib/utils';

export function BrandingVersionHistory() {
  const instanceId = useAuthStore((state) => state.tenantId);
  const { data: page, isLoading, isError } = useBrandingVersions(instanceId);
  const rollbackMutation = useRollbackBranding(instanceId);
  // Re-apply brand CSS vars after rollback so the restored palette shows live (GAP-807 pattern).
  const { refetch: refetchBranding } = useBranding();

  // Confirm dialog state — which version number is pending rollback.
  const [pendingVersion, setPendingVersion] = useState<number | null>(null);

  const versions = page?.content ?? [];

  const handleConfirmRollback = () => {
    if (pendingVersion == null) return;
    rollbackMutation.mutate(pendingVersion, {
      onSuccess: () => {
        void refetchBranding();
      },
    });
    setPendingVersion(null);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <History className="h-5 w-5" />
          Lịch sử phiên bản
        </CardTitle>
        <CardDescription>
          Xem các phiên bản branding trước đây và khôi phục về một phiên bản bất kỳ
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading && (
          <div className="space-y-2">
            <div className="h-12 animate-pulse rounded bg-muted" />
            <div className="h-12 animate-pulse rounded bg-muted" />
          </div>
        )}

        {isError && (
          <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive">
            Không thể tải lịch sử phiên bản. Vui lòng thử lại.
          </div>
        )}

        {!isLoading && !isError && versions.length === 0 && (
          <p className="text-sm text-muted-foreground">
            Chưa có phiên bản nào. Mỗi lần cập nhật branding sẽ tạo một phiên bản mới.
          </p>
        )}

        {!isLoading && !isError && versions.length > 0 && (
          <ul className="divide-y rounded-md border">
            {versions.map((version) => (
              <li
                key={version.id}
                className="flex items-center justify-between gap-4 p-3"
                data-testid="branding-version-row"
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-medium">Phiên bản {version.versionNumber}</span>
                    {version.active && <Badge variant="default">Đang dùng</Badge>}
                    {version.rollbackOf != null && (
                      <Badge variant="secondary">Khôi phục từ v{version.rollbackOf}</Badge>
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {formatDateTime(version.createdAt)}
                  </p>
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  disabled={version.active || rollbackMutation.isPending}
                  onClick={() => setPendingVersion(version.versionNumber)}
                >
                  <RotateCcw className="mr-2 h-4 w-4" />
                  Khôi phục
                </Button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>

      <ConfirmDialog
        open={pendingVersion != null}
        onOpenChange={(open) => {
          if (!open) setPendingVersion(null);
        }}
        onConfirm={handleConfirmRollback}
        title="Khôi phục phiên bản branding"
        description={
          pendingVersion != null
            ? `Khôi phục branding về phiên bản ${pendingVersion}? Thao tác này tạo một phiên bản mới với nội dung của phiên bản đã chọn.`
            : ''
        }
        confirmText="Khôi phục"
        cancelText="Hủy"
      />
    </Card>
  );
}
