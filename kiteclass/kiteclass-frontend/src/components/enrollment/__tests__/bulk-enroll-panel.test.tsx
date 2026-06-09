/**
 * Tests for BulkEnrollPanel (GAP-1104) — bulk-enroll wizard.
 *
 * Verifies: renders the wizard, downloads the template via the API, and runs the
 * preview → commit flow surfacing per-row errors.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@/test/utils';
import { BulkEnrollPanel } from '../bulk-enroll-panel';
import { enrollmentBulkApi } from '@/lib/api/enrollment-bulk';

vi.mock('@/lib/api/enrollment-bulk', () => ({
  enrollmentBulkApi: {
    downloadTemplate: vi.fn(),
    preview: vi.fn(),
    commit: vi.fn(),
  },
}));
vi.mock('@/hooks/use-toast', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/hooks/use-toast')>();
  return { ...actual, toast: vi.fn() };
});

beforeEach(() => {
  vi.clearAllMocks();
  // jsdom lacks object-URL APIs used by the download path.
  window.URL.createObjectURL = vi.fn(() => 'blob:mock');
  window.URL.revokeObjectURL = vi.fn();
});

describe('BulkEnrollPanel', () => {
  it('renders the wizard heading + template + upload affordances', () => {
    render(<BulkEnrollPanel classId={3} />);

    expect(screen.getByRole('heading', { name: 'Ghi danh hàng loạt' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tải template mẫu/ })).toBeInTheDocument();
    expect(screen.getByLabelText(/Chọn tệp xlsx để ghi danh hàng loạt/)).toBeInTheDocument();
  });

  it('downloads the template via the API when the button is clicked', async () => {
    vi.mocked(enrollmentBulkApi.downloadTemplate).mockResolvedValue(
      new Blob(['x'], { type: 'application/octet-stream' }),
    );

    render(<BulkEnrollPanel classId={3} />);
    fireEvent.click(screen.getByRole('button', { name: /Tải template mẫu/ }));

    await waitFor(() => {
      expect(enrollmentBulkApi.downloadTemplate).toHaveBeenCalledTimes(1);
    });
  });

  it('runs preview then commit and shows per-row errors', async () => {
    vi.mocked(enrollmentBulkApi.preview).mockResolvedValue({
      totalRows: 2,
      successCount: 1,
      errorCount: 1,
      errors: [{ rowNumber: 3, field: 'class_code', message: "Không tìm thấy lớp với mã 'XX'" }],
    });
    vi.mocked(enrollmentBulkApi.commit).mockResolvedValue({
      totalRows: 2,
      successCount: 1,
      errorCount: 1,
      errors: [{ rowNumber: 3, field: 'class_code', message: "Không tìm thấy lớp với mã 'XX'" }],
    });

    render(<BulkEnrollPanel classId={3} />);

    // Pick an xlsx file.
    const input = screen.getByLabelText(/Chọn tệp xlsx để ghi danh hàng loạt/);
    const file = new File(['data'], 'ghi-danh.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    fireEvent.change(input, { target: { files: [file] } });

    // Preview.
    fireEvent.click(screen.getByRole('button', { name: 'Xem trước' }));
    await waitFor(() => expect(enrollmentBulkApi.preview).toHaveBeenCalledTimes(1));
    expect(await screen.findByText(/Không tìm thấy lớp với mã 'XX'/)).toBeInTheDocument();

    // Commit.
    fireEvent.click(screen.getByRole('button', { name: /Xác nhận ghi danh/ }));
    await waitFor(() => expect(enrollmentBulkApi.commit).toHaveBeenCalledTimes(1));
  });
});
