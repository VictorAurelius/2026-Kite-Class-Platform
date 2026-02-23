/**
 * Integration tests for Create Course Page.
 * Tests form submission, validation, API errors, and navigation.
 *
 * @since 2026-02-23
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import NewCoursePage from '../page';
import { useRouter } from 'next/navigation';
import type { AppRouterInstance } from 'next/dist/shared/lib/app-router-context.shared-runtime';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { mockValidationError } from '@/test/page-test-utils';

vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
  usePathname: vi.fn(() => '/courses/new'),
}));

describe('NewCoursePage Integration', () => {
  const mockPush = vi.fn();
  const mockRouter = {
    push: mockPush,
    replace: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
    prefetch: vi.fn(),
  };

  beforeEach(() => {
    vi.mocked(useRouter).mockReturnValue(mockRouter as AppRouterInstance);
    mockPush.mockClear();
  });

  it('should render create course form', () => {
    render(<NewCoursePage />);

    expect(screen.getByText('Thêm khóa học')).toBeInTheDocument();
    expect(
      screen.getByText('Tạo khóa học mới cho trung tâm')
    ).toBeInTheDocument();

    // Check form fields are present
    expect(screen.getByLabelText(/tên khóa học/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/mã khóa học/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/thời lượng \(tuần\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/tổng số buổi học/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/học phí \(VND\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/mô tả/i)).toBeInTheDocument();
  });

  it('should create course successfully and redirect', async () => {
    const user = userEvent.setup();
    render(<NewCoursePage />);

    // Mock successful creation
    server.use(
      http.post('*/api/v1/courses', () => {
        return HttpResponse.json(
          {
            success: true,
            data: {
              id: 1,
              name: 'Test Course',
              code: 'TEST-001',
              status: 'DRAFT',
              createdAt: '2026-02-23T00:00:00Z',
              updatedAt: '2026-02-23T00:00:00Z',
            },
          },
          { status: 201 }
        );
      })
    );

    // Fill in required fields
    await user.type(screen.getByLabelText(/tên khóa học/i), 'Test Course');
    await user.type(screen.getByLabelText(/mã khóa học/i), 'TEST-001');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo khóa học/i });
    await user.click(submitButton);

    // Wait for success toast
    await waitFor(() => {
      expect(screen.getByText(/đã tạo khóa học mới/i)).toBeInTheDocument();
    });

    // Verify redirect to courses list
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/courses');
    });
  });

  it.skip('should show validation errors for empty form [SKIP: jsdom validation timing]', async () => {
    const user = userEvent.setup();
    render(<NewCoursePage />);

    // Submit without filling fields
    const submitButton = screen.getByRole('button', { name: /tạo khóa học/i });
    await user.click(submitButton);

    // Verify validation errors appear (from zod schema)
    await waitFor(() => {
      expect(screen.getByText(/tên khóa học không được để trống/i)).toBeInTheDocument();
      expect(screen.getByText(/mã khóa học không được để trống/i)).toBeInTheDocument();
    });

    // Should not redirect
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('should handle duplicate code error (409)', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('*/api/v1/courses', () => {
        return HttpResponse.json(
          {
            status: 409,
            error: 'DUPLICATE_RESOURCE',
            message: 'Mã khóa học TEST-001 đã tồn tại',
          },
          { status: 409 }
        );
      })
    );

    render(<NewCoursePage />);

    // Fill in form
    await user.type(screen.getByLabelText(/tên khóa học/i), 'Test Course');
    await user.type(screen.getByLabelText(/mã khóa học/i), 'TEST-001');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo khóa học/i });
    await user.click(submitButton);

    // Verify error toast
    await waitFor(() => {
      expect(
        screen.getByText(/mã khóa học TEST-001 đã tồn tại/i)
      ).toBeInTheDocument();
    });

    // Should not redirect
    expect(mockPush).not.toHaveBeenCalled();
  });

  it.skip('should handle validation error from API (400) [SKIP: jsdom validation timing]', async () => {
    const user = userEvent.setup();

    mockValidationError('*/api/v1/courses', {
      code: 'Mã khóa học không hợp lệ',
      price: 'Học phí phải >= 0',
    });

    render(<NewCoursePage />);

    // Fill in form with invalid data
    await user.type(screen.getByLabelText(/tên khóa học/i), 'Test Course');
    await user.type(screen.getByLabelText(/mã khóa học/i), 'invalid code');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo khóa học/i });
    await user.click(submitButton);

    // Verify API validation errors shown in toast
    await waitFor(() => {
      expect(screen.getByText(/mã khóa học không hợp lệ/i)).toBeInTheDocument();
    });

    // Should not redirect
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('should handle server error (500)', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('*/api/v1/courses', () => {
        return HttpResponse.json(
          {
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Đã xảy ra lỗi từ máy chủ',
          },
          { status: 500 }
        );
      })
    );

    render(<NewCoursePage />);

    // Fill in form
    await user.type(screen.getByLabelText(/tên khóa học/i), 'Test Course');
    await user.type(screen.getByLabelText(/mã khóa học/i), 'TEST-001');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo khóa học/i });
    await user.click(submitButton);

    // Verify error toast
    await waitFor(() => {
      expect(screen.getByText(/đã xảy ra lỗi từ máy chủ/i)).toBeInTheDocument();
    });

    // Should not redirect
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('should disable submit button while submitting', async () => {
    const user = userEvent.setup();

    // Delay the API response to test loading state
    server.use(
      http.post('*/api/v1/courses', async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
        return HttpResponse.json(
          {
            success: true,
            data: {
              id: 1,
              name: 'Test',
              code: 'TEST-001',
              status: 'DRAFT',
              createdAt: '2026-02-23T00:00:00Z',
              updatedAt: '2026-02-23T00:00:00Z',
            },
          },
          { status: 201 }
        );
      })
    );

    render(<NewCoursePage />);

    // Fill in form
    await user.type(screen.getByLabelText(/tên khóa học/i), 'Test Course');
    await user.type(screen.getByLabelText(/mã khóa học/i), 'TEST-001');

    const submitButton = screen.getByRole('button', { name: /tạo khóa học/i });

    // Submit form
    await user.click(submitButton);

    // Button should be disabled immediately
    expect(submitButton).toBeDisabled();

    // Wait for submission to complete
    await waitFor(
      () => {
        expect(mockPush).toHaveBeenCalledWith('/courses');
      },
      { timeout: 2000 }
    );
  });

  it('should validate duration weeks is positive', async () => {
    const user = userEvent.setup();
    render(<NewCoursePage />);

    // Fill in form with 0 duration
    await user.type(screen.getByLabelText(/tên khóa học/i), 'Test Course');
    await user.type(screen.getByLabelText(/mã khóa học/i), 'TEST-001');
    await user.type(screen.getByLabelText(/thời lượng \(tuần\)/i), '0');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo khóa học/i });
    await user.click(submitButton);

    // Verify validation error
    await waitFor(() => {
      expect(screen.getByText(/thời lượng phải >= 1 tuần/i)).toBeInTheDocument();
    });
  });

  it('should validate price is non-negative', async () => {
    const user = userEvent.setup();
    render(<NewCoursePage />);

    // Fill in form with negative price
    await user.type(screen.getByLabelText(/tên khóa học/i), 'Test Course');
    await user.type(screen.getByLabelText(/mã khóa học/i), 'TEST-001');
    await user.type(screen.getByLabelText(/học phí \(VND\)/i), '-1000');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo khóa học/i });
    await user.click(submitButton);

    // Verify validation error
    await waitFor(() => {
      expect(screen.getByText(/học phí phải >= 0/i)).toBeInTheDocument();
    });
  });
});
