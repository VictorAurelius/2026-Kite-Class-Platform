/**
 * Integration tests for Create Teacher Page.
 * Tests form submission, validation, API errors, and navigation.
 *
 * @since 2026-02-23
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import NewTeacherPage from '../page';
import { useRouter } from 'next/navigation';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import {
  mockDuplicateEmailError,
  mockValidationError,
} from '@/test/page-test-utils';

vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
  usePathname: vi.fn(() => '/teachers/new'),
}));

describe('NewTeacherPage Integration', () => {
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
    vi.mocked(useRouter).mockReturnValue(mockRouter as any);
    mockPush.mockClear();
  });

  it('should render create teacher form', () => {
    render(<NewTeacherPage />);

    expect(screen.getByText('Thêm giáo viên')).toBeInTheDocument();
    expect(
      screen.getByText('Nhập thông tin giáo viên mới')
    ).toBeInTheDocument();

    // Check form fields are present
    expect(screen.getByLabelText(/tên giáo viên/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/số điện thoại/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/chuyên môn/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/bằng cấp \/ chứng chỉ/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/số năm kinh nghiệm/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/giới thiệu/i)).toBeInTheDocument();
  });

  it('should create teacher successfully and redirect', async () => {
    const user = userEvent.setup();
    render(<NewTeacherPage />);

    // Mock successful creation
    server.use(
      http.post('*/api/v1/teachers', () => {
        return HttpResponse.json(
          {
            id: 1,
            name: 'Nguyễn Thị Test',
            email: 'test@example.com',
            phoneNumber: '0901234567',
            specialization: 'Toán học',
            status: 'ACTIVE',
            createdAt: '2026-02-23T00:00:00Z',
            updatedAt: '2026-02-23T00:00:00Z',
          },
          { status: 201 }
        );
      })
    );

    // Fill in required fields
    await user.type(screen.getByLabelText(/tên giáo viên/i), 'Nguyễn Thị Test');
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo mới/i });
    await user.click(submitButton);

    // Wait for success toast
    await waitFor(() => {
      expect(screen.getByText(/đã tạo giáo viên mới/i)).toBeInTheDocument();
    });

    // Verify redirect to teachers list
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/teachers');
    });
  });

  it('should show validation errors for empty form', async () => {
    const user = userEvent.setup();
    render(<NewTeacherPage />);

    // Submit without filling fields
    const submitButton = screen.getByRole('button', { name: /tạo mới/i });
    await user.click(submitButton);

    // Verify validation errors appear (from zod schema)
    await waitFor(() => {
      expect(screen.getByText(/tên không được để trống/i)).toBeInTheDocument();
      expect(screen.getByText(/email không hợp lệ/i)).toBeInTheDocument();
      // Phone and other fields are optional, no error expected
    });

    // Should not redirect
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('should handle duplicate email error (409)', async () => {
    const user = userEvent.setup();
    const duplicateEmail = 'duplicate@example.com';

    mockDuplicateEmailError('*/api/v1/teachers', duplicateEmail);

    render(<NewTeacherPage />);

    // Fill in form with duplicate email
    await user.type(screen.getByLabelText(/tên giáo viên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), duplicateEmail);
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo mới/i });
    await user.click(submitButton);

    // Verify error toast
    await waitFor(() => {
      expect(
        screen.getByText(
          new RegExp(`Email ${duplicateEmail} đã tồn tại trong hệ thống`, 'i')
        )
      ).toBeInTheDocument();
    });

    // Should not redirect
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('should handle validation error from API (400)', async () => {
    const user = userEvent.setup();

    mockValidationError('*/api/v1/teachers', {
      email: 'Email không hợp lệ',
      phoneNumber: 'Số điện thoại phải có 10 chữ số',
    });

    render(<NewTeacherPage />);

    // Fill in form with invalid data
    await user.type(screen.getByLabelText(/tên giáo viên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), 'invalid-email');
    await user.type(screen.getByLabelText(/số điện thoại/i), '123');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo mới/i });
    await user.click(submitButton);

    // Verify API validation errors shown in toast
    await waitFor(() => {
      expect(screen.getByText(/email không hợp lệ/i)).toBeInTheDocument();
      expect(
        screen.getByText(/số điện thoại phải có 10 chữ số/i)
      ).toBeInTheDocument();
    });

    // Should not redirect
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('should handle server error (500)', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('*/api/v1/teachers', () => {
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

    render(<NewTeacherPage />);

    // Fill in form
    await user.type(screen.getByLabelText(/tên giáo viên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo mới/i });
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
      http.post('*/api/v1/teachers', async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
        return HttpResponse.json(
          {
            id: 1,
            name: 'Test',
            email: 'test@example.com',
            phoneNumber: '0901234567',
            specialization: 'Toán học',
            status: 'ACTIVE',
            createdAt: '2026-02-23T00:00:00Z',
            updatedAt: '2026-02-23T00:00:00Z',
          },
          { status: 201 }
        );
      })
    );

    render(<NewTeacherPage />);

    // Fill in form
    await user.type(screen.getByLabelText(/tên giáo viên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    const submitButton = screen.getByRole('button', { name: /tạo mới/i });

    // Submit form
    await user.click(submitButton);

    // Button should be disabled immediately
    expect(submitButton).toBeDisabled();

    // Wait for submission to complete
    await waitFor(
      () => {
        expect(mockPush).toHaveBeenCalledWith('/teachers');
      },
      { timeout: 2000 }
    );
  });

  it('should validate email format', async () => {
    const user = userEvent.setup();
    render(<NewTeacherPage />);

    // Fill in form with invalid email
    await user.type(screen.getByLabelText(/tên giáo viên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), 'not-an-email');
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo mới/i });
    await user.click(submitButton);

    // Verify email validation error
    await waitFor(() => {
      expect(screen.getByText(/email không hợp lệ/i)).toBeInTheDocument();
    });
  });

  it('should validate experience years is non-negative', async () => {
    const user = userEvent.setup();
    render(<NewTeacherPage />);

    // Fill in form with negative experience years
    await user.type(screen.getByLabelText(/tên giáo viên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/số năm kinh nghiệm/i), '-5');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo mới/i });
    await user.click(submitButton);

    // Verify validation error
    await waitFor(() => {
      expect(screen.getByText(/kinh nghiệm phải >= 0/i)).toBeInTheDocument();
    });
  });
});
