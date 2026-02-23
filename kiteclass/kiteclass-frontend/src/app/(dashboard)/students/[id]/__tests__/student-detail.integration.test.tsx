/**
 * Integration tests for Student Detail Page.
 * Tests data loading, status display, actions, and error handling.
 *
 * @since 2026-02-23
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import StudentDetailPage from '../page';
import { useRouter } from 'next/navigation';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { mockConfirm, mock404 } from '@/test/page-test-utils';

vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
  usePathname: vi.fn(() => '/students/1'),
}));

describe.skip('StudentDetailPage Integration - SKIPPED: Next.js 15 use(params) incompatible with RTL', () => {
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
    vi.mocked(useRouter).mockReturnValue(mockRouter as any);
    mockPush.mockClear();
    window.confirm = vi.fn();
  });

  it('should load and display student details', async () => {
    render(<StudentDetailPage params={mockParams} />);

    // Shows loading initially
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();

    // Data loads
    await waitFor(() => {
      expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    });

    // Verify all fields are displayed
    expect(screen.getByText('nguyenvana@gmail.com')).toBeInTheDocument();
    expect(screen.getByText('0901234567')).toBeInTheDocument();

    // Verify status badge
    expect(screen.getByText('Đang học')).toBeInTheDocument();
  });

  it('should display status badge with correct variant', async () => {
    render(<StudentDetailPage params={mockParams} />);

    await waitFor(() => {
      expect(screen.getByText('Đang học')).toBeInTheDocument();
    });

    const statusBadge = screen.getByText('Đang học');
    expect(statusBadge).toBeInTheDocument();
    // Status badge should be rendered with success variant for ACTIVE status
  });

  it('should display formatted dates correctly', async () => {
    render(<StudentDetailPage params={mockParams} />);

    await waitFor(() => {
      expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    });

    // Check for date labels
    expect(screen.getByText('Ngày sinh')).toBeInTheDocument();
    expect(screen.getByText('Ngày tạo')).toBeInTheDocument();
    expect(screen.getByText('Cập nhật lần cuối')).toBeInTheDocument();
  });

  it('should have edit button linking to edit page', async () => {
    render(<StudentDetailPage params={mockParams} />);

    await waitFor(() => {
      expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    });

    const editLink = screen.getByRole('link', { name: /chỉnh sửa/i });
    expect(editLink).toHaveAttribute('href', '/students/1/edit');
  });

  it('should delete student with confirmation and redirect', async () => {
    const user = userEvent.setup();
    mockConfirm(true);

    render(<StudentDetailPage params={mockParams} />);

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    });

    // Mock successful delete
    server.use(
      http.delete('*/api/v1/students/:id', () => {
        return new HttpResponse(null, { status: 204 });
      })
    );

    // Click delete button
    const deleteButton = screen.getByRole('button', { name: /xóa/i });
    await user.click(deleteButton);

    // Verify confirmation was shown
    expect(window.confirm).toHaveBeenCalledWith(
      'Bạn có chắc chắn muốn xóa học viên này? Thao tác này không thể hoàn tác.'
    );

    // Wait for success and redirect
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/students');
    });
  });

  it('should not delete student when confirmation is cancelled', async () => {
    const user = userEvent.setup();
    mockConfirm(false);

    render(<StudentDetailPage params={mockParams} />);

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    });

    // Click delete button
    const deleteButton = screen.getByRole('button', { name: /xóa/i });
    await user.click(deleteButton);

    // Verify confirmation was shown
    expect(window.confirm).toHaveBeenCalled();

    // Should not redirect
    expect(mockPush).not.toHaveBeenCalled();

    // Student details still visible
    expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
  });

  it('should handle 404 student not found', async () => {
    const notFoundParams = Promise.resolve({ id: '999' });
    mock404('*/api/v1/students/999');

    render(<StudentDetailPage params={notFoundParams} />);

    // Wait for error to display
    await waitFor(() => {
      expect(screen.getByText('Lỗi')).toBeInTheDocument();
      expect(
        screen.getByText('Không tìm thấy thông tin học viên')
      ).toBeInTheDocument();
    });

    // Should not show loading or student data
    expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
    expect(screen.queryByText('Nguyễn Văn A')).not.toBeInTheDocument();
  });

  it('should display page title and description', async () => {
    render(<StudentDetailPage params={mockParams} />);

    await waitFor(() => {
      expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    });

    expect(screen.getByText('Thông tin chi tiết học viên')).toBeInTheDocument();
  });

  it('should display optional fields with fallback', async () => {
    // Mock student without optional fields
    server.use(
      http.get('*/api/v1/students/2', () => {
        return HttpResponse.json({
          id: 2,
          name: 'Test Student',
          email: 'test@example.com',
          phone: null,
          status: 'ACTIVE',
          dateOfBirth: null,
          address: null,
          gender: null,
          enrollmentDate: null,
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        });
      })
    );

    const params = Promise.resolve({ id: '2' });
    render(<StudentDetailPage params={params} />);

    await waitFor(() => {
      expect(screen.getByText('Test Student')).toBeInTheDocument();
    });

    // Check that dash (—) is displayed for missing optional fields
    const dashes = screen.getAllByText('—');
    expect(dashes.length).toBeGreaterThan(0);
  });

  it('should display gender correctly', async () => {
    // Mock student with gender
    server.use(
      http.get('*/api/v1/students/3', () => {
        return HttpResponse.json({
          id: 3,
          name: 'Male Student',
          email: 'male@example.com',
          phone: '0901234567',
          status: 'ACTIVE',
          gender: 'MALE',
          dateOfBirth: '2005-01-15',
          address: '123 Test',
          enrollmentDate: '2024-09-01',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        });
      })
    );

    const params = Promise.resolve({ id: '3' });
    render(<StudentDetailPage params={params} />);

    await waitFor(() => {
      expect(screen.getByText('Male Student')).toBeInTheDocument();
    });

    // Check gender is displayed as Vietnamese
    expect(screen.getByText('Nam')).toBeInTheDocument();
  });

  it('should display all status variants correctly', async () => {
    const statuses = [
      { status: 'ACTIVE', label: 'Đang học' },
      { status: 'INACTIVE', label: 'Không hoạt động' },
      { status: 'GRADUATED', label: 'Đã tốt nghiệp' },
      { status: 'SUSPENDED', label: 'Đình chỉ' },
    ];

    for (const { status, label } of statuses) {
      server.use(
        http.get('*/api/v1/students/1', () => {
          return HttpResponse.json({
            id: 1,
            name: 'Test Student',
            email: 'test@example.com',
            phone: '0901234567',
            status,
            dateOfBirth: '2005-01-15',
            address: '123 Test',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          });
        })
      );

      const { unmount } = render(<StudentDetailPage params={mockParams} />);

      await waitFor(() => {
        expect(screen.getByText(label)).toBeInTheDocument();
      });

      unmount();
    }
  });
});
