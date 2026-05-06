/**
 * GradebookEntryGrid component tests — Wave 28 Bucket A (G3).
 *
 * Coverage (≥6 spec states + per-cell editing):
 *  1. `loading`  state renders skeleton
 *  2. `empty`    state renders Vietnamese empty CTA
 *  3. `default`  state renders header + sticky student column + grade columns
 *  4. `editing`  state — input mode shows VN range hint
 *  5. `validation-error` cell shows VN error message
 *  6. `saving`   state — Save button disabled
 *  7. `saved`    state — banner with VN copy + Zalo OA mention
 *  8. Per-cell save indicator: dirty / saving / saved per cell
 *  9. Bulk paste handler invoked with parsed cells
 * 10. onCellChange called when a cell input commits (blur)
 * 11. onSave called on Save button click
 * 12. Sticky header row stays visible (data attribute + role)
 *
 * Vietnamese labels checked verbatim per HTML protos + README §VN UX.
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GradebookEntryGrid } from '../GradebookEntryGrid';
import type {
  GradebookEntryGridProps,
  GradebookSession,
  GradeColumn,
  GradebookStudent,
} from '../types';

const session: GradebookSession = {
  className: 'Lớp 10A2 — Toán nâng cao',
  term: 'Học kỳ I · 2026-2027',
  teacherName: 'Cô Nguyễn Thị Lan',
};

const columns: GradeColumn[] = [
  { id: 'kt15', label: "KT 15'", weight: 1 },
  { id: 'kt1tiet', label: 'KT 1 tiết', weight: 2 },
  { id: 'giuaky', label: 'Giữa kỳ', weight: 2 },
  { id: 'cuoiky', label: 'Cuối kỳ', weight: 3 },
];

function student(
  i: number,
  grades: Record<string, number | undefined> = {},
): GradebookStudent {
  return {
    studentCode: `HS-10A2-${String(i).padStart(3, '0')}`,
    fullName: `Bùi Anh Khoa ${i}`,
    grades,
  };
}

const baseProps = (
  override: Partial<GradebookEntryGridProps> = {},
): GradebookEntryGridProps => ({
  session,
  columns,
  students: [student(1), student(2), student(3)],
  state: 'default',
  onCellChange: vi.fn(),
  onSave: vi.fn(),
  ...override,
});

describe('<GradebookEntryGrid>', () => {
  it('1. loading state — renders skeleton placeholder', () => {
    render(<GradebookEntryGrid {...baseProps({ state: 'loading' })} />);
    expect(screen.getByTestId('gradebook-loading')).toBeInTheDocument();
    expect(
      screen.queryByTestId('gradebook-row-HS-10A2-001'),
    ).not.toBeInTheDocument();
  });

  it('2. empty state — shows Vietnamese empty CTA "Chưa có cột điểm nào"', () => {
    render(
      <GradebookEntryGrid
        {...baseProps({ state: 'empty', columns: [], students: [] })}
      />,
    );
    expect(
      screen.getByText(/chưa có cột điểm nào/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /thêm cột điểm/i }),
    ).toBeInTheDocument();
  });

  it('3. default state — renders class header + sticky student col + grade cols', () => {
    render(<GradebookEntryGrid {...baseProps()} />);
    // Header
    expect(screen.getByText(/lớp 10a2/i)).toBeInTheDocument();
    expect(screen.getByText(/học kỳ i/i)).toBeInTheDocument();
    // Column headers
    expect(screen.getByRole('columnheader', { name: /học sinh/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /kt 15'/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /cuối kỳ/i })).toBeInTheDocument();
    // Students rendered
    expect(screen.getByTestId('gradebook-row-HS-10A2-001')).toBeInTheDocument();
    expect(screen.getByTestId('gradebook-row-HS-10A2-002')).toBeInTheDocument();
  });

  it('4. cell input renders with VN range hint when focused', async () => {
    const user = userEvent.setup();
    render(<GradebookEntryGrid {...baseProps()} />);
    const input = screen.getByTestId('gradebook-cell-HS-10A2-001-cuoiky');
    await user.click(input);
    // Range hint surfaces on focus
    expect(screen.getByText(/thang điểm 0–10/i)).toBeInTheDocument();
  });

  it('5. cell with error status shows VN error inline', () => {
    render(
      <GradebookEntryGrid
        {...baseProps({
          cellStatuses: [
            {
              studentCode: 'HS-10A2-001',
              columnId: 'cuoiky',
              state: 'error',
              error: 'Tối đa 10',
            },
          ],
        })}
      />,
    );
    const input = screen.getByTestId('gradebook-cell-HS-10A2-001-cuoiky');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByText(/tối đa 10/i)).toBeInTheDocument();
  });

  it('6. saving state — Save button shows "Đang lưu" and is disabled', () => {
    render(
      <GradebookEntryGrid
        {...baseProps({ state: 'saving', dirtyCount: 5 })}
      />,
    );
    const bar = screen.getByTestId('gradebook-save-bar');
    const btn = within(bar).getByRole('button', { name: /đang lưu/i });
    expect(btn).toBeDisabled();
  });

  it('7. saved state — banner shows Zalo OA mention + teacher name', () => {
    render(
      <GradebookEntryGrid
        {...baseProps({
          state: 'saved',
          savedAt: '14:47',
        })}
      />,
    );
    const banner = screen.getByTestId('gradebook-saved-banner');
    expect(banner).toBeInTheDocument();
    expect(within(banner).getByText(/zalo oa/i)).toBeInTheDocument();
    expect(within(banner).getByText(/14:47/)).toBeInTheDocument();
    expect(within(banner).getByText(/cô nguyễn thị lan/i)).toBeInTheDocument();
  });

  it('8. per-cell save indicator — saving cell shows aria-busy', () => {
    render(
      <GradebookEntryGrid
        {...baseProps({
          cellStatuses: [
            {
              studentCode: 'HS-10A2-001',
              columnId: 'cuoiky',
              state: 'saving',
            },
            {
              studentCode: 'HS-10A2-002',
              columnId: 'cuoiky',
              state: 'saved',
            },
          ],
        })}
      />,
    );
    const savingCell = screen.getByTestId(
      'gradebook-cell-HS-10A2-001-cuoiky',
    );
    expect(savingCell).toHaveAttribute('aria-busy', 'true');
    const savedCell = screen.getByTestId(
      'gradebook-cell-HS-10A2-002-cuoiky',
    );
    expect(savedCell.parentElement?.getAttribute('data-cell-state')).toBe(
      'saved',
    );
  });

  it('9. bulk paste handler invoked with parsed cells from clipboard TSV', () => {
    const onBulkPaste = vi.fn();
    render(
      <GradebookEntryGrid {...baseProps({ onBulkPaste })} />,
    );
    const input = screen.getByTestId('gradebook-cell-HS-10A2-001-cuoiky');
    // Simulate Excel paste — TSV with 2 rows
    const clipboardData = {
      getData: vi.fn(() => 'HS-10A2-001\t8.5\nHS-10A2-002\t9.0'),
    };
    fireEvent.paste(input, { clipboardData });
    expect(onBulkPaste).toHaveBeenCalledTimes(1);
    expect(onBulkPaste.mock.calls[0]![0]).toEqual([
      { studentCode: 'HS-10A2-001', rawValue: '8.5' },
      { studentCode: 'HS-10A2-002', rawValue: '9.0' },
    ]);
  });

  it('10. onCellChange fires when cell input commits (blur)', async () => {
    const user = userEvent.setup();
    const onCellChange = vi.fn();
    render(
      <GradebookEntryGrid {...baseProps({ onCellChange })} />,
    );
    const input = screen.getByTestId('gradebook-cell-HS-10A2-001-cuoiky');
    await user.click(input);
    await user.keyboard('8.5');
    await user.tab();
    expect(onCellChange).toHaveBeenCalledWith(
      'HS-10A2-001',
      'cuoiky',
      '8.5',
    );
  });

  it('11. onSave fires when Save button clicked in editing state', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(
      <GradebookEntryGrid
        {...baseProps({ state: 'editing', dirtyCount: 1, onSave })}
      />,
    );
    const bar = screen.getByTestId('gradebook-save-bar');
    await user.click(within(bar).getByRole('button', { name: /^lưu/i }));
    expect(onSave).toHaveBeenCalledTimes(1);
  });

  it('12. table header row has data-sticky attribute (sticky header)', () => {
    render(<GradebookEntryGrid {...baseProps()} />);
    const head = screen.getByTestId('gradebook-thead');
    expect(head).toHaveAttribute('data-sticky', 'true');
  });

  it('renders dirtyCount in save bar when in editing state', () => {
    render(
      <GradebookEntryGrid
        {...baseProps({ state: 'editing', dirtyCount: 3 })}
      />,
    );
    const bar = screen.getByTestId('gradebook-save-bar');
    expect(within(bar).getByText(/3 thay đổi/i)).toBeInTheDocument();
  });

  it('error state — banner shows VN error message + retry button', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(
      <GradebookEntryGrid
        {...baseProps({
          state: 'error',
          errorMessage: 'Không lưu được sổ điểm',
          onSave,
        })}
      />,
    );
    expect(screen.getByTestId('gradebook-error-banner')).toBeInTheDocument();
    expect(screen.getByText(/không lưu được sổ điểm/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /thử lại/i }));
    expect(onSave).toHaveBeenCalledTimes(1);
  });
});
