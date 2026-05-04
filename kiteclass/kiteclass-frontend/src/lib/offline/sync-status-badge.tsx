/**
 * Visual badge surfacing the offline-attendance queue state to GVCN.
 *
 * Renders nothing when there is no offline activity (zero counts) so the
 * happy-path UI stays uncluttered. As soon as the queue has pending or
 * failed items, the badge appears with a status-coloured pill.
 *
 * @since 4.x.x (Wave 18b3 Bucket A)
 */

'use client';

import * as React from 'react';
import { cn } from '@/lib/utils';

export interface OfflineSyncStatusBadgeProps {
  pending: number;
  failed: number;
  synced: number;
  className?: string;
  /** Optional callback wired to the user's "thử lại" button. */
  onRetry?: () => void;
}

export function OfflineSyncStatusBadge({
  pending,
  failed,
  synced,
  className,
  onRetry,
}: OfflineSyncStatusBadgeProps) {
  const hasActivity = pending > 0 || failed > 0 || synced > 0;
  if (!hasActivity) return null;

  return (
    <div
      role="status"
      data-testid="offline-sync-status-badge"
      className={cn(
        'flex flex-wrap items-center gap-2 rounded-md border px-3 py-2 text-sm',
        failed > 0
          ? 'border-destructive/40 bg-destructive/10 text-destructive'
          : pending > 0
            ? 'border-amber-400/40 bg-amber-50 text-amber-900'
            : 'border-emerald-400/40 bg-emerald-50 text-emerald-900',
        className,
      )}
    >
      {pending > 0 && (
        <span data-testid="offline-pending">
          🟡 {pending} đang chờ đồng bộ
        </span>
      )}
      {failed > 0 && (
        <span data-testid="offline-failed">🔴 {failed} thất bại</span>
      )}
      {synced > 0 && pending === 0 && failed === 0 && (
        <span data-testid="offline-synced">🟢 {synced} đã đồng bộ</span>
      )}
      {failed > 0 && onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="ml-auto rounded-sm border border-destructive/40 bg-white px-2 py-0.5 text-xs hover:bg-destructive/10"
        >
          Thử lại
        </button>
      )}
    </div>
  );
}
