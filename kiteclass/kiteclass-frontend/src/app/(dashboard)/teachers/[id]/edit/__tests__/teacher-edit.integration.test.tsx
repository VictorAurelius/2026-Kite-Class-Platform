/**
 * Integration tests for Edit Teacher Page.
 * Tests form pre-filling, update, validation, and error handling.
 *
 * @author KiteClass Team
 * @since 3.11.0
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import EditTeacherPage from '../page';
import { useRouter } from 'next/navigation';
import type { AppRouterInstance } from 'next/dist/shared/lib/app-router-context.shared-runtime';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { mock404, mockDuplicateEmailError } from '@/test/page-test-utils';

vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
  usePathname: vi.fn(() => '/teachers/1/edit'),
}));

describe.skip('EditTeacherPage Integration - SKIPPED: Next.js 15 use(params) incompatible with RTL', () => {
  const mockPush = vi.fn();
  const mockRouter = {
    push: mockPush,
    replace: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
    prefetch: vi.fn(),
  };

  const mockParams = Promise.resolve({ id: '1' });

  beforeEach(() => {
    vi.mocked(useRouter).mockReturnValue(mockRouter as AppRouterInstance);
    mockPush.mockClear();
  });

  it('should load and display edit form with teacher data', async () => {
    render(<EditTeacherPage params={mockParams} />);

    // Shows loading initially
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();

    // Form loads with data
    await waitFor(() => {
      expect(screen.getByDisplayValue('Nguyễn Thị Giáo')).toBeInTheDocument();
      expect(screen.getByDisplayValue('giao.nguyen@kiteclass.local')).toBeInTheDocument();
    });

    // Page title
    expect(screen.getByText('Chỉnh sửa giáo viên')).toBeInTheDocument();
    expect(screen.getByText(/cập nhật thông tin cho/i)).toBeInTheDocument();
  });

  it('should pre-fill all form fields with existing data', async () => {
    render(<EditTeacherPage params={mockParams} />);

    await waitFor(() => screen.getByDisplayValue('Nguyễn Thị Giáo'));

    // All fields should be pre-filled
    expect(screen.getByDisplayValue('Nguyễn Thị Giáo')).toBeInTheDocument();
    expect(screen.getByDisplayValue('giao.nguyen@kiteclass.local')).toBeInTheDocument();
    expect(screen.getByDisplayValue('0901234567')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Toán học')).toBeInTheDocument();
  });

  it('should update teacher successfully and redirect', async () => {
    const user = userEvent.setup();

    server.use(
      http.put('*/api/v1/teachers/:id', async ({ request }) => {
        const body = await request.json() as Record<string, unknown>;
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            ...body,
            updatedAt: new Date().toISOString(),
          },
        });
      })
    );

    render(<EditTeacherPage params={mockParams} />);

    await waitFor(() => screen.getByDisplayValue('Nguyễn Thị Giáo'));

    // Change name
    const nameInput = screen.getByDisplayValue('Nguyễn Thị Giáo');
    await user.clear(nameInput);
    await user.type(nameInput, 'Nguyễn Thị Giáo Updated');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /cập nhật|update/i });
    await user.click(submitButton);

    // Should show success toast
    await waitFor(() => {
      expect(screen.getByText(/đã cập nhật giáo viên/i)).toBeInTheDocument();
    });

    // Should redirect to detail page
    expect(mockPush).toHaveBeenCalledWith('/teachers/1');
  });

  it('should validate required fields', async () => {
    const user = userEvent.setup();

    render(<EditTeacherPage params={mockParams} />);

    await waitFor(() => screen.getByDisplayValue('Nguyễn Thị Giáo'));

    // Clear required field (name)
    const nameInput = screen.getByDisplayValue('Nguyễn Thị Giáo');
    await user.clear(nameInput);

    // Submit form
    const submitButton = screen.getByRole('button', { name: /cập nhật|update/i });
    await user.click(submitButton);

    // Should show validation error
    await waitFor(() => {
      expect(screen.getByText(/tên giáo viên/i)).toBeInTheDocument();
    });
  });

  it('should validate email format', async () => {
    const user = userEvent.setup();

    render(<EditTeacherPage params={mockParams} />);

    await waitFor(() => screen.getByDisplayValue('giao.nguyen@kiteclass.local'));

    // Enter invalid email
    const emailInput = screen.getByDisplayValue('giao.nguyen@kiteclass.local');
    await user.clear(emailInput);
    await user.type(emailInput, 'invalid-email');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /cập nhật|update/i });
    await user.click(submitButton);

    // Should show validation error
    await waitFor(() => {
      expect(screen.getByText(/email không hợp lệ/i)).toBeInTheDocument();
    });
  });

  it('should handle duplicate email error (409)', async () => {
    const user = userEvent.setup();

    mockDuplicateEmailError('*/api/v1/teachers/*', 'test@example.com');

    render(<EditTeacherPage params={mockParams} />);

    await waitFor(() => screen.getByDisplayValue('giao.nguyen@kiteclass.local'));

    // Change email to duplicate
    const emailInput = screen.getByDisplayValue('giao.nguyen@kiteclass.local');
    await user.clear(emailInput);
    await user.type(emailInput, 'test@example.com');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /cập nhật|update/i });
    await user.click(submitButton);

    // Should show error toast
    await waitFor(() => {
      expect(screen.getByText(/email.*đã tồn tại/i)).toBeInTheDocument();
    });
  });

  it('should handle server error (500)', async () => {
    const user = userEvent.setup();

    server.use(
      http.put('*/api/v1/teachers/:id', () => {
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

    render(<EditTeacherPage params={mockParams} />);

    await waitFor(() => screen.getByDisplayValue('Nguyễn Thị Giáo'));

    // Submit form
    const submitButton = screen.getByRole('button', { name: /cập nhật|update/i });
    await user.click(submitButton);

    // Should show error toast
    await waitFor(() => {
      expect(screen.getByText(/đã xảy ra lỗi từ máy chủ/i)).toBeInTheDocument();
    });
  });

  it('should disable submit button while updating', async () => {
    const user = userEvent.setup();

    // Mock slow response
    server.use(
      http.put('*/api/v1/teachers/:id', async () => {
        await new Promise(resolve => setTimeout(resolve, 1000));
        return HttpResponse.json({
          success: true,
          data: { id: 1, name: 'Updated' },
        });
      })
    );

    render(<EditTeacherPage params={mockParams} />);

    await waitFor(() => screen.getByDisplayValue('Nguyễn Thị Giáo'));

    const submitButton = screen.getByRole('button', { name: /cập nhật|update/i });

    // Submit form
    await user.click(submitButton);

    // Button should be disabled immediately
    await waitFor(() => {
      expect(submitButton).toBeDisabled();
    });
  });

  it('should show error when teacher not found', async () => {
    mock404('*/api/v1/teachers/*', 'TEACHER_NOT_FOUND');

    render(<EditTeacherPage params={mockParams} />);

    await waitFor(() => {
      expect(screen.getByText('Lỗi')).toBeInTheDocument();
      expect(screen.getByText('Không tìm thấy thông tin giáo viên')).toBeInTheDocument();
    });
  });

  it('should validate phone number format', async () => {
    const user = userEvent.setup();

    render(<EditTeacherPage params={mockParams} />);

    await waitFor(() => screen.getByDisplayValue('0901234567'));

    // Enter invalid phone number
    const phoneInput = screen.getByDisplayValue('0901234567');
    await user.clear(phoneInput);
    await user.type(phoneInput, 'invalid-phone');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /cập nhật|update/i });
    await user.click(submitButton);

    // Should show validation error
    await waitFor(() => {
      expect(screen.getByText(/số điện thoại/i)).toBeInTheDocument();
    });
  });

  it('should allow updating status field', async () => {
    const user = userEvent.setup();

    server.use(
      http.put('*/api/v1/teachers/:id', async ({ request }) => {
        const body = await request.json() as Record<string, unknown>;
        expect(body.status).toBe('INACTIVE');
        return HttpResponse.json({
          success: true,
          data: { id: 1, ...body },
        });
      })
    );

    render(<EditTeacherPage params={mockParams} />);

    await waitFor(() => screen.getByDisplayValue('Nguyễn Thị Giáo'));

    // Change status
    const statusSelect = screen.getByLabelText(/trạng thái/i);
    await user.selectOptions(statusSelect, 'INACTIVE');

    // Submit form
    const submitButton = screen.getByRole('button', { name: /cập nhật|update/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/đã cập nhật giáo viên/i)).toBeInTheDocument();
    });
  });
});
