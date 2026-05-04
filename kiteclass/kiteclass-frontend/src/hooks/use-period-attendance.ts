/**
 * TanStack Query hooks for K-12 per-tiết (period) attendance.
 *
 * Phase 1B v1 (GAP-323b, Wave 18b2 Bucket A) — exposes:
 * - {@link useDailyRoster} → daily class roster (list of period rows)
 * - {@link useUpsertAttendancePeriod} → idempotent batch upsert
 *
 * Single-row PATCH (merge dialog on optimistic-lock 409) is a Phase 1B
 * follow-up; the API client method exists but a dedicated hook is
 * deliberately deferred until the merge UX lands.
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import {
  attendancePeriodApi,
  type AttendancePeriodBatchCreateRequest,
  type AttendancePeriodResponse,
} from '@/lib/api/attendance-period';
import { toast } from '@/hooks/use-toast';

export const PERIOD_ATTENDANCE_QUERY_KEY = 'attendance-period';

/**
 * Fetch the daily roster for one class on one date. The query key includes
 * `(classId, date)` so the upsert mutation below can invalidate the exact
 * roster it just mutated.
 */
export function useDailyRoster(classId: number, date: string) {
  return useQuery({
    queryKey: [PERIOD_ATTENDANCE_QUERY_KEY, 'daily-roster', classId, date],
    queryFn: () => attendancePeriodApi.getDailyRoster(classId, date),
    enabled: classId > 0 && date.length > 0,
  });
}

interface UseUpsertOptions {
  /** Resolved teacher (user) ID — sent as `X-Teacher-Id` per Phase 1B v1 contract. */
  teacherId: number;
  /** Class ID for cache invalidation on success. */
  classId: number;
  /** Date for cache invalidation on success. */
  date: string;
}

/**
 * Idempotent batch upsert — wraps `attendancePeriodApi.upsertBatch`.
 *
 * On success: invalidate the matching daily-roster query so the UI re-reads
 * the canonical server state (no need to merge optimistic updates manually
 * for the v1 — fast network round-trip is fine for ≤60-entry batches).
 */
export function useUpsertAttendancePeriod(opts: UseUpsertOptions) {
  const queryClient = useQueryClient();

  return useMutation<
    AttendancePeriodResponse[],
    AxiosError<{ message?: string; error?: string }>,
    AttendancePeriodBatchCreateRequest
  >({
    mutationFn: (body) =>
      attendancePeriodApi.upsertBatch(body, { teacherId: opts.teacherId }),
    onSuccess: (rows) => {
      queryClient.invalidateQueries({
        queryKey: [
          PERIOD_ATTENDANCE_QUERY_KEY,
          'daily-roster',
          opts.classId,
          opts.date,
        ],
      });
      toast({
        title: 'Đã lưu điểm danh',
        description: `Đã ghi nhận ${rows.length} học sinh.`,
      });
    },
    onError: (error) => {
      const description =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Không thể lưu điểm danh';
      toast({
        title: 'Lỗi',
        description,
        variant: 'destructive',
      });
    },
  });
}
