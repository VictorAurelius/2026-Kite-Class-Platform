/**
 * Integration tests for Teacher Detail Page.
 * Tests data loading, status display, actions, and error handling.
 *
 * @author KiteClass Team
 * @since 3.11.0
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import TeacherDetailPage from '../page';
import { useRouter } from 'next/navigation';
import type { AppRouterInstance } from 'next/dist/shared/lib/app-router-context.shared-runtime';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { mockConfirm, mock404 } from '@/test/page-test-utils';

vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
  usePathname: vi.fn(() => '/teachers/1'),
}));

describe.skip('TeacherDetailPage Integration - SKIPPED: Next.js 15 use(params) incompatible with RTL', () => {
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
    window.confirm = vi.fn();
  });

  it('should load and display teacher details', async () => {
    render(<TeacherDetailPage params={mockParams} />);

    // Shows loading initially
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();

    // Data loads
    await waitFor(() => {
      expect(screen.getByText('Nguyễn Thị Giáo')).toBeInTheDocument();
      expect(screen.getByText('giao.nguyen@kiteclass.local')).toBeInTheDocument();
    });

    // Page title and description
    expect(screen.getByText('Thông tin chi tiết giáo viên')).toBeInTheDocument();
  });

  it('should display all teacher information fields', async () => {
    render(<TeacherDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Nguyễn Thị Giáo'));

    // Field labels
    expect(screen.getByText('Trạng thái')).toBeInTheDocument();
    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByText('Số điện thoại')).toBeInTheDocument();
    expect(screen.getByText('Chuyên môn')).toBeInTheDocument();
    expect(screen.getByText('Bằng cấp / Chứng chỉ')).toBeInTheDocument();
    expect(screen.getByText('Kinh nghiệm')).toBeInTheDocument();

    // Field values
    expect(screen.getByText('giao.nguyen@kiteclass.local')).toBeInTheDocument();
    expect(screen.getByText('0901234567')).toBeInTheDocument();
    expect(screen.getByText('Toán học')).toBeInTheDocument();
  });

  it('should display status badge correctly', async () => {
    render(<TeacherDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Nguyễn Thị Giáo'));

    // Status badge with Vietnamese label
    expect(screen.getByText('Đang hoạt động')).toBeInTheDocument();
  });

  it('should have edit button linking to edit page', async () => {
    render(<TeacherDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Nguyễn Thị Giáo'));

    const editButton = screen.getByRole('link', { name: /chỉnh sửa/i });
    expect(editButton).toHaveAttribute('href', '/teachers/1/edit');
  });

  it('should have delete button', async () => {
    render(<TeacherDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Nguyễn Thị Giáo'));

    const deleteButton = screen.getByRole('button', { name: /xóa/i });
    expect(deleteButton).toBeInTheDocument();
  });

  it('should delete teacher with confirmation', async () => {
    const user = userEvent.setup();
    mockConfirm(true);

    server.use(
      http.delete('*/api/v1/teachers/:id', () => {
        return new HttpResponse(null, { status: 204 });
      })
    );

    render(<TeacherDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Nguyễn Thị Giáo'));

    const deleteButton = screen.getByRole('button', { name: /xóa/i });
    await user.click(deleteButton);

    // Verify confirmation
    expect(window.confirm).toHaveBeenCalledWith(
      'Bạn có chắc chắn muốn xóa giáo viên này? Thao tác này không thể hoàn tác.'
    );

    // Should redirect to teachers list
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/teachers');
    });
  });

  it('should not delete teacher when confirmation cancelled', async () => {
    const user = userEvent.setup();
    mockConfirm(false);

    render(<TeacherDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Nguyễn Thị Giáo'));

    const deleteButton = screen.getByRole('button', { name: /xóa/i });
    await user.click(deleteButton);

    // Should not redirect
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('should show error when teacher not found', async () => {
    mock404('*/api/v1/teachers/*', 'TEACHER_NOT_FOUND');

    render(<TeacherDetailPage params={mockParams} />);

    await waitFor(() => {
      expect(screen.getByText('Lỗi')).toBeInTheDocument();
      expect(screen.getByText('Không tìm thấy thông tin giáo viên')).toBeInTheDocument();
    });
  });

  it('should display timestamps in Vietnamese format', async () => {
    render(<TeacherDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Nguyễn Thị Giáo'));

    expect(screen.getByText('Ngày tạo')).toBeInTheDocument();
    expect(screen.getByText('Cập nhật lần cuối')).toBeInTheDocument();
  });

  it('should handle optional fields (bio, qualification) gracefully', async () => {
    server.use(
      http.get('*/api/v1/teachers/:id', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            name: 'Test Teacher',
            email: 'test@kiteclass.local',
            phoneNumber: null,
            specialization: null,
            qualification: null,
            experienceYears: null,
            bio: null,
            status: 'ACTIVE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        });
      })
    );

    render(<TeacherDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Test Teacher'));

    // Should show em dash for empty fields
    const emDashes = screen.getAllByText('—');
    expect(emDashes.length).toBeGreaterThan(0);
  });

  it('should display bio when provided', async () => {
    server.use(
      http.get('*/api/v1/teachers/:id', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            name: 'Nguyễn Thị Giáo',
            email: 'giao.nguyen@kiteclass.local',
            phoneNumber: '0901234567',
            specialization: 'Toán học',
            qualification: 'Thạc sĩ Toán học',
            experienceYears: 10,
            bio: 'Giáo viên giàu kinh nghiệm với 10 năm giảng dạy.',
            status: 'ACTIVE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        });
      })
    );

    render(<TeacherDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Nguyễn Thị Giáo'));

    expect(screen.getByText('Giới thiệu')).toBeInTheDocument();
    expect(screen.getByText('Giáo viên giàu kinh nghiệm với 10 năm giảng dạy.')).toBeInTheDocument();
  });
});
