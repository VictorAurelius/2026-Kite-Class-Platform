/**
 * Integration tests for Courses List Page.
 * Tests page-level integration: component + hooks + API + navigation.
 *
 * @since 2026-02-23
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import CoursesPage from '../page';
import {
  mockConfirm,
  mock500,
  mockEmptyList,
  waitForLoadingToFinish,
} from '@/test/page-test-utils';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('CoursesListPage Integration', () => {
  beforeEach(() => {
    // Reset window.confirm mock
    window.confirm = vi.fn();
  });

  it('should load and display courses list', async () => {
    render(<CoursesPage />);

    // Data loads
    await waitFor(() => {
      expect(screen.getByText('Tiếng Anh Giao Tiếp Cơ Bản')).toBeInTheDocument();
      expect(screen.getByText('ENG-B1-001')).toBeInTheDocument();
    });

    // Header is visible
    expect(screen.getByText('Khóa học')).toBeInTheDocument();
    expect(
      screen.getByText('Quản lý danh sách khóa học của trung tâm')
    ).toBeInTheDocument();

    // Add button is visible
    expect(screen.getByRole('link', { name: /thêm khóa học/i })).toHaveAttribute(
      'href',
      '/courses/new'
    );
  });

  it('should search courses with debounced query', async () => {
    const user = userEvent.setup();
    render(<CoursesPage />);

    // Wait for initial data to load
    await waitFor(() => screen.getByText('Tiếng Anh Giao Tiếp Cơ Bản'));

    // Type in search input
    const searchInput = screen.getByPlaceholderText(/tìm kiếm theo tên, mã khóa học/i);
    expect(searchInput).toBeInTheDocument();

    await user.type(searchInput, 'Toán');

    // Wait for debounce (500ms) + API call
    await waitFor(
      () => {
        // Should filter to only matching results
        expect(screen.queryByText('Tiếng Anh Giao Tiếp Cơ Bản')).not.toBeInTheDocument();
      },
      { timeout: 1000 }
    );
  });

  it('should display empty state when no courses', async () => {
    mockEmptyList('*/api/v1/courses');

    render(<CoursesPage />);

    await waitForLoadingToFinish();

    // DataTable should show "No results found." message
    await waitFor(() => {
      expect(screen.getByText('No results found.')).toBeInTheDocument();
    });
  });

  it('should handle API error and show error alert', async () => {
    mock500('*/api/v1/courses');

    render(<CoursesPage />);

    // Wait for error to display
    await waitFor(() => {
      expect(screen.getByText('Lỗi tải dữ liệu')).toBeInTheDocument();
      expect(
        screen.getByText('Không thể tải danh sách khóa học. Vui lòng thử lại.')
      ).toBeInTheDocument();
    });

    // Should not show loading spinner or data
    expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should delete course with confirmation', async () => {
    const user = userEvent.setup();
    mockConfirm(true); // User confirms deletion

    render(<CoursesPage />);

    // Wait for data to load
    await waitFor(() => screen.getByText('Tiếng Anh Giao Tiếp Cơ Bản'));

    // Mock successful delete
    server.use(
      http.delete('*/api/v1/courses/:id', () => {
        return new HttpResponse(null, { status: 204 });
      })
    );

    // Find delete button by icon (Actions column)
    const allButtons = screen.getAllByRole('button');
    // Filter to get only icon buttons (size="icon" buttons without text)
    const iconButtons = allButtons.filter(btn => !btn.textContent);
    // Find first delete button in table
    const firstDeleteButton = iconButtons.find(btn =>
      btn.querySelector('svg')?.classList.contains('lucide-trash-2')
    ) || iconButtons[2];
    expect(firstDeleteButton).toBeDefined();

    await user.click(firstDeleteButton!);

    // Verify confirmation was called
    expect(window.confirm).toHaveBeenCalledWith(
      'Bạn có chắc chắn muốn xóa khóa học này?'
    );

    // Wait for toast notification (from mutation success)
    await waitFor(() => {
      // Toast should show success message
      expect(screen.getByText(/đã xóa khóa học/i)).toBeInTheDocument();
    });
  });

  it('should not delete course when confirmation is cancelled', async () => {
    const user = userEvent.setup();

    // Reset and mock confirm to return false
    window.confirm = vi.fn(() => false);

    render(<CoursesPage />);

    // Wait for data to load
    await waitFor(() => screen.getByText('Tiếng Anh Giao Tiếp Cơ Bản'));

    // Find delete button by icon
    const allButtons = screen.getAllByRole('button');
    const iconButtons = allButtons.filter(btn => !btn.textContent);
    const firstDeleteButton = iconButtons.find(btn =>
      btn.querySelector('svg')?.classList.contains('lucide-trash-2')
    ) || iconButtons[2];
    expect(firstDeleteButton).toBeDefined();

    await user.click(firstDeleteButton!);

    // Verify confirmation was called
    expect(window.confirm).toHaveBeenCalledWith(
      'Bạn có chắc chắn muốn xóa khóa học này?'
    );

    // Course should still be in the list
    expect(screen.getByText('Tiếng Anh Giao Tiếp Cơ Bản')).toBeInTheDocument();

    // Should not show toast notification
    expect(
      screen.queryByText(/đã xóa khóa học/i)
    ).not.toBeInTheDocument();
  });

  it('should display search input placeholder', () => {
    render(<CoursesPage />);

    const searchInput = screen.getByPlaceholderText(/tìm kiếm theo tên, mã khóa học/i);
    expect(searchInput).toBeInTheDocument();
  });

  it('should render page title and description', () => {
    render(<CoursesPage />);

    expect(screen.getByText('Khóa học')).toBeInTheDocument();
    expect(
      screen.getByText('Quản lý danh sách khóa học của trung tâm')
    ).toBeInTheDocument();
  });

  it('should have working add button link', () => {
    render(<CoursesPage />);

    const addButton = screen.getByRole('link', { name: /thêm khóa học/i });
    expect(addButton).toHaveAttribute('href', '/courses/new');
  });
});
