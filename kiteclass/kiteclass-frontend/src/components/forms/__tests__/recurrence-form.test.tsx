/**
 * RecurrenceForm tests — GAP-290 Wave 18a.
 *
 * Covers FE-side validation + multi-day picker + exclude-dates flow.
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@/test/utils';
import { RecurrenceForm } from '../recurrence-form';

describe('RecurrenceForm', () => {
  it('renders all 7 day chips', () => {
    render(<RecurrenceForm onSubmit={vi.fn()} />);
    expect(screen.getByTestId('day-MO')).toBeInTheDocument();
    expect(screen.getByTestId('day-SU')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /^T[2-7]$|^CN$/ })).toHaveLength(7);
  });

  it('toggles day chips on click (aria-pressed reflects state)', () => {
    render(<RecurrenceForm onSubmit={vi.fn()} />);
    const tu = screen.getByTestId('day-TU');
    expect(tu.getAttribute('aria-pressed')).toBe('false');
    fireEvent.click(tu);
    expect(tu.getAttribute('aria-pressed')).toBe('true');
    fireEvent.click(tu);
    expect(tu.getAttribute('aria-pressed')).toBe('false');
  });

  it('blocks submit when no day selected', () => {
    const onSubmit = vi.fn();
    render(<RecurrenceForm onSubmit={onSubmit} />);
    fireEvent.change(screen.getByTestId('until-date'), { target: { value: '2030-12-31' } });
    fireEvent.click(screen.getByTestId('submit-recurrence'));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText(/Phải chọn ít nhất 1 ngày/)).toBeInTheDocument();
  });

  it('blocks submit when end_time <= start_time', () => {
    const onSubmit = vi.fn();
    render(<RecurrenceForm onSubmit={onSubmit} />);
    fireEvent.click(screen.getByTestId('day-TU'));
    fireEvent.change(screen.getByTestId('start-time'), { target: { value: '20:00' } });
    fireEvent.change(screen.getByTestId('end-time'), { target: { value: '19:00' } });
    fireEvent.change(screen.getByTestId('until-date'), { target: { value: '2030-12-31' } });
    fireEvent.click(screen.getByTestId('submit-recurrence'));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText(/Giờ kết thúc phải sau giờ bắt đầu/)).toBeInTheDocument();
  });

  it('submits valid rule with TU+TH 19:00-20:30 until 2030-12-31', () => {
    const onSubmit = vi.fn();
    render(<RecurrenceForm onSubmit={onSubmit} />);
    fireEvent.click(screen.getByTestId('day-TU'));
    fireEvent.click(screen.getByTestId('day-TH'));
    fireEvent.change(screen.getByTestId('until-date'), { target: { value: '2030-12-31' } });
    fireEvent.click(screen.getByTestId('submit-recurrence'));
    expect(onSubmit).toHaveBeenCalledWith({
      freq: 'WEEKLY',
      byDay: ['TU', 'TH'],
      startTime: '19:00',
      endTime: '20:30',
      until: '2030-12-31',
    });
  });

  it('adds and removes exclude dates', () => {
    const onSubmit = vi.fn();
    render(<RecurrenceForm onSubmit={onSubmit} />);
    fireEvent.click(screen.getByTestId('day-MO'));
    fireEvent.change(screen.getByTestId('exclude-date-input'), { target: { value: '2030-06-15' } });
    fireEvent.click(screen.getByText('Thêm'));
    expect(screen.getByTestId('exclude-dates-list')).toHaveTextContent('2030-06-15');

    fireEvent.change(screen.getByTestId('until-date'), { target: { value: '2030-12-31' } });
    fireEvent.click(screen.getByTestId('submit-recurrence'));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        excludeDates: ['2030-06-15'],
      })
    );
  });

  it('shows warning banner when hasExistingSessions=true', () => {
    render(<RecurrenceForm onSubmit={vi.fn()} hasExistingSessions />);
    expect(
      screen.getByText(/Sửa quy tắc sẽ tạo lại các buổi sắp tới/)
    ).toBeInTheDocument();
  });

  it('hydrates initial rule from props', () => {
    render(
      <RecurrenceForm
        onSubmit={vi.fn()}
        initialRule={{
          freq: 'WEEKLY',
          byDay: ['MO', 'WE', 'FR'],
          startTime: '08:00',
          endTime: '09:30',
          until: '2030-12-31',
        }}
      />
    );
    expect(screen.getByTestId('day-MO').getAttribute('aria-pressed')).toBe('true');
    expect(screen.getByTestId('day-WE').getAttribute('aria-pressed')).toBe('true');
    expect(screen.getByTestId('day-FR').getAttribute('aria-pressed')).toBe('true');
    expect(screen.getByTestId('day-TU').getAttribute('aria-pressed')).toBe('false');
    expect((screen.getByTestId('start-time') as HTMLInputElement).value).toBe('08:00');
  });
});
