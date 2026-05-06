/**
 * G12 BulkActionsBar — RTL coverage of the 5 spec'd states (default / loading /
 * empty / error / success — mapped to the 4 enum actions + the destructive-
 * confirm flow), plus cross-component re-use of D1 `<ConfirmDialog>`.
 *
 * Spec source: `ui_kits/components/G12-bulk-actions-bar/spec.md` + 5 root state
 * HTML files (`default.html`, `selecting.html`, `bulk-confirm.html`,
 * `action-running.html`, `action-done.html`).  Vietnamese-only labels per
 * CLAUDE.md.
 *
 * State-name mapping (Round 2 spec → enum-API tests):
 *   - default.html         → "renders default state (no selection)"      (idle)
 *   - selecting.html       → "renders count + 4 action buttons"          (active)
 *   - bulk-confirm.html    → "destructive Xóa shows ConfirmDialog"       (confirm)
 *   - action-running.html  → "disabled prop disables action buttons"     (loading/running)
 *   - action-done.html     → "fires onAction(DELETE) after confirm"      (success)
 *
 * The cross-component `ConfirmDialog` import below is INTENTIONAL: same smoke
 * test pattern as G10 → G6 in Wave 28 — verifies relative import works +
 * identity preserved (no copy-paste drift).
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BulkActionsBar } from '../BulkActionsBar';
import { ConfirmDialog as G12ReExport } from '../BulkActionsBar';
// Cross-component re-use proof: import the SAME D1 dialog directly.
import { ConfirmDialog as D1Direct } from '../../D1-confirm-dialog';
import type { BulkAction } from '../types';

describe('<BulkActionsBar>', () => {
  it('renders default state — count chip + 4 action buttons + region role (idle)', () => {
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={0} onAction={onAction} sticky="none" />);

    // Region landmark + aria-live
    const region = screen.getByRole('region', {
      name: /thanh thao tác hàng loạt/i,
    });
    expect(region).toHaveAttribute('aria-live', 'polite');

    // 4 action buttons rendered
    expect(screen.getByTestId('bulk-actions-bar-action-EXPORT_CSV')).toBeInTheDocument();
    expect(screen.getByTestId('bulk-actions-bar-action-ARCHIVE')).toBeInTheDocument();
    expect(screen.getByTestId('bulk-actions-bar-action-ASSIGN')).toBeInTheDocument();
    expect(screen.getByTestId('bulk-actions-bar-action-DELETE')).toBeInTheDocument();

    // Count chip shows 0
    expect(screen.getByTestId('bulk-actions-bar-count')).toHaveTextContent('Đã chọn 0');
  });

  it('renders selecting state — Đã chọn N count display (multi-select)', () => {
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={3} onAction={onAction} sticky="none" />);

    const chip = screen.getByTestId('bulk-actions-bar-count');
    expect(chip).toHaveTextContent('Đã chọn 3');
    // role="status" so SR users hear the count change.
    expect(chip).toHaveAttribute('role', 'status');
  });

  it('disabled=true disables all 4 action buttons (running state)', () => {
    const onAction = vi.fn();
    render(
      <BulkActionsBar
        selectedCount={5}
        onAction={onAction}
        disabled
        sticky="none"
      />,
    );

    expect(screen.getByTestId('bulk-actions-bar-action-EXPORT_CSV')).toBeDisabled();
    expect(screen.getByTestId('bulk-actions-bar-action-ARCHIVE')).toBeDisabled();
    expect(screen.getByTestId('bulk-actions-bar-action-ASSIGN')).toBeDisabled();
    expect(screen.getByTestId('bulk-actions-bar-action-DELETE')).toBeDisabled();
  });

  it('selectedCount=0 disables all action buttons even when not explicitly disabled (empty)', () => {
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={0} onAction={onAction} sticky="none" />);

    expect(screen.getByTestId('bulk-actions-bar-action-EXPORT_CSV')).toBeDisabled();
    expect(screen.getByTestId('bulk-actions-bar-action-DELETE')).toBeDisabled();
  });

  it('renders Vietnamese labels per agent prompt: Xuất CSV / Lưu trữ / Phân lớp / Xóa', () => {
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={2} onAction={onAction} sticky="none" />);

    expect(
      screen.getByTestId('bulk-actions-bar-action-EXPORT_CSV'),
    ).toHaveTextContent('Xuất CSV');
    expect(
      screen.getByTestId('bulk-actions-bar-action-ARCHIVE'),
    ).toHaveTextContent('Lưu trữ');
    expect(
      screen.getByTestId('bulk-actions-bar-action-ASSIGN'),
    ).toHaveTextContent('Phân lớp');
    expect(screen.getByTestId('bulk-actions-bar-action-DELETE')).toHaveTextContent(
      'Xóa',
    );
  });

  it('clicking Xuất CSV fires onAction(EXPORT_CSV) immediately (no confirm)', async () => {
    const user = userEvent.setup();
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={3} onAction={onAction} sticky="none" />);

    await user.click(screen.getByTestId('bulk-actions-bar-action-EXPORT_CSV'));
    expect(onAction).toHaveBeenCalledWith<[BulkAction]>('EXPORT_CSV');
    expect(onAction).toHaveBeenCalledTimes(1);
  });

  it('clicking Lưu trữ fires onAction(ARCHIVE); Phân lớp fires onAction(ASSIGN)', async () => {
    const user = userEvent.setup();
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={3} onAction={onAction} sticky="none" />);

    await user.click(screen.getByTestId('bulk-actions-bar-action-ARCHIVE'));
    expect(onAction).toHaveBeenLastCalledWith<[BulkAction]>('ARCHIVE');

    await user.click(screen.getByTestId('bulk-actions-bar-action-ASSIGN'));
    expect(onAction).toHaveBeenLastCalledWith<[BulkAction]>('ASSIGN');

    expect(onAction).toHaveBeenCalledTimes(2);
  });

  it('destructive Xóa opens ConfirmDialog FIRST and does NOT fire onAction(DELETE) yet (bulk-confirm state)', async () => {
    const user = userEvent.setup();
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={5} onAction={onAction} sticky="none" />);

    // No alertdialog rendered before click.
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();

    await user.click(screen.getByTestId('bulk-actions-bar-action-DELETE'));

    // Confirm dialog now visible with VN copy.
    const dialog = screen.getByRole('alertdialog');
    expect(dialog).toBeInTheDocument();
    expect(
      screen.getByText(/xác nhận xóa hàng loạt/i),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /xác nhận xóa/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^hủy$/i })).toBeInTheDocument();

    // onAction must NOT have fired yet.
    expect(onAction).not.toHaveBeenCalled();
  });

  it('confirming Xóa fires onAction(DELETE) and closes the dialog (action-done success state)', async () => {
    const user = userEvent.setup();
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={5} onAction={onAction} sticky="none" />);

    await user.click(screen.getByTestId('bulk-actions-bar-action-DELETE'));
    await user.click(screen.getByRole('button', { name: /xác nhận xóa/i }));

    expect(onAction).toHaveBeenCalledWith<[BulkAction]>('DELETE');
    expect(onAction).toHaveBeenCalledTimes(1);
    // Dialog auto-closed by D1's onConfirm wrapper.
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });

  it('cancelling the destructive confirm does NOT fire onAction(DELETE)', async () => {
    const user = userEvent.setup();
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={5} onAction={onAction} sticky="none" />);

    await user.click(screen.getByTestId('bulk-actions-bar-action-DELETE'));
    await user.click(screen.getByRole('button', { name: /^hủy$/i }));

    expect(onAction).not.toHaveBeenCalled();
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });

  it('sticky="bottom" applies sticky-bottom positioning class on root', () => {
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={3} onAction={onAction} sticky="bottom" />);

    const root = screen.getByTestId('bulk-actions-bar-root');
    expect(root).toHaveAttribute('data-sticky', 'bottom');
    expect(root.className).toContain('sticky-bottom');
  });

  it('sticky="top" applies sticky-top positioning class on root', () => {
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={3} onAction={onAction} sticky="top" />);

    const root = screen.getByTestId('bulk-actions-bar-root');
    expect(root).toHaveAttribute('data-sticky', 'top');
    expect(root.className).toContain('sticky-top');
  });

  it('cross-component ConfirmDialog re-use: G12 ConfirmDialog === D1 ConfirmDialog', () => {
    // Sanity: the helper imported from D1 is itself functional (renders an
    // alertdialog when open).
    const noop = (): void => {};
    render(
      <D1Direct
        open
        onOpenChange={noop}
        onConfirm={noop}
        title="t"
        description="d"
      />,
    );
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();

    // Identity: the dialog re-exported under G12 (BulkActionsBar module) must
    // be the SAME function — proves the module-internal re-use works and
    // there's no copy-paste re-implementation drift.
    expect(G12ReExport).toBe(D1Direct);
  });

  it('uses lang="vi" by default on the wrapper', () => {
    const onAction = vi.fn();
    render(<BulkActionsBar selectedCount={3} onAction={onAction} sticky="none" />);
    const root = screen.getByTestId('bulk-actions-bar-root');
    expect(root).toHaveAttribute('lang', 'vi');
  });

  it('renders the optional clear-selection X when onClearSelection is provided', async () => {
    const user = userEvent.setup();
    const onAction = vi.fn();
    const onClearSelection = vi.fn();
    render(
      <BulkActionsBar
        selectedCount={3}
        onAction={onAction}
        onClearSelection={onClearSelection}
        sticky="none"
      />,
    );

    const clear = screen.getByTestId('bulk-actions-bar-clear');
    expect(clear).toHaveAttribute('aria-label', 'Bỏ chọn tất cả');

    await user.click(clear);
    expect(onClearSelection).toHaveBeenCalledTimes(1);
  });
});
