'use client';

/**
 * ConfirmDialog — D1 component (Wave 28 Bucket E).
 *
 * Radix-native confirmation dialog for `@kite/shared-ui`. Replaces native
 * `window.confirm()` with a styled modal that respects WCAG AA, focus trap,
 * and Vietnamese-first defaults.
 *
 * Design source: documents/02-architecture/design-system/dossier/04-component-gaps.md §D1
 *
 * Drop-in replacement for kiteclass-frontend's local
 * `src/components/ui/confirm-dialog.tsx` — props match exactly so callsites
 * can swap import path with no other change. Existing local wrapper stays
 * untouched until callsites migrate (separate follow-up gap).
 *
 * Why Radix directly (not shadcn wrapper):
 *  - shadcn's `Dialog` wrapper depends on kiteclass-frontend `cn()` util +
 *    `@/components/ui/button` + `lucide-react` — none of which are present in
 *    `@kite/shared-ui`'s minimal deps. Adopting them would bloat the shared
 *    lib + couple it to kiteclass-frontend specifics.
 *  - Radix Dialog Primitives are headless + zero-runtime + already a
 *    transitive dep of kiteclass-frontend → low marginal cost as workspace
 *    peer dep.
 *  - Same WCAG semantics, focus trap, click-outside-to-close, Escape-to-close.
 *
 * Accessibility (WCAG AA):
 *  - role="alertdialog" via Radix Dialog Content default semantics.
 *  - aria-labelledby + aria-describedby auto-wired when Title + Description
 *    primitives are used.
 *  - Focus trap inside content (Radix native).
 *  - Escape + overlay click trigger onOpenChange(false).
 *  - Buttons receive visible focus ring (focus-visible classes).
 *  - Destructive variant red maintains ≥4.5:1 contrast on white text.
 *
 * Vietnamese-first per CLAUDE.md.
 */

import * as React from 'react';
import * as DialogPrimitive from '@radix-ui/react-dialog';
import type { ConfirmDialogProps, ConfirmDialogVariant } from './types';

const COPY_VI = {
  confirm: 'Xác nhận',
  cancel: 'Hủy',
};

// Tailwind class strings (consumer apps provide tokens via tailwind config).
// We avoid shadcn `cn()` util so this lib stays standalone.
const OVERLAY_CLASS =
  'fixed inset-0 z-50 bg-black/80 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0';

const CONTENT_CLASS =
  'fixed left-[50%] top-[50%] z-50 grid w-full max-w-lg translate-x-[-50%] translate-y-[-50%] gap-4 border bg-background p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 sm:rounded-lg';

const TITLE_CLASS = 'text-lg font-semibold leading-none tracking-tight';
const DESCRIPTION_CLASS = 'text-sm text-muted-foreground';
const FOOTER_CLASS = 'flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2';

const BUTTON_BASE =
  'inline-flex items-center justify-center rounded-md px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50';

// Cancel = outline-style neutral; Confirm = solid primary or destructive red.
const CANCEL_BUTTON_CLASS =
  `${BUTTON_BASE} border border-input bg-background hover:bg-accent hover:text-accent-foreground`;

const CONFIRM_BUTTON_VARIANT_CLASS: Record<ConfirmDialogVariant, string> = {
  default: `${BUTTON_BASE} bg-primary text-primary-foreground hover:bg-primary/90`,
  destructive: `${BUTTON_BASE} bg-destructive text-destructive-foreground hover:bg-destructive/90`,
};

export function ConfirmDialog(props: ConfirmDialogProps): React.JSX.Element {
  const {
    open,
    onOpenChange,
    onConfirm,
    title,
    description,
    confirmText = COPY_VI.confirm,
    cancelText = COPY_VI.cancel,
    variant = 'default',
  } = props;

  const handleConfirm = React.useCallback(() => {
    onConfirm();
    onOpenChange(false);
  }, [onConfirm, onOpenChange]);

  const handleCancel = React.useCallback(() => {
    onOpenChange(false);
  }, [onOpenChange]);

  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className={OVERLAY_CLASS} />
        <DialogPrimitive.Content
          className={CONTENT_CLASS}
          // Promote to alertdialog for screen readers (Radix Dialog default
          // role is "dialog"; alertdialog signals high priority confirm action).
          role="alertdialog"
        >
          <div className="flex flex-col space-y-1.5 text-center sm:text-left">
            <DialogPrimitive.Title className={TITLE_CLASS}>
              {title}
            </DialogPrimitive.Title>
            <DialogPrimitive.Description className={DESCRIPTION_CLASS}>
              {description}
            </DialogPrimitive.Description>
          </div>
          <div className={FOOTER_CLASS}>
            <button
              type="button"
              className={CANCEL_BUTTON_CLASS}
              onClick={handleCancel}
            >
              {cancelText}
            </button>
            <button
              type="button"
              className={CONFIRM_BUTTON_VARIANT_CLASS[variant]}
              onClick={handleConfirm}
              data-variant={variant}
            >
              {confirmText}
            </button>
          </div>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
