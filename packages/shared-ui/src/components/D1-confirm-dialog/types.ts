/**
 * Type definitions for ConfirmDialog — D1 component (Wave 28 Bucket E).
 *
 * Design source: documents/02-architecture/design-system/dossier/04-component-gaps.md §D1
 * Mirrors API surface of existing kiteclass-frontend implementation:
 *   kiteclass/kiteclass-frontend/src/components/ui/confirm-dialog.tsx
 *
 * Vietnamese-first per CLAUDE.md. Default labels:
 *   confirmText = 'Xác nhận'
 *   cancelText  = 'Hủy'
 *
 * Accessibility:
 *   - Built on `@radix-ui/react-dialog` Primitives → role="alertdialog"
 *     (Radix Dialog Content default semantics applied via aria-describedby).
 *   - Focus trap inside modal (Radix native) — first focusable receives focus
 *     on open; Escape + click-outside trigger close (controlled via onOpenChange).
 *   - Vietnamese labels are sr-only friendly; visible text mirrors button copy.
 */

/**
 * Visual variant of the confirm button.
 *
 * - `default`     — neutral primary action (e.g., "Xác nhận lưu thay đổi").
 * - `destructive` — red emphasis for irreversible actions (e.g., "Xóa lớp học").
 */
export type ConfirmDialogVariant = 'default' | 'destructive';

/**
 * Public props for `<ConfirmDialog>`.
 *
 * The component is **controlled** — parent owns `open` state and provides the
 * `onOpenChange` setter. The `onConfirm` callback fires when user clicks the
 * confirm button; the dialog auto-closes after the callback (calls
 * `onOpenChange(false)` internally).
 *
 * Drop-in replacement for `kiteclass-frontend/src/components/ui/confirm-dialog.tsx`
 * — props match exactly so callsites can swap import path with no other change.
 */
export interface ConfirmDialogProps {
  /** Controlled open state. */
  open: boolean;
  /** Called whenever the dialog wants to change open state (Esc, overlay click, cancel). */
  onOpenChange: (open: boolean) => void;
  /** Called when user clicks the confirm button. Dialog auto-closes after. */
  onConfirm: () => void;
  /** Heading text. Required. */
  title: string;
  /** Body description. Required. */
  description: string;
  /** Confirm button label. Default: 'Xác nhận'. */
  confirmText?: string;
  /** Cancel button label. Default: 'Hủy'. */
  cancelText?: string;
  /** Visual variant for confirm button. Default: 'default'. */
  variant?: ConfirmDialogVariant;
}
