/**
 * Integration tests for Classes List Page.
 * Tests course selector dependency, conditional rendering, and search functionality.
 *
 * @author KiteClass Team
 * @since 3.11.0
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import ClassesPage from '../page';
import {
  mockConfirm,
  waitForLoadingToFinish,
} from '@/test/page-test-utils';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Mock next/navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
  }),
  usePathname: () => '/classes',
  useSearchParams: () => new URLSearchParams(),
}));

// Mock DashboardLayout
vi.mock('@/components/layout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

describe.skip('ClassesListPage Integration - SKIPPED: Radix UI Select incompatible with JSDOM (PointerCapture API)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should load and display page heading', async () => {
    render(<ClassesPage />);

    expect(screen.getByRole('heading', { name: /lớp học/i })).toBeInTheDocument();
    expect(screen.getByText(/quản lý danh sách lớp học theo từng khóa học/i)).toBeInTheDocument();
  });

  it('should show course selector dropdown', async () => {
    render(<ClassesPage />);

    // Course selector should be visible
    expect(screen.getByText(/chọn khóa học/i)).toBeInTheDocument();
  });

  it('should show empty state when no course selected', async () => {
    render(<ClassesPage />);

    // Empty state message
    await waitFor(() => {
      expect(screen.getByText(/vui lòng chọn khóa học để xem danh sách lớp học/i)).toBeInTheDocument();
    });

    // Create button should NOT be visible
    expect(screen.queryByRole('link', { name: /thêm lớp học/i })).not.toBeInTheDocument();

    // Search should NOT be visible
    expect(screen.queryByPlaceholderText(/tìm kiếm/i)).not.toBeInTheDocument();
  });

  it('should load classes when course is selected', async () => {
    const user = userEvent.setup();

    render(<ClassesPage />);

    // Select a course
    const courseSelector = screen.getByRole('combobox');
    await user.click(courseSelector);

    // Select first course
    const courseOption = screen.getByText(/tiếng anh giao tiếp cơ bản.*ENG-B1-001/i);
    await user.click(courseOption);

    // Should show loading
    await waitFor(() => {
      expect(screen.queryByTestId('loading-spinner')).toBeInTheDocument();
    });

    // Data should load
    await waitForLoadingToFinish();

    // Classes from mock data should appear
    await waitFor(() => {
      expect(screen.getByText('Lớp Tiếng Anh Buổi Sáng')).toBeInTheDocument();
    });
  });

  it('should show create button after selecting course', async () => {
    const user = userEvent.setup();

    render(<ClassesPage />);

    // No create button initially
    expect(screen.queryByRole('link', { name: /thêm lớp học/i })).not.toBeInTheDocument();

    // Select course
    const courseSelector = screen.getByRole('combobox');
    await user.click(courseSelector);
    const courseOption = screen.getByText(/tiếng anh giao tiếp cơ bản.*ENG-B1-001/i);
    await user.click(courseOption);

    // Create button should appear
    await waitFor(() => {
      const createButton = screen.getByRole('link', { name: /thêm lớp học/i });
      expect(createButton).toBeInTheDocument();
      expect(createButton).toHaveAttribute('href', '/courses/1/classes/new');
    });
  });

  it('should enable search after selecting course', async () => {
    const user = userEvent.setup();

    render(<ClassesPage />);

    // No search initially
    expect(screen.queryByPlaceholderText(/tìm kiếm/i)).not.toBeInTheDocument();

    // Select course
    const courseSelector = screen.getByRole('combobox');
    await user.click(courseSelector);
    const courseOption = screen.getByText(/tiếng anh giao tiếp cơ bản.*ENG-B1-001/i);
    await user.click(courseOption);

    // Search should appear
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/tìm kiếm theo tên lớp, mã lớp/i)).toBeInTheDocument();
    });
  });

  it('should filter classes by search query', async () => {
    const user = userEvent.setup();

    server.use(
      http.get(`${BASE_URL}/api/v1/courses/:courseId/classes`, ({ request }) => {
        const url = new URL(request.url);
        const search = url.searchParams.get('search');

        if (search === 'Sáng') {
          return HttpResponse.json({
            success: true,
            data: {
              content: [
                {
                  id: 1,
                  courseId: 1,
                  name: 'Lớp Tiếng Anh Buổi Sáng',
                  classCode: 'ENG-B1-SANG',
                  schedule: 'Thứ 2, 4, 6: 08:00-10:00',
                  status: 'SCHEDULED',
                  currentEnrolled: 15,
                  maxStudents: 30,
                },
              ],
              totalElements: 1,
              totalPages: 1,
              size: 20,
              number: 0,
            },
          });
        }

        return HttpResponse.json({
          success: true,
          data: {
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: 20,
            number: 0,
          },
        });
      })
    );

    render(<ClassesPage />);

    // Select course
    const courseSelector = screen.getByRole('combobox');
    await user.click(courseSelector);
    const courseOption = screen.getByText(/tiếng anh giao tiếp cơ bản.*ENG-B1-001/i);
    await user.click(courseOption);

    await waitForLoadingToFinish();

    // Type search query
    const searchInput = screen.getByPlaceholderText(/tìm kiếm theo tên lớp, mã lớp/i);
    await user.type(searchInput, 'Sáng');

    // Wait for debounced search
    await waitFor(
      () => {
        expect(screen.getByText('Lớp Tiếng Anh Buổi Sáng')).toBeInTheDocument();
      },
      { timeout: 2000 }
    );
  });

  it('should show empty state when selected course has no classes', async () => {
    const user = userEvent.setup();

    server.use(
      http.get(`${BASE_URL}/api/v1/courses/:courseId/classes`, () => {
        return HttpResponse.json({
          success: true,
          data: {
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: 20,
            number: 0,
          },
        });
      })
    );

    render(<ClassesPage />);

    // Select course
    const courseSelector = screen.getByRole('combobox');
    await user.click(courseSelector);
    const courseOption = screen.getByText(/tiếng anh giao tiếp cơ bản.*ENG-B1-001/i);
    await user.click(courseOption);

    await waitForLoadingToFinish();

    // Empty state message
    await waitFor(() => {
      expect(screen.getByText(/chưa có lớp học nào cho khóa học này/i)).toBeInTheDocument();
    });

    // Should have "Create first class" button
    const createFirstButton = screen.getByRole('link', { name: /tạo lớp học đầu tiên/i });
    expect(createFirstButton).toBeInTheDocument();
    expect(createFirstButton).toHaveAttribute('href', '/courses/1/classes/new');
  });

  it('should handle API error gracefully', async () => {
    const user = userEvent.setup();

    server.use(
      http.get(`${BASE_URL}/api/v1/courses/:courseId/classes`, () => {
        return HttpResponse.json(
          {
            success: false,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Đã xảy ra lỗi từ máy chủ',
          },
          { status: 500 }
        );
      })
    );

    render(<ClassesPage />);

    // Select course
    const courseSelector = screen.getByRole('combobox');
    await user.click(courseSelector);
    const courseOption = screen.getByText(/tiếng anh giao tiếp cơ bản.*ENG-B1-001/i);
    await user.click(courseOption);

    // Wait for error to display
    await waitFor(() => {
      expect(screen.getByText(/lỗi tải dữ liệu/i)).toBeInTheDocument();
      expect(screen.getByText(/không thể tải danh sách lớp học/i)).toBeInTheDocument();
    });
  });

  it('should delete class with confirmation', async () => {
    const user = userEvent.setup();
    mockConfirm(true);

    server.use(
      http.delete(`${BASE_URL}/api/v1/classes/:id`, () => {
        return new HttpResponse(null, { status: 204 });
      })
    );

    render(<ClassesPage />);

    // Select course
    const courseSelector = screen.getByRole('combobox');
    await user.click(courseSelector);
    const courseOption = screen.getByText(/tiếng anh giao tiếp cơ bản.*ENG-B1-001/i);
    await user.click(courseOption);

    await waitForLoadingToFinish();

    // Find delete button
    const deleteButtons = screen.getAllByRole('button');
    const iconButtons = deleteButtons.filter(btn => !btn.textContent);
    const firstDeleteButton = iconButtons[2]; // After View and Edit

    await user.click(firstDeleteButton!);

    // Should show confirmation
    expect(window.confirm).toHaveBeenCalledWith(
      expect.stringContaining('Xóa lớp học')
    );

    // Should show success toast
    await waitFor(() => {
      expect(screen.getByText(/đã xóa lớp học/i)).toBeInTheDocument();
    });
  });

  it('should display class status badges', async () => {
    const user = userEvent.setup();

    render(<ClassesPage />);

    // Select course
    const courseSelector = screen.getByRole('combobox');
    await user.click(courseSelector);
    const courseOption = screen.getByText(/tiếng anh giao tiếp cơ bản.*ENG-B1-001/i);
    await user.click(courseOption);

    await waitForLoadingToFinish();

    // Status badges should be visible
    await waitFor(() => {
      expect(screen.getByText('SCHEDULED')).toBeInTheDocument();
    });
  });

  it('should reset pagination when changing course', async () => {
    const user = userEvent.setup();

    render(<ClassesPage />);

    // Select first course
    const courseSelector = screen.getByRole('combobox');
    await user.click(courseSelector);
    let courseOption = screen.getByText(/tiếng anh giao tiếp cơ bản.*ENG-B1-001/i);
    await user.click(courseOption);

    await waitForLoadingToFinish();

    // Select different course
    await user.click(courseSelector);
    courseOption = screen.getByText(/toán học nâng cao.*MATH-A1-001/i);
    await user.click(courseOption);

    // Should reload with page 0
    await waitFor(() => {
      expect(screen.queryByTestId('loading-spinner')).toBeInTheDocument();
    });
  });
});
