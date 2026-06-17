/**
 * Tests for ClassRosterSection (GAP-1474 Part A) — owner roster view.
 *
 * Verifies: loading, empty, and error states, plus that enrolled students are
 * listed with their enrollment-status badge (ACTIVE = "Đang học",
 * PENDING_PAYMENT = "Chờ thanh toán") and that names are resolved from the
 * students list (the roster endpoint returns only studentId).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@/test/utils';
import { ClassRosterSection } from '../class-roster-section';
import { useEnrollmentsByClass } from '@/hooks/use-enrollments';
import { useStudents } from '@/hooks/use-students';
import { EnrollmentStatus } from '@/types/enrollment';

vi.mock('@/hooks/use-enrollments', () => ({ useEnrollmentsByClass: vi.fn() }));
vi.mock('@/hooks/use-students', () => ({ useStudents: vi.fn() }));

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(useStudents).mockReturnValue({
    data: {
      content: [{ id: 224, name: 'Trần Văn An', email: 'an@g2walk.vn' }],
    },
    isLoading: false,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } as any);
});

describe('ClassRosterSection (GAP-1474)', () => {
  it('shows a loading spinner while enrollments load', () => {
    vi.mocked(useEnrollmentsByClass).mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any);

    render(<ClassRosterSection classId={27} />);

    expect(screen.getByText('Danh sách học sinh')).toBeInTheDocument();
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('shows an empty state when the class has no students', () => {
    vi.mocked(useEnrollmentsByClass).mockReturnValue({
      data: { content: [] },
      isLoading: false,
      error: null,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any);

    render(<ClassRosterSection classId={27} />);

    expect(screen.getByText('Lớp chưa có học sinh nào')).toBeInTheDocument();
  });

  it('lists enrolled students with their enrollment-status badge', () => {
    vi.mocked(useEnrollmentsByClass).mockReturnValue({
      data: {
        content: [
          { id: 109, studentId: 224, status: EnrollmentStatus.PENDING_PAYMENT },
          { id: 110, studentId: 999, status: EnrollmentStatus.ACTIVE },
        ],
      },
      isLoading: false,
      error: null,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any);

    render(<ClassRosterSection classId={27} />);

    // Name resolved from the students list for known studentId 224.
    expect(screen.getByText('Trần Văn An')).toBeInTheDocument();
    // Unknown studentId falls back to a deterministic placeholder.
    expect(screen.getByText('Học sinh #999')).toBeInTheDocument();
    // Status badges distinguish ACTIVE vs PENDING_PAYMENT.
    expect(screen.getByText('Chờ thanh toán')).toBeInTheDocument();
    expect(screen.getByText('Đang học')).toBeInTheDocument();
  });

  it('shows an error state when the roster fails to load', () => {
    vi.mocked(useEnrollmentsByClass).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error('boom'),
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any);

    render(<ClassRosterSection classId={27} />);

    expect(
      screen.getByText(/không tải được danh sách học sinh/i)
    ).toBeInTheDocument();
  });
});
