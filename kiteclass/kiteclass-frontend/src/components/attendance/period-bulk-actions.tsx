/**
 * PeriodBulkActions — bulk-action toolbar for the K-12 per-tiết tap-grid.
 *
 * Three actions:
 * 1. "Đánh dấu tất cả có mặt" — set every student to PRESENT (most common
 *    starting point per AC-OPS-001 ≤2-min target).
 * 2. "Xoá lựa chọn" — reset every student back to the unset state so the
 *    teacher can start over (e.g. wrong period selected).
 * 3. "Lưu" — submit the current local state via the upsert mutation.
 *
 * The component is purely presentational — it emits callbacks; the route
 * shell owns the local optimistic state and the mutation.
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

'use client';

import * as React from 'react';
import { Button } from '@/components/ui/button';

export interface PeriodBulkActionsProps {
  onMarkAllPresent: () => void;
  onReset: () => void;
  onSave: () => void;
  /** True while the upsert mutation is in flight. */
  isSaving?: boolean;
  /** Disables Save when the local state has nothing to save. */
  isSaveDisabled?: boolean;
}

export function PeriodBulkActions({
  onMarkAllPresent,
  onReset,
  onSave,
  isSaving = false,
  isSaveDisabled = false,
}: PeriodBulkActionsProps) {
  return (
    <div
      data-testid="period-bulk-actions"
      className="flex flex-wrap items-center gap-2 rounded-md border bg-card p-2 shadow-sm sm:p-3"
    >
      <Button
        type="button"
        variant="secondary"
        size="sm"
        onClick={onMarkAllPresent}
        disabled={isSaving}
      >
        Đánh dấu tất cả có mặt
      </Button>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        onClick={onReset}
        disabled={isSaving}
      >
        Xoá lựa chọn
      </Button>

      <div className="ml-auto">
        <Button
          type="button"
          size="sm"
          onClick={onSave}
          disabled={isSaving || isSaveDisabled}
          aria-label={isSaving ? 'Đang lưu điểm danh' : 'Lưu điểm danh'}
        >
          {isSaving ? 'Đang lưu…' : 'Lưu'}
        </Button>
      </div>
    </div>
  );
}
