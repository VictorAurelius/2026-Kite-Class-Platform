/**
 * ConfirmDialog component tests — Wave 28 Bucket E (D1).
 *
 * Coverage (≥6 tests):
 *  1. Closed state — dialog NOT in DOM when open=false
 *  2. Open state — dialog IS in DOM with title + description + 2 buttons
 *  3. onConfirm fires + dialog auto-closes via onOpenChange(false)
 *  4. Cancel click triggers onOpenChange(false) without firing onConfirm
 *  5. Default Vietnamese labels — 'Xác nhận' + 'Hủy'
 *  6. Custom labels override defaults
 *  7. Destructive variant applies destructive styling class to confirm btn
 *  8. role="alertdialog" semantics from Radix Dialog
 *  9. Focus trap smoke test — focus lands inside dialog on open
 * 10. Escape key triggers onOpenChange(false) (Radix native)
 *
 * Mirrors existing kiteclass-frontend `confirm-dialog.tsx` behaviour 1:1 for
 * drop-in replacement when callsites migrate.
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfirmDialog } from '../ConfirmDialog';
import type { ConfirmDialogProps } from '../types';

const baseProps = (
  override: Partial<ConfirmDialogProps> = {},
): ConfirmDialogProps => ({
  open: true,
  onOpenChange: vi.fn(),
  onConfirm: vi.fn(),
  title: 'Xác nhận xóa',
  description: 'Hành động này không thể hoàn tác.',
  ...override,
});

describe('<ConfirmDialog>', () => {
  it('1. closed state — dialog content NOT rendered when open=false', () => {
    render(<ConfirmDialog {...baseProps({ open: false })} />);
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    expect(screen.queryByText(/xác nhận xóa/i)).not.toBeInTheDocument();
  });

  it('2. open state — renders title, description, and both buttons', () => {
    render(<ConfirmDialog {...baseProps()} />);
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    expect(screen.getByText('Xác nhận xóa')).toBeInTheDocument();
    expect(
      screen.getByText('Hành động này không thể hoàn tác.'),
    ).toBeInTheDocument();
    // Two action buttons
    expect(screen.getByRole('button', { name: /^xác nhận$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^hủy$/i })).toBeInTheDocument();
  });

  it('3. onConfirm fires + dialog auto-closes via onOpenChange(false)', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const onOpenChange = vi.fn();
    render(
      <ConfirmDialog
        {...baseProps({ onConfirm, onOpenChange })}
      />,
    );
    await user.click(screen.getByRole('button', { name: /^xác nhận$/i }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
    // Dialog auto-closes — onOpenChange called with false
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('4. cancel click triggers onOpenChange(false) without onConfirm', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const onOpenChange = vi.fn();
    render(
      <ConfirmDialog
        {...baseProps({ onConfirm, onOpenChange })}
      />,
    );
    await user.click(screen.getByRole('button', { name: /^hủy$/i }));
    expect(onConfirm).not.toHaveBeenCalled();
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('5. default Vietnamese labels — "Xác nhận" + "Hủy"', () => {
    render(<ConfirmDialog {...baseProps()} />);
    expect(screen.getByRole('button', { name: 'Xác nhận' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Hủy' })).toBeInTheDocument();
  });

  it('6. custom labels override defaults', () => {
    render(
      <ConfirmDialog
        {...baseProps({ confirmText: 'Đồng ý xóa', cancelText: 'Quay lại' })}
      />,
    );
    expect(screen.getByRole('button', { name: 'Đồng ý xóa' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Quay lại' })).toBeInTheDocument();
    // Defaults must NOT be present when overridden
    expect(screen.queryByRole('button', { name: 'Xác nhận' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Hủy' })).not.toBeInTheDocument();
  });

  it('7. destructive variant applies destructive class to confirm button', () => {
    render(<ConfirmDialog {...baseProps({ variant: 'destructive' })} />);
    const confirmBtn = screen.getByRole('button', { name: /^xác nhận$/i });
    // Destructive variant uses red palette — token-based class includes "destructive"
    // or rose/red. We assert via `data-variant` attr exposed for testability.
    expect(confirmBtn).toHaveAttribute('data-variant', 'destructive');
  });

  it('8. role="alertdialog" semantics — high-priority modal', () => {
    render(<ConfirmDialog {...baseProps()} />);
    const dialog = screen.getByRole('alertdialog');
    expect(dialog).toBeInTheDocument();
    // Radix wires aria-labelledby + aria-describedby automatically when Title +
    // Description primitives are used.
    expect(dialog).toHaveAttribute('aria-labelledby');
    expect(dialog).toHaveAttribute('aria-describedby');
  });

  it('9. focus trap smoke test — focus lands inside dialog on open', async () => {
    render(<ConfirmDialog {...baseProps()} />);
    const dialog = screen.getByRole('alertdialog');
    // Radix moves focus to the first focusable element inside Content on open.
    // We assert active element is inside the dialog (focus trap entry working).
    await new Promise((r) => setTimeout(r, 0));
    expect(dialog.contains(document.activeElement)).toBe(true);
  });

  it('10. Escape key triggers onOpenChange(false) — Radix native', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<ConfirmDialog {...baseProps({ onOpenChange })} />);
    await user.keyboard('{Escape}');
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });
});
