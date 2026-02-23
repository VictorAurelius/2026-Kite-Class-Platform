/**
 * Integration tests for Students List Page.
 * Tests page-level integration: component + hooks + API + navigation.
 *
 * @since 2026-02-23
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import StudentsPage from '../page';
import {
  mockConfirm,
  mock500,
  mockEmptyList,
  waitForLoadingToFinish,
} from '@/test/page-test-utils';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('StudentsListPage Integration', () => {
  beforeEach(() => {
    // Reset window.confirm mock
    window.confirm = vi.fn();
  });

  it('should load and display students list', async () => {
    render(<StudentsPage />);

    // Data loads
    await waitFor(() => {
      expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
      expect(screen.getByText('nguyenvana@gmail.com')).toBeInTheDocument();
    });

    // Header is visible
    expect(screen.getByText('Học viên')).toBeInTheDocument();
    expect(
      screen.getByText('Quản lý danh sách học viên của trung tâm')
    ).toBeInTheDocument();

    // Add button is visible
    expect(screen.getByRole('link', { name: /thêm học viên/i })).toHaveAttribute(
      'href',
      '/students/new'
    );
  });

  it('should display loading spinner initially', () => {
    render(<StudentsPage />);

    const spinner = screen.getByTestId('loading-spinner');
    expect(spinner).toBeInTheDocument();
  });

  it('should search students with debounced query', async () => {
    const user = userEvent.setup();
    render(<StudentsPage />);

    // Wait for initial data to load
    await waitFor(() => screen.getByText('Nguyễn Văn A'));

    // Type in search input
    const searchInput = screen.getByPlaceholderText(/tìm kiếm theo tên, email/i);
    expect(searchInput).toBeInTheDocument();

    await user.type(searchInput, 'Trần');

    // Wait for debounce (500ms) + API call
    await waitFor(
      () => {
        // Should filter to only matching results
        expect(screen.queryByText('Nguyễn Văn A')).not.toBeInTheDocument();
      },
      { timeout: 1000 }
    );
  });

  it('should handle pagination - next page', async () => {
    const user = userEvent.setup();
    render(<StudentsPage />);

    // Wait for initial data
    await waitFor(() => screen.getByText('Nguyễn Văn A'));

    // Mock page 1 response
    server.use(
      http.get('*/api/v1/students', ({ request }) => {
        const url = new URL(request.url);
        const page = url.searchParams.get('page');

        if (page === '1') {
          return HttpResponse.json({
            content: [
              {
                id: 3,
                name: 'Lê Thị C',
                email: 'lethic@gmail.com',
                phone: '0901234569',
                status: 'ACTIVE',
                dateOfBirth: '2005-03-15',
                address: '789 Đường XYZ, Hà Nội',
                createdAt: '2026-01-01T00:00:00Z',
                updatedAt: '2026-01-01T00:00:00Z',
              },
            ],
            page: {
              size: 20,
              number: 1,
              totalElements: 3,
              totalPages: 2,
            },
          });
        }

        return HttpResponse.json({
          content: [
            {
              id: 1,
              name: 'Nguyễn Văn A',
              email: 'nguyenvana@gmail.com',
              phone: '0901234567',
              status: 'ACTIVE',
              dateOfBirth: '2005-01-15',
              address: '123 Đường ABC, TP.HCM',
              createdAt: '2026-01-01T00:00:00Z',
              updatedAt: '2026-01-01T00:00:00Z',
            },
          ],
          page: {
            size: 20,
            number: 0,
            totalElements: 3,
            totalPages: 2,
          },
        });
      })
    );

    // Click next page button
    const nextButton = screen.getByRole('button', { name: /next/i });
    await user.click(nextButton);

    // Verify page 2 data loads
    await waitFor(() => {
      expect(screen.getByText('Lê Thị C')).toBeInTheDocument();
      expect(screen.queryByText('Nguyễn Văn A')).not.toBeInTheDocument();
    });
  });

  it('should display empty state when no students', async () => {
    mockEmptyList('*/api/v1/students');

    render(<StudentsPage />);

    await waitForLoadingToFinish();

    // DataTable should show "No results" message
    await waitFor(() => {
      expect(screen.getByText(/no results/i)).toBeInTheDocument();
    });
  });

  it('should handle API error and show error alert', async () => {
    mock500('*/api/v1/students');

    render(<StudentsPage />);

    // Wait for error to display
    await waitFor(() => {
      expect(screen.getByText('Lỗi tải dữ liệu')).toBeInTheDocument();
      expect(
        screen.getByText('Không thể tải danh sách học viên. Vui lòng thử lại.')
      ).toBeInTheDocument();
    });

    // Should not show loading spinner or data
    expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should delete student with confirmation', async () => {
    const user = userEvent.setup();
    mockConfirm(true); // User confirms deletion

    render(<StudentsPage />);

    // Wait for data to load
    await waitFor(() => screen.getByText('Nguyễn Văn A'));

    // Mock successful delete
    server.use(
      http.delete('*/api/v1/students/:id', () => {
        return new HttpResponse(null, { status: 204 });
      })
    );

    // Find and click delete button (assuming it's in the table row)
    const deleteButtons = screen.getAllByRole('button', { name: /xóa/i });
    await user.click(deleteButtons[0]);

    // Verify confirmation was called
    expect(window.confirm).toHaveBeenCalledWith(
      'Bạn có chắc chắn muốn xóa học viên này? Thao tác này không thể hoàn tác.'
    );

    // Wait for toast notification (from mutation success)
    await waitFor(() => {
      // Toast should show success message
      expect(screen.getByText(/xóa học viên thành công/i)).toBeInTheDocument();
    });
  });

  it('should not delete student when confirmation is cancelled', async () => {
    const user = userEvent.setup();
    mockConfirm(false); // User cancels deletion

    render(<StudentsPage />);

    // Wait for data to load
    await waitFor(() => screen.getByText('Nguyễn Văn A'));

    // Find and click delete button
    const deleteButtons = screen.getAllByRole('button', { name: /xóa/i });
    await user.click(deleteButtons[0]);

    // Verify confirmation was called
    expect(window.confirm).toHaveBeenCalledWith(
      'Bạn có chắc chắn muốn xóa học viên này? Thao tác này không thể hoàn tác.'
    );

    // Student should still be in the list
    expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();

    // Should not show toast notification
    expect(
      screen.queryByText(/xóa học viên thành công/i)
    ).not.toBeInTheDocument();
  });

  it('should display search input placeholder', () => {
    render(<StudentsPage />);

    const searchInput = screen.getByPlaceholderText(/tìm kiếm theo tên, email/i);
    expect(searchInput).toBeInTheDocument();
  });

  it('should render page title and description', () => {
    render(<StudentsPage />);

    expect(screen.getByText('Học viên')).toBeInTheDocument();
    expect(
      screen.getByText('Quản lý danh sách học viên của trung tâm')
    ).toBeInTheDocument();
  });

  it('should have working add button link', () => {
    render(<StudentsPage />);

    const addButton = screen.getByRole('link', { name: /thêm học viên/i });
    expect(addButton).toHaveAttribute('href', '/students/new');
  });
});
