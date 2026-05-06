/**
 * Type definitions for G12 Bulk Actions Bar.
 *
 * Mirrors `ui_kits/components/G12-bulk-actions-bar/spec.md` + 5 root state HTML
 * files (`default.html`, `selecting.html`, `bulk-confirm.html`,
 * `action-running.html`, `action-done.html`) and `dossier/04-component-gaps.md`
 * §G12.
 *
 * Two abstractions live here:
 *  - `BulkAction` — the closed enum of supported bulk actions (4 per Wave 29
 *    Bucket D scope: EXPORT_CSV, ARCHIVE, ASSIGN, DELETE).  The HTML proto's
 *    open `actions: Array<{id,label,icon,variant}>` shape was deliberately
 *    narrowed to a closed enum here so callsites get TS exhaustiveness +
 *    centralised i18n + uniform destructive-confirm wiring.
 *  - `BulkActionsBarProps` — controlled component props.  The bar is
 *    presentational; the parent owns the selection state, `onAction` callback,
 *    and decides what tier of permission gates each action.
 */

/**
 * The closed set of supported bulk actions.
 *
 * Extending: add a new variant here, then handle it in `ACTION_LABELS` +
 * `ACTION_ICON` + `ACTION_VARIANT` inside `BulkActionsBar.tsx`. Tests will fail
 * fast if any of those tables miss the new entry (TS exhaustiveness).
 *
 * Naming convention: SCREAMING_SNAKE so the enum reads like a backend command.
 */
export type BulkAction = 'EXPORT_CSV' | 'ARCHIVE' | 'ASSIGN' | 'DELETE';

/**
 * Number of selected items rendered in the count chip.
 *
 * Re-exported alias kept for callsites that want to be explicit ("how many is
 * `5`?").  Always a non-negative integer; passing `0` hides the bar
 * (sticky="none" effective render).
 */
export type SelectedCount = number;

/**
 * Sticky positioning for the bar wrapper.
 *
 * - `'bottom'` (default) — fixed to viewport bottom, slides up on appear.
 *   Matches `ui_kits/.../selecting.html`.
 * - `'top'` — fixed to viewport top.  Useful for admin tables where bottom
 *   sheet collides with footer pagination.
 * - `'none'` — render inline, no sticky positioning.  Used by tests + when
 *   the consuming page handles its own positioning.
 */
export type BulkActionsBarSticky = 'top' | 'bottom' | 'none';

export interface BulkActionsBarProps {
  /**
   * Number of items currently selected.  When `0`, the bar still renders
   * (so `aria-live="polite"` announcements work) but the action buttons are
   * disabled and the count chip shows `0`.  Hide the bar at the call-site
   * (conditional render on `selectedCount > 0`) if you want the slide-out
   * behaviour from the HTML proto.
   */
  selectedCount: SelectedCount;

  /**
   * Fires when an action button is clicked.  For `DELETE` (destructive),
   * the bar shows a `<ConfirmDialog>` (D1) FIRST and only invokes
   * `onAction('DELETE')` after the user confirms — so the parent never
   * sees a `DELETE` callback that hasn't been confirmed.
   */
  onAction: (action: BulkAction) => void;

  /**
   * Sticky positioning. Default `'bottom'` — matches the HTML proto.
   *
   * Pass `'none'` in tests + storybook so RTL queries don't fight position:fixed.
   */
  sticky?: BulkActionsBarSticky;

  /**
   * When `true`, all action buttons render with `disabled` + dimmed visual
   * (e.g., during async batch ops). The clear-selection button stays enabled
   * so the user can always escape.
   *
   * Default `false`.
   */
  disabled?: boolean;

  /**
   * Optional callback fired when user clicks the trailing X to clear the
   * current selection.  Optional so simple pages that only support
   * "page-level" select-all don't need to wire it up.
   */
  onClearSelection?: () => void;

  /**
   * Override the wrapper `lang` attribute. Defaults to `'vi'`.
   * `'en'` falls back to `'vi'` for v1 (Vietnamese-first per CLAUDE.md).
   */
  lang?: 'vi' | 'en';
}
