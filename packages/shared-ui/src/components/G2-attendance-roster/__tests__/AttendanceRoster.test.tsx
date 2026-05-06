/**
 * AttendanceRoster component tests — Wave 27 Bucket A (G2).
 *
 * Coverage:
 *  1. `loading`  state renders skeleton rows
 *  2. `empty`    state renders Vietnamese empty CTA
 *  3. `default`  state renders 4 toggle buttons per student + save bar hidden
 *  4. `marking`  state shows sticky save bar with N-thay-đổi count + Save button enabled
 *  5. `saving`   state disables save button (in-flight)
 *  6. `saved`    state locks toggles read-only + shows "Đã lưu lúc" inline
 *  7. `error`    state shows banner + retry CTA wired to onSave
 *  8. Toggle: clicking a status button emits `onChange(studentId, status)`
 *  9. Mark-all-present button calls onMarkAllPresent when provided
 * 10. Sticky save bar primary button calls onSave
 *
 * Vietnamese labels checked verbatim per spec.md §States + §Vietnamese UX.
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AttendanceRoster } from '../AttendanceRoster';
import type {
  AttendanceRosterProps,
  ClassSession,
  StudentRecord,
} from '../types';

const session: ClassSession = {
  id: 'cls-10A2-toan-2026-04-15',
  className: 'Lớp 10A2 — Toán nâng cao',
  sessionNumber: 12,
  date: new Date('2026-04-15T07:00:00.000Z'), // 14:00 ICT
  durationMinutes: 90,
  teacherName: 'Cô Nguyễn Thị Lan',
};

function student(
  i: number,
  status: StudentRecord['status'] = null,
): StudentRecord {
  return {
    id: `stu-${i}`,
    fullName: `Nguyễn Văn Học Sinh ${i}`,
    studentCode: `MST${String(i).padStart(4, '0')}`,
    currentRate: 0.92,
    status,
  };
}

const baseProps = (
  override: Partial<AttendanceRosterProps> = {},
): AttendanceRosterProps => ({
  classSession: session,
  students: [student(1), student(2), student(3)],
  state: 'default',
  onChange: vi.fn(),
  onSave: vi.fn(),
  ...override,
});

describe('<AttendanceRoster>', () => {
  it('1. loading state — renders skeleton placeholder', () => {
    render(<AttendanceRoster {...baseProps({ state: 'loading' })} />);
    expect(
      screen.getByTestId('attendance-roster-loading'),
    ).toBeInTheDocument();
    // No real student rows during skeleton
    expect(
      screen.queryByTestId('attendance-row-stu-1'),
    ).not.toBeInTheDocument();
  });

  it('2. empty state — shows Vietnamese empty CTA', () => {
    render(
      <AttendanceRoster
        {...baseProps({ state: 'empty', students: [] })}
      />,
    );
    expect(
      screen.getByText(/chưa có học sinh nào trong lớp/i),
    ).toBeInTheDocument();
  });

  it('3. default state — renders 4 toggle buttons per student + save bar hidden', () => {
    render(<AttendanceRoster {...baseProps()} />);
    const row = screen.getByTestId('attendance-row-stu-1');
    // 4 status buttons
    expect(within(row).getByRole('radio', { name: /có mặt/i })).toBeInTheDocument();
    expect(
      within(row).getByRole('radio', { name: /vắng có phép/i }),
    ).toBeInTheDocument();
    expect(
      within(row).getByRole('radio', { name: /vắng không phép/i }),
    ).toBeInTheDocument();
    expect(within(row).getByRole('radio', { name: /đi trễ/i })).toBeInTheDocument();

    // Save bar hidden in default state
    expect(
      screen.queryByTestId('attendance-save-bar'),
    ).not.toBeInTheDocument();
  });

  it('4. marking state — shows save bar with N thay đổi + enabled Save button', () => {
    render(
      <AttendanceRoster
        {...baseProps({ state: 'marking', dirtyCount: 5 })}
      />,
    );
    const bar = screen.getByTestId('attendance-save-bar');
    expect(within(bar).getByText(/5 thay đổi/i)).toBeInTheDocument();
    const saveBtn = within(bar).getByRole('button', { name: /^lưu$/i });
    expect(saveBtn).toBeEnabled();
  });

  it('5. saving state — Save button disabled (in flight)', () => {
    render(
      <AttendanceRoster
        {...baseProps({ state: 'saving', dirtyCount: 5 })}
      />,
    );
    const bar = screen.getByTestId('attendance-save-bar');
    expect(within(bar).getByRole('button', { name: /đang lưu/i })).toBeDisabled();
  });

  it('6. saved state — toggles locked + "Đã lưu" inline indicator', () => {
    render(
      <AttendanceRoster
        {...baseProps({
          state: 'saved',
          students: [student(1, 'P'), student(2, 'V')],
        })}
      />,
    );
    expect(screen.getByTestId('attendance-saved-badge')).toBeInTheDocument();
    expect(screen.getByText(/đã lưu/i)).toBeInTheDocument();
    // Toggles disabled for tap
    const row = screen.getByTestId('attendance-row-stu-1');
    expect(within(row).getByRole('radio', { name: /có mặt/i })).toBeDisabled();
  });

  it('7. error state — shows error banner + retry button calls onSave', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(
      <AttendanceRoster
        {...baseProps({
          state: 'error',
          onSave,
          errorMessage: 'Không lưu được',
        })}
      />,
    );
    expect(
      screen.getByTestId('attendance-error-banner'),
    ).toBeInTheDocument();
    expect(screen.getByText(/không lưu được/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /thử lại/i }));
    expect(onSave).toHaveBeenCalledTimes(1);
  });

  it('8. clicking a status button emits onChange(studentId, status)', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<AttendanceRoster {...baseProps({ onChange })} />);
    const row = screen.getByTestId('attendance-row-stu-1');
    await user.click(within(row).getByRole('radio', { name: /vắng có phép/i }));
    expect(onChange).toHaveBeenCalledWith('stu-1', 'V');
  });

  it('9. mark-all-present button calls onMarkAllPresent when provided', async () => {
    const user = userEvent.setup();
    const onMarkAllPresent = vi.fn();
    render(<AttendanceRoster {...baseProps({ onMarkAllPresent })} />);
    await user.click(screen.getByRole('button', { name: /tất cả có mặt/i }));
    expect(onMarkAllPresent).toHaveBeenCalledTimes(1);
  });

  it('10. save bar primary button calls onSave', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(
      <AttendanceRoster
        {...baseProps({
          state: 'marking',
          dirtyCount: 2,
          onSave,
        })}
      />,
    );
    const bar = screen.getByTestId('attendance-save-bar');
    await user.click(within(bar).getByRole('button', { name: /^lưu$/i }));
    expect(onSave).toHaveBeenCalledTimes(1);
  });

  it('renders class session header with name + session number', () => {
    render(<AttendanceRoster {...baseProps()} />);
    expect(screen.getByText(/lớp 10a2/i)).toBeInTheDocument();
    expect(screen.getByText(/buổi #12/i)).toBeInTheDocument();
  });

  it('marks active status visually for already-set rows', () => {
    render(
      <AttendanceRoster
        {...baseProps({
          students: [student(1, 'P'), student(2, 'L')],
        })}
      />,
    );
    const row1 = screen.getByTestId('attendance-row-stu-1');
    expect(
      within(row1).getByRole('radio', { name: /có mặt/i }),
    ).toHaveAttribute('aria-checked', 'true');
    const row2 = screen.getByTestId('attendance-row-stu-2');
    expect(
      within(row2).getByRole('radio', { name: /đi trễ/i }),
    ).toHaveAttribute('aria-checked', 'true');
  });
});
