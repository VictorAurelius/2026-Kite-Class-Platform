/**
 * Integration tests for Create Student Page.
 * Tests form submission, validation, API errors, and navigation.
 *
 * @since 2026-02-23
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import NewStudentPage from '../page';
import { useRouter } from 'next/navigation';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import {
  mockDuplicateEmailError,
  mockValidationError,
} from '@/test/page-test-utils';

vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
}));

describe('NewStudentPage Integration', () => {
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

  it('should render create student form', () => {
    render(<NewStudentPage />);

    expect(screen.getByText('Thêm học viên mới')).toBeInTheDocument();
    expect(
      screen.getByText('Nhập thông tin học viên để tạo hồ sơ mới')
    ).toBeInTheDocument();

    // Check form fields are present
    expect(screen.getByLabelText(/họ và tên/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/số điện thoại/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/ngày sinh/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/địa chỉ/i)).toBeInTheDocument();
  });

  it('should create student successfully and redirect', async () => {
    const user = userEvent.setup();
    render(<NewStudentPage />);

    // Mock successful creation
    server.use(
      http.post('*/api/v1/students', () => {
        return HttpResponse.json(
          {
            id: 1,
            name: 'Nguyễn Văn Test',
            email: 'test@example.com',
            phone: '0901234567',
            status: 'ACTIVE',
            dateOfBirth: '2005-01-15',
            address: '123 Test Street',
            createdAt: '2026-02-23T00:00:00Z',
            updatedAt: '2026-02-23T00:00:00Z',
          },
          { status: 201 }
        );
      })
    );

    // Fill in required fields
    await user.type(screen.getByLabelText(/họ và tên/i), 'Nguyễn Văn Test');
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo học viên/i });
    await user.click(submitButton);

    // Wait for success toast
    await waitFor(() => {
      expect(screen.getByText(/tạo học viên thành công/i)).toBeInTheDocument();
    });

    // Verify redirect to students list
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/students');
    });
  });

  it('should show validation errors for empty form', async () => {
    const user = userEvent.setup();
    render(<NewStudentPage />);

    // Submit without filling fields
    const submitButton = screen.getByRole('button', { name: /tạo học viên/i });
    await user.click(submitButton);

    // Verify validation errors appear (from zod schema)
    await waitFor(() => {
      expect(screen.getByText(/họ và tên là bắt buộc/i)).toBeInTheDocument();
      expect(screen.getByText(/email là bắt buộc/i)).toBeInTheDocument();
      expect(screen.getByText(/số điện thoại là bắt buộc/i)).toBeInTheDocument();
    });

    // Should not redirect
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('should handle duplicate email error (409)', async () => {
    const user = userEvent.setup();
    const duplicateEmail = 'duplicate@example.com';

    mockDuplicateEmailError('*/api/v1/students', duplicateEmail);

    render(<NewStudentPage />);

    // Fill in form with duplicate email
    await user.type(screen.getByLabelText(/họ và tên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), duplicateEmail);
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo học viên/i });
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

    mockValidationError('*/api/v1/students', {
      email: 'Email không hợp lệ',
      phone: 'Số điện thoại phải có 10 chữ số',
    });

    render(<NewStudentPage />);

    // Fill in form with invalid data
    await user.type(screen.getByLabelText(/họ và tên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), 'invalid-email');
    await user.type(screen.getByLabelText(/số điện thoại/i), '123');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo học viên/i });
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
      http.post('*/api/v1/students', () => {
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

    render(<NewStudentPage />);

    // Fill in form
    await user.type(screen.getByLabelText(/họ và tên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo học viên/i });
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
      http.post('*/api/v1/students', async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
        return HttpResponse.json(
          {
            id: 1,
            name: 'Test',
            email: 'test@example.com',
            phone: '0901234567',
            status: 'ACTIVE',
            dateOfBirth: '2005-01-15',
            address: '123 Test',
            createdAt: '2026-02-23T00:00:00Z',
            updatedAt: '2026-02-23T00:00:00Z',
          },
          { status: 201 }
        );
      })
    );

    render(<NewStudentPage />);

    // Fill in form
    await user.type(screen.getByLabelText(/họ và tên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    const submitButton = screen.getByRole('button', { name: /tạo học viên/i });

    // Submit form
    await user.click(submitButton);

    // Button should be disabled immediately
    expect(submitButton).toBeDisabled();

    // Wait for submission to complete
    await waitFor(
      () => {
        expect(mockPush).toHaveBeenCalledWith('/students');
      },
      { timeout: 2000 }
    );
  });

  it('should validate email format', async () => {
    const user = userEvent.setup();
    render(<NewStudentPage />);

    // Fill in form with invalid email
    await user.type(screen.getByLabelText(/họ và tên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), 'not-an-email');
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo học viên/i });
    await user.click(submitButton);

    // Verify email validation error
    await waitFor(() => {
      expect(screen.getByText(/email không hợp lệ/i)).toBeInTheDocument();
    });
  });

  it('should validate phone number format', async () => {
    const user = userEvent.setup();
    render(<NewStudentPage />);

    // Fill in form with invalid phone
    await user.type(screen.getByLabelText(/họ và tên/i), 'Test User');
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/số điện thoại/i), '123'); // Too short

    // Submit form
    const submitButton = screen.getByRole('button', { name: /tạo học viên/i });
    await user.click(submitButton);

    // Verify phone validation error
    await waitFor(() => {
      expect(
        screen.getByText(/số điện thoại phải có 10 chữ số/i)
      ).toBeInTheDocument();
    });
  });
});
