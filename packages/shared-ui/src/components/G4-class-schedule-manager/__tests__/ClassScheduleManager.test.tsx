/**
 * ClassScheduleManager component tests — Wave 28 Bucket B (G4).
 *
 * Coverage of the 5 spec'd states + interaction wiring:
 *   1. `empty`            — preset CTA grid renders 3 presets + "Thêm buổi học"
 *   2. `single-class`     — week grid renders one slot
 *   3. `recurring-edit`   — day toggles render 7 buttons (T2..CN), VN week-start helper visible
 *   4. `recurring-edit`   — recurrence end mode is mutually exclusive (date OR count radio)
 *   5. `conflict-warning` — alert role + 3 resolution buttons + summary copy
 *   6. `saved`            — banner copy + stat strip + class list
 *   7. preset CTA wiring — clicking each preset emits `onPickPreset` with correct id
 *   8. add-slot wiring   — "Thêm buổi học" emits onAddSlot
 *   9. resolve-conflict  — clicking resolution button emits onResolveConflict with right key
 *
 * Vietnamese labels checked verbatim per kit README §VN UX.
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ClassScheduleManager } from '../ClassScheduleManager';
import type {
  ClassScheduleManagerProps,
  ConflictWarning,
  ScheduleSlot,
} from '../types';

const baseSlot: ScheduleSlot = {
  id: 'slot-toan',
  className: 'Toán nâng cao',
  teacherName: 'Cô Lan',
  date: '2026-04-07',
  startTime: '14:00',
  endTime: '15:30',
  recurrence: 'WEEKLY',
  daysOfWeek: ['MON', 'WED', 'FRI'],
  endsOn: '2027-05-30',
};

function renderManager(overrides: Partial<ClassScheduleManagerProps> = {}) {
  const props: ClassScheduleManagerProps = {
    className: 'Lớp 6A1',
    schoolYearLabel: 'Năm học 2026-2027',
    slots: [],
    state: 'empty',
    onAddSlot: vi.fn(),
    onPickPreset: vi.fn(),
    onSaveSlot: vi.fn(),
    onCancelEdit: vi.fn(),
    onResolveConflict: vi.fn(),
    onExportPdf: vi.fn(),
    ...overrides,
  };
  render(<ClassScheduleManager {...props} />);
  return props;
}

describe('<ClassScheduleManager>', () => {
  it('renders the empty-state preset grid with 3 VN preset cards + add CTA', () => {
    renderManager({ state: 'empty' });
    expect(screen.getByText('Chưa có buổi học nào')).toBeInTheDocument();
    expect(screen.getByText('3 buổi/tuần')).toBeInTheDocument();
    expect(screen.getByText('Hằng ngày')).toBeInTheDocument();
    expect(screen.getByText('Cuối tuần')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Thêm buổi học' }),
    ).toBeInTheDocument();
  });

  it('renders the single-class week grid with one slot', () => {
    renderManager({ state: 'single-class', slots: [baseSlot] });
    expect(screen.getByTestId('g4-single-class')).toBeInTheDocument();
    expect(screen.getByText('Toán nâng cao')).toBeInTheDocument();
    expect(screen.getByText('14:00 – 15:30')).toBeInTheDocument();
  });

  it('renders the recurring-edit form with 7 weekday toggles starting Monday (T2)', () => {
    renderManager({ state: 'recurring-edit', editingSlot: baseSlot });
    const group = screen.getByRole('group', { name: 'Lặp lại vào các ngày' });
    const buttons = within(group).getAllByRole('button');
    expect(buttons).toHaveLength(7);
    // First column = T2 = MONDAY (NOT Sunday — VN convention).
    expect(buttons[0]).toHaveTextContent('T2');
    expect(buttons[6]).toHaveTextContent('CN');
    // Helper sentence about VN week start should be visible.
    expect(
      screen.getByText(
        /Tuần lễ Việt Nam bắt đầu Thứ Hai\. Cuối tuần \(T7, CN\) hiển thị màu xanh\./,
      ),
    ).toBeInTheDocument();
  });

  it('renders mutually-exclusive recurrence-end radios (date OR count)', () => {
    renderManager({ state: 'recurring-edit', editingSlot: baseSlot });
    const dateRadio = screen.getByLabelText(
      /Theo ngày kết thúc/,
    ) as HTMLInputElement;
    const countRadio = screen.getByLabelText(
      /Theo số buổi/,
    ) as HTMLInputElement;
    expect(dateRadio.type).toBe('radio');
    expect(countRadio.type).toBe('radio');
    expect(dateRadio.name).toBe('recur-end-mode');
    expect(countRadio.name).toBe(dateRadio.name); // same group → mutually exclusive
  });

  it('renders the conflict-warning state with role="alert" + 3 resolution buttons per conflict', () => {
    const slotB: ScheduleSlot = {
      ...baseSlot,
      id: 'slot-van',
      className: 'Văn 8',
      teacherName: 'Cô Lan',
    };
    const conflict: ConflictWarning = {
      slotAId: baseSlot.id,
      slotBId: slotB.id,
      date: '2026-04-11',
      summary: '11/04/2026 · 14:00 – 15:30',
      reason: 'Trùng giờ với Văn 8 (Cô Lan)',
    };
    renderManager({
      state: 'conflict-warning',
      slots: [baseSlot, slotB],
      conflicts: [conflict],
    });
    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(within(alert).getByText('11/04/2026 · 14:00 – 15:30')).toBeInTheDocument();
    expect(
      within(alert).getByText('Trùng giờ với Văn 8 (Cô Lan)'),
    ).toBeInTheDocument();
    expect(
      within(alert).getByRole('button', { name: 'Đổi giáo viên cho buổi này' }),
    ).toBeInTheDocument();
    expect(
      within(alert).getByRole('button', { name: 'Đổi giờ học' }),
    ).toBeInTheDocument();
    expect(
      within(alert).getByRole('button', {
        name: 'Bỏ qua ngày này trong chuỗi lặp',
      }),
    ).toBeInTheDocument();
  });

  it('renders the saved-state banner + stat strip + class list', () => {
    renderManager({ state: 'saved', slots: [baseSlot] });
    expect(screen.getByText('Đã lưu lịch học')).toBeInTheDocument();
    expect(screen.getByText('Tổng buổi/tuần')).toBeInTheDocument();
    expect(screen.getByText('Môn học')).toBeInTheDocument();
    expect(screen.getByText('Giáo viên')).toBeInTheDocument();
    expect(screen.getByText('Danh sách buổi học')).toBeInTheDocument();
  });

  it('emits onPickPreset with the correct id when a preset card is clicked', async () => {
    const props = renderManager({ state: 'empty' });
    await userEvent.click(screen.getByText('3 buổi/tuần'));
    expect(props.onPickPreset).toHaveBeenCalledWith('three-per-week');
    await userEvent.click(screen.getByText('Hằng ngày'));
    expect(props.onPickPreset).toHaveBeenCalledWith('daily');
    await userEvent.click(screen.getByText('Cuối tuần'));
    expect(props.onPickPreset).toHaveBeenCalledWith('weekend');
  });

  it('emits onAddSlot when the empty-state CTA is clicked', async () => {
    const props = renderManager({ state: 'empty' });
    await userEvent.click(screen.getByRole('button', { name: 'Thêm buổi học' }));
    expect(props.onAddSlot).toHaveBeenCalledTimes(1);
  });

  it('emits onResolveConflict with the right resolution key', async () => {
    const slotB: ScheduleSlot = { ...baseSlot, id: 'slot-van', className: 'Văn 8' };
    const conflict: ConflictWarning = {
      slotAId: baseSlot.id,
      slotBId: slotB.id,
      date: '2026-04-11',
      summary: '11/04/2026 · 14:00 – 15:30',
      reason: 'Trùng giờ với Văn 8',
    };
    const props = renderManager({
      state: 'conflict-warning',
      slots: [baseSlot, slotB],
      conflicts: [conflict],
    });
    await userEvent.click(
      screen.getByRole('button', { name: 'Đổi giáo viên cho buổi này' }),
    );
    expect(props.onResolveConflict).toHaveBeenCalledWith(
      conflict,
      'change-teacher',
    );
  });
});
