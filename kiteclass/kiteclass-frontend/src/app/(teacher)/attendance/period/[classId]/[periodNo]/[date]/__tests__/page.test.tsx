/**
 * Smoke test for the GVCN per-tiết attendance route shell (Phase 1B v1).
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AllTheProviders } from '@/test/utils';
import PeriodAttendancePage from '../page';
import { attendancePeriodApi } from '@/lib/api/attendance-period';
import type { AttendancePeriodResponse } from '@/lib/api/attendance-period';
import { useAuthStore } from '@/stores/auth-store';
import { UserType } from '@/types/auth';

vi.mock('next/navigation', () => ({
  useParams: () => ({
    classId: '202',
    periodNo: '2',
    date: '2026-09-05',
  }),
}));

vi.mock('@/lib/api/attendance-period', () => ({
  attendancePeriodApi: {
    getDailyRoster: vi.fn(),
    upsertBatch: vi.fn(),
    updateOne: vi.fn(),
  },
}));

const seedRoster: AttendancePeriodResponse[] = [
  {
    id: 1,
    studentId: 101,
    classId: 202,
    subjectSectionId: 303,
    periodNo: 2,
    date: '2026-09-05',
    status: 'PRESENT',
    recordedBy: 404,
    recordedAt: '2026-09-05T07:05:00',
    notes: null,
    version: 1,
    createdAt: '2026-09-05T07:05:00.123Z',
    updatedAt: null,
  },
  {
    id: 2,
    studentId: 102,
    classId: 202,
    subjectSectionId: 303,
    periodNo: 2,
    date: '2026-09-05',
    status: 'ABSENT',
    recordedBy: 404,
    recordedAt: '2026-09-05T07:05:00',
    notes: null,
    version: 1,
    createdAt: '2026-09-05T07:05:00.123Z',
    updatedAt: null,
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  // Seed auth store with a logged-in teacher.
  useAuthStore.setState({
    user: {
      id: 404,
      email: 'gvcn@example.com',
      name: 'GVCN',
      userType: UserType.TEACHER,
    },
    accessToken: 'token',
    refreshToken: 'refresh',
    tenantId: 'tenant-1',
    isAuthenticated: true,
  });
});

describe('PeriodAttendancePage (route shell)', () => {
  it('renders the header + tap-grid + bulk actions when roster loads', async () => {
    vi.mocked(attendancePeriodApi.getDailyRoster).mockResolvedValueOnce(
      seedRoster,
    );

    render(<PeriodAttendancePage />, { wrapper: AllTheProviders });

    expect(
      screen.getByRole('heading', { name: /Điểm danh tiết 2/ }),
    ).toBeInTheDocument();

    await waitFor(() =>
      expect(screen.getByTestId('period-tap-grid')).toBeInTheDocument(),
    );
    expect(screen.getByTestId('period-bulk-actions')).toBeInTheDocument();
    expect(screen.getByTestId('period-tap-row-101')).toBeInTheDocument();
    expect(screen.getByTestId('period-tap-row-102')).toBeInTheDocument();
  });

  it('clicking "Lưu" calls upsertBatch with the seeded statuses', async () => {
    vi.mocked(attendancePeriodApi.getDailyRoster).mockResolvedValueOnce(
      seedRoster,
    );
    vi.mocked(attendancePeriodApi.upsertBatch).mockResolvedValueOnce(seedRoster);

    render(<PeriodAttendancePage />, { wrapper: AllTheProviders });

    await waitFor(() =>
      expect(screen.getByTestId('period-tap-grid')).toBeInTheDocument(),
    );

    fireEvent.click(screen.getByRole('button', { name: /Lưu điểm danh/ }));

    await waitFor(() =>
      expect(attendancePeriodApi.upsertBatch).toHaveBeenCalledTimes(1),
    );

    const callArgs = vi.mocked(attendancePeriodApi.upsertBatch).mock.calls[0];
    expect(callArgs).toBeDefined();
    const [body, opts] = callArgs!;
    expect(opts).toEqual({ teacherId: 404 });
    expect(body.entries).toHaveLength(2);
    const studentIds = body.entries.map((e) => e.studentId).sort();
    expect(studentIds).toEqual([101, 102]);
  });
});
