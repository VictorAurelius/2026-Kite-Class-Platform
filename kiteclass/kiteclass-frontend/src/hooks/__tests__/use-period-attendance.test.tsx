/**
 * Tests for the period-attendance TanStack hooks (Phase 1B v1, GAP-323b).
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { AllTheProviders } from '@/test/utils';
import {
  useDailyRoster,
  useUpsertAttendancePeriod,
} from '../use-period-attendance';
import { attendancePeriodApi } from '@/lib/api/attendance-period';
import type { AttendancePeriodResponse } from '@/lib/api/attendance-period';

vi.mock('@/lib/api/attendance-period', () => ({
  attendancePeriodApi: {
    getDailyRoster: vi.fn(),
    upsertBatch: vi.fn(),
    updateOne: vi.fn(),
  },
}));

const sampleRow = {
  id: 1,
  studentId: 101,
  classId: 202,
  subjectSectionId: 303,
  periodNo: 2,
  date: '2026-09-05',
  status: 'PRESENT' as const,
  recordedBy: 404,
  recordedAt: '2026-09-05T07:05:00',
  notes: null,
  version: 1,
  createdAt: '2026-09-05T07:05:00.123Z',
  updatedAt: null,
};

describe('useDailyRoster', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches the roster for a class+date when both are present', async () => {
    vi.mocked(attendancePeriodApi.getDailyRoster).mockResolvedValueOnce([
      sampleRow,
    ]);

    const { result } = renderHook(() => useDailyRoster(202, '2026-09-05'), {
      wrapper: AllTheProviders,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(attendancePeriodApi.getDailyRoster).toHaveBeenCalledWith(
      202,
      '2026-09-05',
    );
    expect(result.current.data).toEqual([sampleRow]);
  });

  it('does NOT fire when classId is 0 (guarded `enabled`)', () => {
    renderHook(() => useDailyRoster(0, '2026-09-05'), {
      wrapper: AllTheProviders,
    });
    expect(attendancePeriodApi.getDailyRoster).not.toHaveBeenCalled();
  });

  it('does NOT fire when date is empty', () => {
    renderHook(() => useDailyRoster(202, ''), {
      wrapper: AllTheProviders,
    });
    expect(attendancePeriodApi.getDailyRoster).not.toHaveBeenCalled();
  });
});

describe('useUpsertAttendancePeriod', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('POSTs the batch via the API client and resolves with returned rows', async () => {
    vi.mocked(attendancePeriodApi.upsertBatch).mockResolvedValueOnce([sampleRow]);

    const { result } = renderHook(
      () =>
        useUpsertAttendancePeriod({
          teacherId: 404,
          classId: 202,
          date: '2026-09-05',
        }),
      { wrapper: AllTheProviders },
    );

    let mutationResult: AttendancePeriodResponse[] | undefined;
    await act(async () => {
      mutationResult = await result.current.mutateAsync({
        entries: [
          {
            studentId: 101,
            classId: 202,
            subjectSectionId: 303,
            periodNo: 2,
            date: '2026-09-05',
            status: 'PRESENT',
          },
        ],
      });
    });

    expect(attendancePeriodApi.upsertBatch).toHaveBeenCalledTimes(1);
    expect(attendancePeriodApi.upsertBatch).toHaveBeenCalledWith(
      expect.objectContaining({ entries: expect.any(Array) }),
      { teacherId: 404 },
    );
    expect(mutationResult).toEqual([sampleRow]);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([sampleRow]);
  });
});
