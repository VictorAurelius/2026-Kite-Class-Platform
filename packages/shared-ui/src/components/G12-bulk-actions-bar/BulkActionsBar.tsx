'use client';

/**
 * G12 — Sticky Multi-Select Bulk Actions Bar.
 *
 * Replaces the missing bulk-action affordance in production tables (KC
 * `/students`, KC `/teachers`, KH `/admin/instances`, KH `/admin/payments`)
 * per `dossier/04-component-gaps.md` §G12 + `ui_kits/components/G12-bulk-actions-bar/`
 * 5 root state HTML files (Round 2 layout — `default.html`, `selecting.html`,
 * `bulk-confirm.html`, `action-running.html`, `action-done.html`).
 *
 * Cross-component re-use:
 *   The destructive `Xóa` action consumes the D1 `<ConfirmDialog>` shipped
 *   in Wave 28 Bucket E (PR #859) — first time a G* component composes a
 *   D* dialog inside `@kite/shared-ui`.  Imported via relative path (same
 *   reasoning as G10 → G6 in Wave 28 Bucket D: avoid circular `@kite/shared-ui`
 *   self-import + preserve identity for future refactor audits).
 *
 * Vietnamese formatting:
 *   - Count chip copy: `Đã chọn N` (per agent prompt; the HTML proto uses
 *     `N mục đã chọn` — agent prompt is the binding contract for this PR).
 *   - Action labels (`Xuất CSV` / `Lưu trữ` / `Phân lớp` / `Xóa`) per the
 *     agent prompt.  The HTML proto uses `Xuất Excel` + `Chuyển lớp`; the
 *     enum-API approach lets either copy ship via `ACTION_LABELS` if a host
 *     app overrides later.
 *
 * Accessibility (WCAG AA, contrast measurements documented in
 * `ui_kits/.../selecting.html` HTML proto comments):
 *   - Bar wrapper carries `role="region"` + `aria-label="Thanh thao tác hàng loạt"`
 *     + `aria-live="polite"` so screen readers announce selection-count
 *     changes.
 *   - Count chip carries `role="status"` so SR users hear `Đã chọn 3` on
 *     selection change.
 *   - All action buttons have visible focus rings (Tailwind `focus-visible:`
 *     classes).
 *   - Destructive `Xóa` button uses red contrast ≥4.5:1 verified in proto.
 *   - The clear-selection X icon button has `aria-label="Bỏ chọn tất cả"`.
 *
 * State machine: presentational only (per `ai-branding-guidelines.md` §10
 * doesn't apply — this is a leaf UI control, not a domain entity).  The
 * confirm/running/done states from `spec.md` are owned by the parent page
 * (which mounts/unmounts the bar + invokes `onAction` after server work).
 *
 * No new deps — re-uses `@radix-ui/react-dialog` already a peer dep through
 * D1.
 */

import * as React from 'react';
import { ConfirmDialog } from '../D1-confirm-dialog';
import type {
  BulkAction,
  BulkActionsBarProps,
  BulkActionsBarSticky,
} from './types';

// Re-export D1's ConfirmDialog so consumer tests + downstream code can verify
// the cross-component identity (`G12.ConfirmDialog === D1.ConfirmDialog`).
// Same pattern as G10 → G6 `formatVNCurrency` in Wave 28 Bucket D.
export { ConfirmDialog } from '../D1-confirm-dialog';

const COPY_VI = {
  selectedPrefix: 'Đã chọn',
  clearSelection: 'Bỏ chọn tất cả',
  regionLabel: 'Thanh thao tác hàng loạt',
  confirm: {
    title: 'Xác nhận xóa hàng loạt',
    description:
      'Bạn có chắc chắn muốn xóa các mục đã chọn? Hành động này không thể hoàn tác.',
    confirmText: 'Xác nhận xóa',
    cancelText: 'Hủy',
  },
} as const;

/** VN action labels per agent prompt (Wave 29 Bucket D). */
const ACTION_LABELS: Record<BulkAction, string> = {
  EXPORT_CSV: 'Xuất CSV',
  ARCHIVE: 'Lưu trữ',
  ASSIGN: 'Phân lớp',
  DELETE: 'Xóa',
};

/**
 * Glyph for each action — kept inline (no `lucide-react` dep) so the lib
 * remains zero-runtime aside from React + Radix Dialog.  Each glyph is
 * `aria-hidden` decorative; meaning conveyed via the button's text label.
 */
const ACTION_ICON: Record<BulkAction, string> = {
  EXPORT_CSV: '⬇',
  ARCHIVE: '📦',
  ASSIGN: '👥',
  DELETE: '🗑',
};

/** Whether this action is destructive (drives the red color + confirm flow). */
const ACTION_VARIANT: Record<BulkAction, 'default' | 'destructive'> = {
  EXPORT_CSV: 'default',
  ARCHIVE: 'default',
  ASSIGN: 'default',
  DELETE: 'destructive',
};

const BUTTON_BASE =
  'inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50';

const BUTTON_VARIANT: Record<'default' | 'destructive', string> = {
  default: `${BUTTON_BASE} border bg-card hover:bg-muted`,
  destructive: `${BUTTON_BASE} border border-destructive/30 bg-destructive/5 text-destructive hover:bg-destructive/10`,
};

function stickyClasses(sticky: BulkActionsBarSticky): string {
  switch (sticky) {
    case 'top':
      return 'sticky-top fixed top-4 inset-x-4 md:inset-x-auto md:left-1/2 md:-translate-x-1/2 z-40 max-w-3xl mx-auto';
    case 'bottom':
      return 'sticky-bottom fixed bottom-4 inset-x-4 md:inset-x-auto md:left-1/2 md:-translate-x-1/2 z-40 max-w-3xl mx-auto';
    case 'none':
      return '';
  }
}

/** Order action buttons render in — destructive last (UX best practice). */
const ACTION_ORDER: readonly BulkAction[] = [
  'EXPORT_CSV',
  'ARCHIVE',
  'ASSIGN',
  'DELETE',
] as const;

export function BulkActionsBar(
  props: BulkActionsBarProps,
): React.JSX.Element {
  const {
    selectedCount,
    onAction,
    sticky = 'bottom',
    disabled = false,
    onClearSelection,
    lang = 'vi',
  } = props;

  const [confirmOpen, setConfirmOpen] = React.useState(false);

  const handleClick = React.useCallback(
    (action: BulkAction) => {
      // Destructive: open confirm dialog first; only call `onAction` AFTER
      // the user confirms.  Non-destructive: fire immediately.
      if (ACTION_VARIANT[action] === 'destructive') {
        setConfirmOpen(true);
        return;
      }
      onAction(action);
    },
    [onAction],
  );

  const handleConfirmDelete = React.useCallback(() => {
    onAction('DELETE');
  }, [onAction]);

  const wrapperClassName = [
    stickyClasses(sticky),
    'rounded-2xl border bg-card/95 backdrop-blur shadow-lg p-3 flex flex-wrap items-center gap-2',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <>
      <div
        data-testid="bulk-actions-bar-root"
        data-sticky={sticky}
        lang={lang}
        role="region"
        aria-label={COPY_VI.regionLabel}
        aria-live="polite"
        className={wrapperClassName}
      >
        <span
          role="status"
          data-testid="bulk-actions-bar-count"
          className="inline-flex items-center gap-2 rounded-lg bg-primary/10 text-primary px-3 py-1.5 text-sm font-semibold shrink-0"
        >
          <span className="grid h-6 min-w-6 place-items-center rounded-full bg-primary px-1.5 text-xs text-primary-foreground tabular-nums">
            {selectedCount}
          </span>
          <span>{`${COPY_VI.selectedPrefix} ${selectedCount}`}</span>
        </span>

        <div className="hidden md:block h-6 w-px bg-border" aria-hidden="true" />

        <div className="flex flex-wrap gap-1.5 flex-1 min-w-0">
          {ACTION_ORDER.map((action) => {
            const variant = ACTION_VARIANT[action];
            return (
              <button
                key={action}
                type="button"
                data-testid={`bulk-actions-bar-action-${action}`}
                data-variant={variant}
                disabled={disabled || selectedCount === 0}
                onClick={() => handleClick(action)}
                className={BUTTON_VARIANT[variant]}
              >
                <span aria-hidden="true">{ACTION_ICON[action]}</span>
                <span>{ACTION_LABELS[action]}</span>
              </button>
            );
          })}
        </div>

        {onClearSelection && (
          <>
            <div
              className="hidden md:block h-6 w-px bg-border"
              aria-hidden="true"
            />
            <button
              type="button"
              data-testid="bulk-actions-bar-clear"
              aria-label={COPY_VI.clearSelection}
              onClick={onClearSelection}
              className="rounded-lg p-2 hover:bg-muted text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <span aria-hidden="true">×</span>
            </button>
          </>
        )}
      </div>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        onConfirm={handleConfirmDelete}
        title={COPY_VI.confirm.title}
        description={COPY_VI.confirm.description}
        confirmText={COPY_VI.confirm.confirmText}
        cancelText={COPY_VI.confirm.cancelText}
        variant="destructive"
      />
    </>
  );
}
