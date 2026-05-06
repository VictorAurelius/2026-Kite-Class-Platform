/**
 * Wave 30 Bucket C tests — Phase 4 KC pro v2 port (GAP-266).
 *
 * Validates @kite/shared-ui consumption pattern for the dashboard scope:
 *   - StudentsPage renders the BulkActionsBar (G12) when rows are selected
 *   - Selecting rows updates the count chip (`Đã chọn N`)
 *   - Non-destructive bulk action callback fires via `onAction`
 *   - Destructive `Xóa` action triggers the D1 ConfirmDialog (consumed
 *     internally by BulkActionsBar) — once user confirms, `onAction('DELETE')`
 *     fires and the selection clears.
 *   - StudentsPage list renders with mocked data.
 *
 * @since 2026-05-06
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, within } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import StudentsPage from '../page';
import NewStudentPage from '../new/page';

beforeEach(() => {
  // Suppress info log emitted by non-destructive bulk action stubs.
  vi.spyOn(console, 'info').mockImplementation(() => {});
});

describe('Wave 30 Bucket C — StudentsPage list', () => {
  it('renders the heading + Add button', async () => {
    render(<StudentsPage />);

    expect(
      screen.getByRole('heading', { name: 'Học viên' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: /thêm học viên/i }),
    ).toHaveAttribute('href', '/students/new');
  });

  it('does NOT render BulkActionsBar before any selection', async () => {
    render(<StudentsPage />);

    // Wait for some content to settle (header is enough — bar is conditional).
    await screen.findByRole('heading', { name: 'Học viên' });

    // BulkActionsBar carries data-testid="bulk-actions-bar-root" (per G12).
    expect(screen.queryByTestId('bulk-actions-bar-root')).not.toBeInTheDocument();
  });

  it('renders BulkActionsBar with count "Đã chọn 1" after selecting one row', async () => {
    const user = userEvent.setup();
    render(<StudentsPage />);

    // Wait for the table to populate (mock returns 2 students).
    await waitFor(
      () => {
        expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
      },
      { timeout: 4000 },
    );

    const rowCheckbox = screen.getByRole('checkbox', {
      name: /Chọn học viên Nguyễn Văn A/i,
    });
    await user.click(rowCheckbox);

    const bar = await screen.findByTestId('bulk-actions-bar-root');
    expect(bar).toBeInTheDocument();
    expect(within(bar).getByText('Đã chọn 1')).toBeInTheDocument();
  });

  it('fires non-destructive bulk action callback (Xuất CSV) without confirm dialog', async () => {
    const user = userEvent.setup();
    render(<StudentsPage />);

    await waitFor(
      () => {
        expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
      },
      { timeout: 4000 },
    );

    await user.click(
      screen.getByRole('checkbox', { name: /Chọn học viên Nguyễn Văn A/i }),
    );

    const bar = await screen.findByTestId('bulk-actions-bar-root');
    const exportBtn = within(bar).getByTestId('bulk-actions-bar-action-EXPORT_CSV');
    await user.click(exportBtn);

    // Stub logs the action ids — assert observability.
    expect(console.info).toHaveBeenCalledWith(
      expect.stringContaining('EXPORT_CSV'),
      expect.any(Array),
    );
  });

  it('shows ConfirmDialog when destructive Xóa pressed; clears selection on confirm', async () => {
    const user = userEvent.setup();
    render(<StudentsPage />);

    await waitFor(
      () => {
        expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
      },
      { timeout: 4000 },
    );

    await user.click(
      screen.getByRole('checkbox', { name: /Chọn học viên Nguyễn Văn A/i }),
    );

    const bar = await screen.findByTestId('bulk-actions-bar-root');
    await user.click(
      within(bar).getByTestId('bulk-actions-bar-action-DELETE'),
    );

    // D1 ConfirmDialog rendered by BulkActionsBar — title is set in COPY_VI.
    const dialog = await screen.findByRole('alertdialog');
    expect(within(dialog).getByText('Xác nhận xóa hàng loạt')).toBeInTheDocument();

    await user.click(within(dialog).getByText('Xác nhận xóa'));

    // After confirm, the bar disappears (selection cleared).
    await waitFor(() => {
      expect(screen.queryByTestId('bulk-actions-bar-root')).not.toBeInTheDocument();
    });
  });
});

describe('Wave 30 Bucket C — NewStudentPage bulk import dropzone', () => {
  it('renders single-form mode by default + can switch to bulk import', async () => {
    const user = userEvent.setup();
    render(<NewStudentPage />);

    expect(
      screen.getByRole('heading', { name: 'Thêm học viên mới' }),
    ).toBeInTheDocument();

    // Switch to bulk
    await user.click(screen.getByRole('tab', { name: 'Nhập hàng loạt' }));

    // BulkImportDropzone rendered (we wrap it in a marker testid).
    expect(screen.getByTestId('students-bulk-import')).toBeInTheDocument();
  });
});
