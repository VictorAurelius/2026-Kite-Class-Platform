/**
 * G1 Bulk Import Drop-zone — RTL coverage of the 5 spec'd states (idle,
 * drag-over, parsing, partial-success, done) + drag-over interaction +
 * file selection via input + parse-success / parse-error driven props +
 * commit progress visible during parsing.
 *
 * Spec source: `ui_kits/components/G1-bulk-import-dropzone/README.md` + 5
 * HTML state files. Vietnamese-only labels per CLAUDE.md.
 */

import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BulkImportDropzone } from '../BulkImportDropzone';
import type {
  BulkImportDropzoneProps,
  ImportSummary,
} from '../types';

function renderG1(over: Partial<BulkImportDropzoneProps> = {}) {
  const defaultProps: BulkImportDropzoneProps = {
    status: 'idle',
    tenantLabel: 'Trung tâm EduPlus',
    contextLabel: 'Tuần tuyển sinh tháng 8/2026',
  };
  return render(<BulkImportDropzone {...defaultProps} {...over} />);
}

const SAMPLE_VALID_CSV =
  'ho_va_ten,ngay_sinh,lop,phu_huynh_phone\nNguyễn Văn An,15/08/2015,Lớp 6A1,0901234567\n';

const SAMPLE_ERRORS_SUMMARY: ImportSummary = {
  validCount: 487,
  errorCount: 13,
  duplicateCount: 0,
  errors: [
    { row: 23, message: 'Số điện thoại không hợp lệ', field: 'phu_huynh_phone' },
    { row: 47, message: 'Ngày sinh sai định dạng', field: 'ngay_sinh' },
    { row: 89, message: 'Tên lớp không được để trống', field: 'lop' },
    { row: 112, message: 'Họ tên phải có ít nhất 2 từ', field: 'ho_va_ten' },
    { row: 200, message: 'Số điện thoại không hợp lệ', field: 'phu_huynh_phone' },
  ],
};

describe('<BulkImportDropzone>', () => {
  it('renders idle state with drop-zone CTA + sample download + format hint', () => {
    renderG1();
    expect(screen.getByTestId('bulk-import-dropzone-idle')).toBeInTheDocument();
    expect(
      screen.getByText(/kéo thả file csv\/excel vào đây/i),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/tải file mẫu \(\.xlsx\)/i),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/ho_va_ten · ngay_sinh · lop · phu_huynh_phone/),
    ).toBeInTheDocument();
    // Constraint hint with default 10.000 + 5 MB.
    expect(
      screen.getByText(/tối đa 10\.000 dòng/i),
    ).toBeInTheDocument();
  });

  it('renders drag-over state with primary banner + filename preview', () => {
    renderG1({ status: 'drag-over', fileName: 'danh-sach-hs-thang-8.xlsx' });
    const region = screen.getByRole('region', { name: /đang kéo file/i });
    expect(region).toBeInTheDocument();
    expect(within(region).getByText(/thả file ra đây/i)).toBeInTheDocument();
    expect(
      within(region).getByText('danh-sach-hs-thang-8.xlsx'),
    ).toBeInTheDocument();
  });

  it('renders parsing state with progress bar reflecting JobProgress', () => {
    renderG1({
      status: 'parsing',
      fileName: 'danh-sach-hs-thang-8.xlsx',
      progress: {
        processed: 310,
        total: 500,
        etaLabel: '12 giây',
        stepLabel: 'Đang kiểm tra trùng SĐT phụ huynh',
      },
    });
    expect(
      screen.getByTestId('bulk-import-dropzone-parsing'),
    ).toBeInTheDocument();
    const bar = screen.getByRole('progressbar', {
      name: /tiến độ kiểm tra dữ liệu/i,
    });
    expect(bar).toHaveAttribute('aria-valuenow', '62');
    expect(bar).toHaveAttribute('aria-valuemin', '0');
    expect(bar).toHaveAttribute('aria-valuemax', '100');
    // Counter visible.
    const counter = screen.getByTestId('bulk-import-progress-counter');
    expect(counter).toHaveTextContent('310');
    expect(counter).toHaveTextContent('500');
    // ETA + step caption.
    expect(screen.getByText(/12 giây/)).toBeInTheDocument();
    expect(
      screen.getByText(/đang kiểm tra trùng sđt phụ huynh/i),
    ).toBeInTheDocument();
  });

  it('renders partial-success state with summary banner + 4 errors + truncation note', () => {
    renderG1({
      status: 'partial-success',
      summary: SAMPLE_ERRORS_SUMMARY,
    });
    expect(
      screen.getByTestId('bulk-import-dropzone-partial-success'),
    ).toBeInTheDocument();
    // Status banner.
    const banner = screen.getByRole('status');
    expect(banner).toHaveTextContent(
      /487 trên 500 dòng hợp lệ — 13 dòng có lỗi/,
    );
    // First 4 error rows visible (per spec: render first 4, append "Còn N lỗi khác").
    expect(screen.getByTestId('bulk-import-error-row-23')).toBeInTheDocument();
    expect(screen.getByTestId('bulk-import-error-row-47')).toBeInTheDocument();
    expect(screen.getByTestId('bulk-import-error-row-89')).toBeInTheDocument();
    expect(screen.getByTestId('bulk-import-error-row-112')).toBeInTheDocument();
    // 5th error truncated.
    expect(
      screen.queryByTestId('bulk-import-error-row-200'),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText(/còn 1 lỗi khác — tải file để xem đầy đủ/i),
    ).toBeInTheDocument();
    // Commit CTA reflects valid count.
    expect(
      screen.getByTestId('bulk-import-commit'),
    ).toHaveTextContent('Tiếp tục với 487 dòng hợp lệ');
  });

  it('renders done state with hero check + valid count title', () => {
    renderG1({
      status: 'done',
      summary: { validCount: 500, errorCount: 0, duplicateCount: 12, errors: [] },
    });
    expect(screen.getByTestId('bulk-import-dropzone-done')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', {
        level: 2,
        name: /đã nhập 500 học sinh thành công/i,
      }),
    ).toBeInTheDocument();
  });

  it('drag-over interaction: dragenter on idle drop-zone switches to drag-over body', () => {
    renderG1();
    // Drag handlers live on the root container so they survive the
    // idle ↔ drag-over body swap. We dispatch on root to mirror the real
    // browser bubbling behaviour.
    const root = screen.getByTestId('bulk-import-dropzone-root');
    fireEvent.dragEnter(root, {
      dataTransfer: { files: [], items: [], types: ['Files'] },
    });
    // After dragenter, the component renders the drag-over body.
    expect(
      screen.getByTestId('bulk-import-dropzone-drag-over'),
    ).toBeInTheDocument();
    fireEvent.dragLeave(root);
    // After dragleave, idle returns.
    expect(
      screen.getByTestId('bulk-import-dropzone-idle'),
    ).toBeInTheDocument();
  });

  it('file selection via input fires onFileSelect with the selected File', async () => {
    const onFileSelect = vi.fn();
    renderG1({ onFileSelect });
    const file = new File([SAMPLE_VALID_CSV], 'students.csv', {
      type: 'text/csv',
    });
    // The label wraps a hidden <input type="file">.
    // userEvent.upload targets the input by querying it under the dropzone.
    const dropzone = screen.getByTestId('bulk-import-dropzone-idle');
    const input = dropzone.querySelector(
      'input[type="file"]',
    ) as HTMLInputElement;
    expect(input).not.toBeNull();
    await userEvent.upload(input, file);
    expect(onFileSelect).toHaveBeenCalledTimes(1);
    expect(onFileSelect.mock.calls[0]?.[0]).toBeInstanceOf(File);
    expect(onFileSelect.mock.calls[0]?.[0].name).toBe('students.csv');
  });

  it('parse-success path: caller drives done state with full validCount', () => {
    // Simulates the host pipeline: file selected → parseCSV → validateRow per
    // row → all rows valid → status goes done.
    const summary: ImportSummary = {
      validCount: 1,
      errorCount: 0,
      duplicateCount: 0,
      errors: [],
    };
    renderG1({ status: 'done', summary });
    expect(screen.getByTestId('bulk-import-dropzone-done')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', {
        level: 2,
        name: /đã nhập 1 học sinh thành công/i,
      }),
    ).toBeInTheDocument();
  });

  it('parse-error path: caller drives partial-success with structured errors', () => {
    // Simulates parser detecting invalid rows; the partial-success view
    // surfaces them as `Dòng N: <message>` rows.
    renderG1({ status: 'partial-success', summary: SAMPLE_ERRORS_SUMMARY });
    const errorRow23 = screen.getByTestId('bulk-import-error-row-23');
    expect(errorRow23).toHaveTextContent('Số điện thoại không hợp lệ');
    expect(errorRow23).toHaveTextContent('Dòng 23');
    const errorRow47 = screen.getByTestId('bulk-import-error-row-47');
    expect(errorRow47).toHaveTextContent('Ngày sinh sai định dạng');
  });

  it('commit progress visible during parsing — fill width matches percent', () => {
    const { container } = renderG1({
      status: 'parsing',
      fileName: 'danh-sach.csv',
      progress: { processed: 250, total: 500 },
    });
    const fill = container.querySelector(
      '[data-testid="bulk-import-progress-fill"]',
    ) as HTMLElement;
    expect(fill).not.toBeNull();
    expect(fill.style.width).toBe('50%');
  });

  it('uses lang="vi" by default on the wrapper', () => {
    renderG1();
    expect(
      screen.getByTestId('bulk-import-dropzone-root'),
    ).toHaveAttribute('lang', 'vi');
  });
});
