/**
 * AttendanceHistoryTable component tests.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { AttendanceHistoryTable } from '../attendance-history-table';
import { mockAttendanceRecords } from '@/__tests__/fixtures/attendance';

describe('AttendanceHistoryTable', () => {
  describe('Loading State', () => {
    it('renders loading skeleton when isLoading is true', () => {
      render(<AttendanceHistoryTable data={[]} isLoading={true} />);

      const skeletons = screen.getAllByRole('generic').filter((el) =>
        el.className.includes('animate-pulse')
      );
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it('does not show data while loading', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={true}
        />
      );

      expect(screen.queryByText('Nguyễn Văn A')).not.toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('shows empty state when no data', () => {
      render(<AttendanceHistoryTable data={[]} isLoading={false} />);

      expect(screen.getByText('Chưa có lịch sử điểm danh')).toBeInTheDocument();
    });

    it('shows empty state message', () => {
      render(<AttendanceHistoryTable data={[]} isLoading={false} />);

      expect(
        screen.getByText(/Lịch sử điểm danh sẽ hiển thị tại đây khi có dữ liệu/)
      ).toBeInTheDocument();
    });

    it('shows empty state icon', () => {
      render(<AttendanceHistoryTable data={[]} isLoading={false} />);

      expect(screen.getByText('📋')).toBeInTheDocument();
    });
  });

  describe('Data Rendering', () => {
    it('renders table with data', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
        />
      );

      // Multiple instances of student name (one per record)
      expect(screen.getAllByText('Nguyễn Văn A').length).toBeGreaterThan(0);
    });

    it('renders all records', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
        />
      );

      // Should render 4 student name instances (one per record)
      const studentNames = screen.getAllByText('Nguyễn Văn A');
      expect(studentNames.length).toBe(4);
    });

    it('renders session numbers', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
        />
      );

      expect(screen.getByText('Buổi 1')).toBeInTheDocument();
      expect(screen.getByText('Buổi 2')).toBeInTheDocument();
      expect(screen.getByText('Buổi 3')).toBeInTheDocument();
      expect(screen.getByText('Buổi 4')).toBeInTheDocument();
    });

    it('renders attendance statuses', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
        />
      );

      expect(screen.getByText('Có mặt')).toBeInTheDocument();
      expect(screen.getByText('Vắng')).toBeInTheDocument();
      expect(screen.getByText('Đi trễ')).toBeInTheDocument();
      expect(screen.getByText('Có phép')).toBeInTheDocument();
    });

    it('renders notes', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
        />
      );

      expect(screen.getByText('Ốm')).toBeInTheDocument();
      expect(screen.getByText('Đến trễ 15 phút')).toBeInTheDocument();
      expect(screen.getByText('Xin phép nghỉ')).toBeInTheDocument();
    });

    it('renders marked by names', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
        />
      );

      const markedByElements = screen.getAllByText('GV Trần B');
      expect(markedByElements.length).toBeGreaterThan(0);
    });
  });

  describe('Pagination', () => {
    const paginatedProps = {
      data: mockAttendanceRecords,
      isLoading: false,
      totalElements: 100,
      page: 0,
      size: 20,
      onPageChange: vi.fn(),
    };

    it('shows pagination controls when totalPages > 1', () => {
      render(<AttendanceHistoryTable {...paginatedProps} />);

      expect(screen.getByText('Trước')).toBeInTheDocument();
      expect(screen.getByText('Tiếp')).toBeInTheDocument();
    });

    it('shows current page info', () => {
      render(<AttendanceHistoryTable {...paginatedProps} />);

      expect(screen.getByText(/Hiển thị 1 - 4 trong tổng số 100 bản ghi/)).toBeInTheDocument();
    });

    it('calls onPageChange when clicking next', () => {
      const onPageChange = vi.fn();
      render(<AttendanceHistoryTable {...paginatedProps} onPageChange={onPageChange} />);

      const nextButton = screen.getByText('Tiếp');
      fireEvent.click(nextButton);

      expect(onPageChange).toHaveBeenCalledWith(1);
    });

    it('calls onPageChange when clicking previous', () => {
      const onPageChange = vi.fn();
      render(
        <AttendanceHistoryTable
          {...paginatedProps}
          page={2}
          onPageChange={onPageChange}
        />
      );

      const prevButton = screen.getByText('Trước');
      fireEvent.click(prevButton);

      expect(onPageChange).toHaveBeenCalledWith(1);
    });

    it('disables previous button on first page', () => {
      render(<AttendanceHistoryTable {...paginatedProps} page={0} />);

      const prevButton = screen.getByText('Trước');
      expect(prevButton).toBeDisabled();
    });

    it('disables next button on last page', () => {
      render(
        <AttendanceHistoryTable
          {...paginatedProps}
          page={4} // Last page (100 items / 20 per page = 5 pages, 0-indexed)
          totalElements={100}
        />
      );

      const nextButton = screen.getByText('Tiếp');
      expect(nextButton).toBeDisabled();
    });

    it('shows page numbers', () => {
      render(<AttendanceHistoryTable {...paginatedProps} />);

      expect(screen.getByText('1')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
    });

    it('calls onPageChange when clicking page number', () => {
      const onPageChange = vi.fn();
      render(<AttendanceHistoryTable {...paginatedProps} onPageChange={onPageChange} />);

      const pageButton = screen.getByText('2');
      fireEvent.click(pageButton);

      expect(onPageChange).toHaveBeenCalledWith(1); // Page 2 is index 1
    });

    it('highlights current page', () => {
      render(<AttendanceHistoryTable {...paginatedProps} page={1} />);

      const currentPageButton = screen.getByText('2');
      expect(currentPageButton).toHaveClass('bg-primary');
    });
  });

  describe('No Pagination', () => {
    it('does not show pagination when totalPages <= 1', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
          totalElements={4}
          page={0}
          size={20}
        />
      );

      expect(screen.queryByText('Trước')).not.toBeInTheDocument();
      expect(screen.queryByText('Tiếp')).not.toBeInTheDocument();
    });

    it('does not show pagination when onPageChange is not provided', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
          totalElements={100}
          page={0}
          size={20}
        />
      );

      expect(screen.queryByText('Trước')).not.toBeInTheDocument();
    });
  });

  describe('Date Formatting', () => {
    it('formats dates in Vietnamese locale', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
        />
      );

      // Check for Vietnamese date format (dd/mm/yyyy)
      const dateElements = screen.getAllByText(/\d{1,2}\/\d{1,2}\/\d{4}/);
      expect(dateElements.length).toBeGreaterThan(0);
    });

    it('formats times correctly', () => {
      render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
        />
      );

      // Check for time format (hh:mm)
      const timeElements = screen.getAllByText(/\d{2}:\d{2}/);
      expect(timeElements.length).toBeGreaterThan(0);
    });
  });

  describe('Accessibility', () => {
    it('renders proper table structure', () => {
      const { container } = render(
        <AttendanceHistoryTable
          data={mockAttendanceRecords}
          isLoading={false}
        />
      );

      const table = container.querySelector('table');
      expect(table).toBeInTheDocument();
    });
  });
});
