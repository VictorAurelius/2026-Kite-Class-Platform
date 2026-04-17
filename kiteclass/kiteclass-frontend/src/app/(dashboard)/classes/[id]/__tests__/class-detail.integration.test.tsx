/**
 * Integration tests for Class Detail Page.
 * Tests lifecycle actions (Start, Complete, Cancel), sessions display, class code generation.
 *
 * @author KiteClass Team
 * @since 3.11.0
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import ClassDetailPage from '../page';
import { useRouter } from 'next/navigation';
import type { AppRouterInstance } from 'next/dist/shared/lib/app-router-context.shared-runtime';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { mockConfirm, mock404 } from '@/test/page-test-utils';

vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
  usePathname: vi.fn(() => '/classes/1'),
}));

describe.skip('ClassDetailPage Integration - SKIPPED: Next.js 15 use(params) incompatible with RTL', () => {
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

    // Mock clipboard API
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn(),
      },
    });
  });

  it('should load and display class details', async () => {
    render(<ClassDetailPage params={mockParams} />);

    // Shows loading initially
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();

    // Data loads
    await waitFor(() => {
      expect(screen.getByText('Lớp Tiếng Anh Buổi Sáng')).toBeInTheDocument();
    });
  });

  it('should display status badge with Vietnamese label', async () => {
    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    // Status badge for SCHEDULED class
    expect(screen.getByText('Đã lên lịch')).toBeInTheDocument();
  });

  it('should display class code with copy button', async () => {
    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    // Class code should be visible
    expect(screen.getByText(/mã: ENG-B1-SANG/i)).toBeInTheDocument();

    // Copy button should be visible
    const copyButton = screen.getByRole('button', { name: '' }); // Icon button
    expect(copyButton).toBeInTheDocument();
  });

  it('should copy class code to clipboard', async () => {
    const user = userEvent.setup();
    const writeTextSpy = vi.spyOn(navigator.clipboard, 'writeText');

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    // Click copy button
    const copyButtons = screen.getAllByRole('button');
    const copyButton = copyButtons.find(btn => btn.querySelector('svg')); // Find icon button
    await user.click(copyButton!);

    // Should copy to clipboard
    expect(writeTextSpy).toHaveBeenCalledWith('ENG-B1-SANG');

    // Should show success toast
    await waitFor(() => {
      expect(screen.getByText(/đã sao chép mã lớp học/i)).toBeInTheDocument();
    });
  });

  it('should show "Bắt đầu" button for SCHEDULED classes', async () => {
    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    // Should have Start button
    const startButton = screen.getByRole('button', { name: /bắt đầu/i });
    expect(startButton).toBeInTheDocument();
  });

  it('should start SCHEDULED class successfully', async () => {
    const user = userEvent.setup();
    mockConfirm(true);

    server.use(
      http.post('*/api/v1/classes/:id/start', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            status: 'IN_PROGRESS',
            startedAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        });
      })
    );

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    const startButton = screen.getByRole('button', { name: /bắt đầu/i });
    await user.click(startButton);

    // Should show confirmation
    expect(window.confirm).toHaveBeenCalledWith(
      expect.stringContaining('Bắt đầu lớp học')
    );

    // Should show success toast
    await waitFor(() => {
      expect(screen.getByText(/đã bắt đầu lớp học/i)).toBeInTheDocument();
    });
  });

  it('should show "Hoàn thành" and "Hủy lớp" buttons for IN_PROGRESS classes', async () => {
    server.use(
      http.get('*/api/v1/classes/:id', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            courseId: 1,
            name: 'Lớp Đang Học',
            classCode: 'IN-PROGRESS-001',
            status: 'IN_PROGRESS',
            schedule: 'Thứ 2, 4, 6: 08:00-10:00',
            locationType: 'IN_PERSON',
            locationDetail: 'Phòng A101',
            startDate: '2026-03-01',
            endDate: '2026-06-30',
            maxStudents: 30,
            currentEnrolled: 15,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        });
      })
    );

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Đang Học'));

    // Should have Complete and Cancel buttons
    expect(screen.getByRole('button', { name: /hoàn thành/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /hủy lớp/i })).toBeInTheDocument();
  });

  it('should complete IN_PROGRESS class successfully', async () => {
    const user = userEvent.setup();
    mockConfirm(true);

    server.use(
      http.get('*/api/v1/classes/:id', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            courseId: 1,
            name: 'Lớp Đang Học',
            classCode: 'IN-PROGRESS-001',
            status: 'IN_PROGRESS',
            currentEnrolled: 15,
            maxStudents: 30,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        });
      }),
      http.post('*/api/v1/classes/:id/complete', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            status: 'COMPLETED',
            completedAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        });
      })
    );

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Đang Học'));

    const completeButton = screen.getByRole('button', { name: /hoàn thành/i });
    await user.click(completeButton);

    // Should show confirmation
    expect(window.confirm).toHaveBeenCalledWith(
      expect.stringContaining('Hoàn thành lớp học')
    );

    // Should show success toast
    await waitFor(() => {
      expect(screen.getByText(/đã hoàn thành lớp học/i)).toBeInTheDocument();
    });
  });

  it('should show cancel dialog when clicking "Hủy lớp"', async () => {
    const user = userEvent.setup();

    server.use(
      http.get('*/api/v1/classes/:id', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            courseId: 1,
            name: 'Lớp Đang Học',
            classCode: 'IN-PROGRESS-001',
            status: 'IN_PROGRESS',
            currentEnrolled: 15,
            maxStudents: 30,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        });
      })
    );

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Đang Học'));

    const cancelButton = screen.getByRole('button', { name: /hủy lớp/i });
    await user.click(cancelButton);

    // Cancel dialog should appear
    await waitFor(() => {
      expect(screen.getByText('Hủy lớp học')).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/nhập lý do hủy lớp học/i)).toBeInTheDocument();
    });
  });

  it('should require reason to cancel class', async () => {
    const user = userEvent.setup();

    server.use(
      http.get('*/api/v1/classes/:id', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            courseId: 1,
            name: 'Lớp Đang Học',
            classCode: 'IN-PROGRESS-001',
            status: 'IN_PROGRESS',
            currentEnrolled: 15,
            maxStudents: 30,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        });
      })
    );

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Đang Học'));

    // Open cancel dialog
    const cancelButton = screen.getByRole('button', { name: /hủy lớp/i });
    await user.click(cancelButton);

    // Click confirm without entering reason
    const confirmButton = screen.getByRole('button', { name: /xác nhận hủy/i });
    await user.click(confirmButton);

    // Should show error toast
    await waitFor(() => {
      expect(screen.getByText(/vui lòng nhập lý do hủy/i)).toBeInTheDocument();
    });
  });

  it('should cancel class with reason', async () => {
    const user = userEvent.setup();

    server.use(
      http.get('*/api/v1/classes/:id', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            courseId: 1,
            name: 'Lớp Đang Học',
            classCode: 'IN-PROGRESS-001',
            status: 'IN_PROGRESS',
            currentEnrolled: 15,
            maxStudents: 30,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        });
      }),
      http.post('*/api/v1/classes/:id/cancel', async ({ request }) => {
        const body = await request.json() as { reason: string };
        expect(body.reason).toBe('Không đủ học viên');
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            status: 'CANCELLED',
            cancelReason: body.reason,
            cancelledAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        });
      })
    );

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Đang Học'));

    // Open cancel dialog
    const cancelButton = screen.getByRole('button', { name: /hủy lớp/i });
    await user.click(cancelButton);

    // Enter reason
    const reasonInput = screen.getByPlaceholderText(/nhập lý do hủy lớp học/i);
    await user.type(reasonInput, 'Không đủ học viên');

    // Confirm
    const confirmButton = screen.getByRole('button', { name: /xác nhận hủy/i });
    await user.click(confirmButton);

    // Should show success toast
    await waitFor(() => {
      expect(screen.getByText(/đã hủy lớp học/i)).toBeInTheDocument();
    });
  });

  it('should generate class code', async () => {
    const user = userEvent.setup();
    mockConfirm(true);

    server.use(
      http.post('*/api/v1/classes/:id/generate-code', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            classCode: 'NEW-CODE-123',
            updatedAt: new Date().toISOString(),
          },
        });
      })
    );

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    const generateButton = screen.getByRole('button', { name: /tạo mã lớp/i });
    await user.click(generateButton);

    // Should show confirmation
    expect(window.confirm).toHaveBeenCalledWith(
      expect.stringContaining('Tạo hoặc tạo lại mã lớp học')
    );

    // Should show success toast
    await waitFor(() => {
      expect(screen.getByText(/đã tạo mã lớp học/i)).toBeInTheDocument();
    });
  });

  it('should display sessions when available', async () => {
    server.use(
      http.get('*/api/v1/classes/:id/sessions', () => {
        return HttpResponse.json({
          success: true,
          data: [
            {
              id: 1,
              classId: 1,
              sessionNumber: 1,
              topic: 'Introduction',
              sessionDate: '2026-03-01',
              startTime: '08:00',
              endTime: '10:00',
              status: 'SCHEDULED',
            },
            {
              id: 2,
              classId: 1,
              sessionNumber: 2,
              topic: 'Basic Grammar',
              sessionDate: '2026-03-03',
              startTime: '08:00',
              endTime: '10:00',
              status: 'SCHEDULED',
            },
          ],
        });
      })
    );

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    // Sessions card should be visible
    expect(screen.getByText('Buổi học')).toBeInTheDocument();

    // Session details should be visible
    await waitFor(() => {
      expect(screen.getByText(/buổi 1: introduction/i)).toBeInTheDocument();
      expect(screen.getByText(/buổi 2: basic grammar/i)).toBeInTheDocument();
    });
  });

  it('should show empty state when no sessions', async () => {
    server.use(
      http.get('*/api/v1/classes/:id/sessions', () => {
        return HttpResponse.json({
          success: true,
          data: [],
        });
      })
    );

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    // Should show empty sessions message
    expect(screen.getByText(/chưa có buổi học nào được tạo/i)).toBeInTheDocument();
  });

  it('should delete SCHEDULED class with 0 students', async () => {
    const user = userEvent.setup();
    mockConfirm(true);

    server.use(
      http.get('*/api/v1/classes/:id', () => {
        return HttpResponse.json({
          success: true,
          data: {
            id: 1,
            courseId: 1,
            name: 'Lớp Chưa Có Học Viên',
            classCode: 'EMPTY-001',
            status: 'SCHEDULED',
            currentEnrolled: 0,
            maxStudents: 30,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        });
      }),
      http.delete('*/api/v1/classes/:id', () => {
        return new HttpResponse(null, { status: 204 });
      })
    );

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Chưa Có Học Viên'));

    const deleteButton = screen.getByRole('button', { name: /xóa/i });
    await user.click(deleteButton);

    // Should show confirmation
    expect(window.confirm).toHaveBeenCalledWith(
      expect.stringContaining('Xóa lớp học')
    );

    // Should redirect to classes list
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/classes');
    });
  });

  it('should NOT show delete button for classes with students', async () => {
    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    // currentEnrolled = 15, so no delete button
    expect(screen.queryByRole('button', { name: /xóa/i })).not.toBeInTheDocument();
  });

  it('should show error when class not found', async () => {
    mock404('*/api/v1/classes/*', 'CLASS_NOT_FOUND');

    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => {
      expect(screen.getByText('Lỗi')).toBeInTheDocument();
      expect(screen.getByText('Không tìm thấy lớp học')).toBeInTheDocument();
    });
  });

  it('should display enrollment info', async () => {
    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    // Should show current/max students
    expect(screen.getByText(/15\/30/)).toBeInTheDocument();
    expect(screen.getByText(/sĩ số/i)).toBeInTheDocument();
  });

  it('should display location info', async () => {
    render(<ClassDetailPage params={mockParams} />);

    await waitFor(() => screen.getByText('Lớp Tiếng Anh Buổi Sáng'));

    // Should show location type and detail
    expect(screen.getByText(/trực tiếp/i)).toBeInTheDocument();
    expect(screen.getByText('Phòng A101')).toBeInTheDocument();
  });
});
