/**
 * Wave 60 Bucket B — GAP-137 bulk-import admin page tests.
 *
 * Covers:
 *  - Initial render (heading, file picker, disabled Preview button)
 *  - File picking flips phase idle → selected
 *  - Wrong-extension rejection (.csv)
 *  - Oversized file rejection (>10MB)
 *  - Happy path: upload → preview → commit (success)
 *  - Mixed path: upload → preview shows errors → commit shows partial result
 *  - BE 500 error → user-visible error alert
 *  - Reset clears file + phase
 *
 * MSW handler triggers in `src/mocks/handlers.ts`:
 *  - "good.xlsx" → 10 rows / 0 errors (default)
 *  - "errors.xlsx" → 5 rows / 2 errors
 *  - "fail.xlsx" → HTTP 500
 *  - "invalid.xlsx" → HTTP 400
 *
 * @since 2026-05-12
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import BulkImportPage from '../page';
import { bulkImportApi } from '@/lib/api/bulk-import';

function makeXlsxFile(name: string, size = 1024): File {
  const content = new Uint8Array(size);
  return new File([content], name, {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
}

function getFileInput(): HTMLInputElement {
  // The label's `htmlFor` matches the input id; use the id selector since the
  // input itself is sr-only and not directly queryable by role/name.
  const input = document.getElementById(
    'bulk-import-file-input',
  ) as HTMLInputElement | null;
  if (!input) throw new Error('bulk-import-file-input not in DOM');
  return input;
}

describe('Wave 60 Bucket B — Bulk Import admin page', () => {
  beforeEach(() => {
    // Suppress axios error logs surfacing via console in jsdom.
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  it('renders heading, breadcrumb, and a disabled Preview button initially', async () => {
    render(<BulkImportPage />);

    expect(
      screen.getByRole('heading', { name: /nhập học viên hàng loạt/i }),
    ).toBeInTheDocument();
    // The page renders a breadcrumb link back to /students (sidebar also has
    // a "Học viên" link, hence we filter by href).
    const studentLinks = screen
      .getAllByRole('link', { name: /học viên/i })
      .filter((el) => el.getAttribute('href') === '/students');
    expect(studentLinks.length).toBeGreaterThan(0);

    const previewBtn = screen.getByRole('button', { name: /xem trước/i });
    expect(previewBtn).toBeDisabled();
  });

  it('rejects non-xlsx files', async () => {
    // Disable userEvent's `applyAccept` filter so the .csv file actually reaches
    // the change handler. The page enforces the extension client-side anyway.
    const user = userEvent.setup({ applyAccept: false });
    render(<BulkImportPage />);

    const csv = new File(['name,email'], 'list.csv', { type: 'text/csv' });
    await user.upload(getFileInput(), csv);

    const alert = await screen.findByTestId('bulk-import-error');
    expect(within(alert).getByText(/chỉ chấp nhận tệp \.xlsx/i)).toBeInTheDocument();
  });

  it('rejects oversized files (>10MB)', async () => {
    const user = userEvent.setup();
    render(<BulkImportPage />);

    const huge = makeXlsxFile('big.xlsx', 11 * 1024 * 1024);
    await user.upload(getFileInput(), huge);

    const alert = await screen.findByTestId('bulk-import-error');
    expect(within(alert).getByText(/vượt quá 10 mb/i)).toBeInTheDocument();
  });

  it('happy path: upload → preview (0 errors) → commit → success state', async () => {
    const user = userEvent.setup();
    render(<BulkImportPage />);

    const file = makeXlsxFile('good.xlsx');
    await user.upload(getFileInput(), file);

    expect(screen.getByTestId('bulk-import-file-name')).toHaveTextContent('good.xlsx');

    const previewBtn = screen.getByRole('button', { name: /xem trước/i });
    expect(previewBtn).toBeEnabled();
    await user.click(previewBtn);

    const preview = await screen.findByTestId('bulk-import-preview');
    expect(within(preview).getByText(/không có lỗi/i)).toBeInTheDocument();

    const commitBtn = within(preview).getByRole('button', {
      name: /xác nhận nhập \(10 hàng\)/i,
    });
    await user.click(commitBtn);

    // After commit success, the section heading flips to "Kết quả nhập".
    await waitFor(() => {
      expect(screen.getByText(/kết quả nhập/i)).toBeInTheDocument();
    });
    expect(
      screen.getByRole('link', { name: /về danh sách học viên/i }),
    ).toHaveAttribute('href', '/students');
  });

  it('preview with errors lists row-level diagnostics and allows commit', async () => {
    const user = userEvent.setup();
    render(<BulkImportPage />);

    const file = makeXlsxFile('errors.xlsx');
    await user.upload(getFileInput(), file);
    await user.click(screen.getByRole('button', { name: /xem trước/i }));

    const preview = await screen.findByTestId('bulk-import-preview');
    expect(within(preview).getByTestId('bulk-import-error-row-2')).toHaveTextContent(
      /email không hợp lệ/i,
    );
    expect(within(preview).getByTestId('bulk-import-error-row-4')).toHaveTextContent(
      /số điện thoại/i,
    );

    // Commit with 3 valid rows
    await user.click(
      within(preview).getByRole('button', { name: /xác nhận nhập \(3 hàng\)/i }),
    );

    await waitFor(() => {
      expect(screen.getByText(/kết quả nhập/i)).toBeInTheDocument();
    });
    // Error-report download button visible because errorCount > 0
    expect(
      screen.getByRole('button', { name: /tải báo cáo lỗi/i }),
    ).toBeInTheDocument();
  });

  it('shows error alert when BE returns 500 on preview', async () => {
    const user = userEvent.setup();
    render(<BulkImportPage />);

    await user.upload(getFileInput(), makeXlsxFile('fail.xlsx'));
    await user.click(screen.getByRole('button', { name: /xem trước/i }));

    const alert = await screen.findByTestId('bulk-import-error');
    expect(within(alert).getByText(/lỗi máy chủ nội bộ/i)).toBeInTheDocument();
  });

  it('renders "Tải template mẫu" button and clicking it calls downloadTemplate (GAP-1102)', async () => {
    const user = userEvent.setup();
    const blob = new Blob(['xlsx-bytes'], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const spy = vi
      .spyOn(bulkImportApi, 'downloadTemplate')
      .mockResolvedValue(blob);
    // jsdom lacks object-URL APIs — stub for the download handler.
    window.URL.createObjectURL = vi.fn(() => 'blob:mock-template');
    window.URL.revokeObjectURL = vi.fn();

    render(<BulkImportPage />);

    const templateBtn = screen.getByRole('button', {
      name: /tải template mẫu/i,
    });
    expect(templateBtn).toBeInTheDocument();

    await user.click(templateBtn);

    await waitFor(() => {
      expect(spy).toHaveBeenCalledTimes(1);
    });

    spy.mockRestore();
  });

  it('reset clears selected file and returns to idle', async () => {
    const user = userEvent.setup();
    render(<BulkImportPage />);

    await user.upload(getFileInput(), makeXlsxFile('good.xlsx'));
    expect(screen.getByTestId('bulk-import-file-name')).toBeInTheDocument();

    const resetBtn = screen.getByRole('button', { name: /đặt lại/i });
    await user.click(resetBtn);

    expect(screen.queryByTestId('bulk-import-file-name')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /xem trước/i })).toBeDisabled();
  });
});
