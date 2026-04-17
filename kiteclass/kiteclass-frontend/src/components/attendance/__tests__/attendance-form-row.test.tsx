/**
 * Unit tests for AttendanceFormRow component.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { AttendanceFormRow } from '../attendance-form-row';
import { AttendanceStatus } from '@/types/attendance';

describe.skip('AttendanceFormRow', () => {
  const mockProps = {
    enrollmentId: 1,
    studentName: 'Nguyễn Văn A',
    status: AttendanceStatus.PRESENT,
    notes: '',
    onStatusChange: vi.fn(),
    onNotesChange: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders student name correctly', () => {
    render(<AttendanceFormRow {...mockProps} />);
    expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
  });

  it('displays current status in selector', () => {
    render(<AttendanceFormRow {...mockProps} />);
    // Status selector should show current status
    expect(screen.getByRole('combobox')).toBeInTheDocument();
  });

  it('calls onStatusChange when status is changed', () => {
    render(<AttendanceFormRow {...mockProps} />);

    const selector = screen.getByRole('combobox');
    fireEvent.click(selector);

    // Select "Vắng" option
    const absentOption = screen.getByText('Vắng');
    fireEvent.click(absentOption);

    expect(mockProps.onStatusChange).toHaveBeenCalledWith(AttendanceStatus.ABSENT);
  });

  it('calls onNotesChange when notes are updated', () => {
    render(<AttendanceFormRow {...mockProps} />);

    const notesTextarea = screen.getByPlaceholderText('Ghi chú (nếu có)...');
    fireEvent.change(notesTextarea, { target: { value: 'Học viên xin phép' } });

    expect(mockProps.onNotesChange).toHaveBeenCalledWith('Học viên xin phép');
  });

  it('displays notes value correctly', () => {
    const propsWithNotes = { ...mockProps, notes: 'Test note' };
    render(<AttendanceFormRow {...propsWithNotes} />);

    const notesTextarea = screen.getByPlaceholderText('Ghi chú (nếu có)...');
    expect(notesTextarea).toHaveValue('Test note');
  });

  it('shows all attendance status options', () => {
    render(<AttendanceFormRow {...mockProps} />);

    const selector = screen.getByRole('combobox');
    fireEvent.click(selector);

    // Check all 5 status options are present
    expect(screen.getByText('Có mặt')).toBeInTheDocument();
    expect(screen.getByText('Vắng')).toBeInTheDocument();
    expect(screen.getByText('Đi trễ')).toBeInTheDocument();
    expect(screen.getByText('Có phép')).toBeInTheDocument();
    expect(screen.getByText('Học bù')).toBeInTheDocument();
  });

  it('has correct data attribute for enrollment ID', () => {
    const { container } = render(<AttendanceFormRow {...mockProps} />);
    const row = container.querySelector('[data-enrollment-id="1"]');
    expect(row).toBeInTheDocument();
  });
});
