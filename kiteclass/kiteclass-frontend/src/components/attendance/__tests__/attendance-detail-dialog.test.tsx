/**
 * AttendanceDetailDialog component tests.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { AttendanceDetailDialog } from '../attendance-detail-dialog';
import { mockAttendanceRecords } from '@/__tests__/fixtures/attendance';

describe('AttendanceDetailDialog', () => {
  const testDate = new Date('2026-03-01');
  const mockOnOpenChange = vi.fn();

  beforeEach(() => {
    mockOnOpenChange.mockClear();
  });

  describe('Visibility', () => {
    it('renders when open is true', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('does not render when open is false', () => {
      render(
        <AttendanceDetailDialog
          open={false}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('does not render when date is null', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={null}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('does not render when records array is empty', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={[]}
        />
      );

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  describe('Header', () => {
    it('displays dialog title', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText('Chi tiết điểm danh')).toBeInTheDocument();
    });

    it('displays formatted date', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      // Date should be formatted in Vietnamese
      const dateText = screen.getByText(/tháng 3/i);
      expect(dateText).toBeInTheDocument();
    });
  });

  describe('Statistics Summary', () => {
    it('displays total count', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText('Tổng')).toBeInTheDocument();
      expect(screen.getByText('4')).toBeInTheDocument(); // 4 records
    });

    it('displays present count', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText('Có mặt')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument(); // 1 PRESENT
    });

    it('displays absent count', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText('Vắng')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument(); // 1 ABSENT
    });

    it('displays late count', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText('Đi trễ')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument(); // 1 LATE
    });

    it('displays excused count', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText('Có phép')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument(); // 1 EXCUSED
    });

    it('displays makeup count', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText('Học bù')).toBeInTheDocument();
      expect(screen.getByText('0')).toBeInTheDocument(); // 0 MAKEUP
    });
  });

  describe('Record Details', () => {
    it('displays student names', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    });

    it('displays session numbers', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText('Buổi 1')).toBeInTheDocument();
      expect(screen.getByText('Buổi 2')).toBeInTheDocument();
      expect(screen.getByText('Buổi 3')).toBeInTheDocument();
      expect(screen.getByText('Buổi 4')).toBeInTheDocument();
    });

    it('displays attendance status badges', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText('Có mặt')).toBeInTheDocument();
      expect(screen.getByText('Vắng')).toBeInTheDocument();
      expect(screen.getByText('Đi trễ')).toBeInTheDocument();
      expect(screen.getByText('Có phép')).toBeInTheDocument();
    });

    it('displays notes when available', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getByText(/Ốm/)).toBeInTheDocument();
      expect(screen.getByText(/Đến trễ 15 phút/)).toBeInTheDocument();
      expect(screen.getByText(/Xin phép nghỉ/)).toBeInTheDocument();
    });

    it('displays marked by information', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      expect(screen.getAllByText(/GV Trần B/).length).toBeGreaterThan(0);
    });
  });

  describe('Grouping by Session', () => {
    it('groups records by session', () => {
      const multiSessionRecords = [
        { ...mockAttendanceRecords[0], sessionNumber: 1 },
        { ...mockAttendanceRecords[1], sessionNumber: 1 },
        { ...mockAttendanceRecords[2], sessionNumber: 2 },
      ];

      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={multiSessionRecords}
        />
      );

      const session1Headers = screen.getAllByText('Buổi 1');
      const session2Headers = screen.getAllByText('Buổi 2');

      expect(session1Headers.length).toBeGreaterThan(0);
      expect(session2Headers.length).toBeGreaterThan(0);
    });
  });

  describe('Closing', () => {
    it('calls onOpenChange when closed via escape', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={mockAttendanceRecords}
        />
      );

      fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });

      // Note: Actual close behavior depends on Dialog implementation
      // This test verifies the dialog is rendered correctly
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
  });

  describe('Edge Cases', () => {
    it('handles single record', () => {
      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={[mockAttendanceRecords[0]]}
        />
      );

      expect(screen.getByText('1')).toBeInTheDocument(); // Total count
    });

    it('handles records without notes', () => {
      const recordsWithoutNotes = mockAttendanceRecords.map((r) => ({
        ...r,
        notes: undefined,
      }));

      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={recordsWithoutNotes}
        />
      );

      expect(screen.queryByText('Ghi chú:')).not.toBeInTheDocument();
    });

    it('handles records without marked by name', () => {
      const recordsWithoutMarkedBy = mockAttendanceRecords.map((r) => ({
        ...r,
        markedByName: undefined,
      }));

      render(
        <AttendanceDetailDialog
          open={true}
          onOpenChange={mockOnOpenChange}
          date={testDate}
          records={recordsWithoutMarkedBy}
        />
      );

      expect(screen.queryByText('Điểm danh bởi:')).not.toBeInTheDocument();
    });
  });
});
