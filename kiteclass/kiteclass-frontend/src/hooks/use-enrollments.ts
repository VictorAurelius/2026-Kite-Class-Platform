/**
 * React Query hooks for enrollment operations (simplified for attendance).
 *
 * @author KiteClass Team
 * @since 2.6.0
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { enrollmentsApi } from '@/lib/api/enrollments';
import type {
  CreateEnrollmentRequest,
  EnrollmentSearchParams,
  EnrollmentStatus,
} from '@/types/enrollment';

const ENROLLMENTS_QUERY_KEY = 'enrollments';

/**
 * Get enrollments by class ID.
 */
export function useEnrollmentsByClass(
  classId: number,
  params: EnrollmentSearchParams = {}
) {
  return useQuery({
    queryKey: [ENROLLMENTS_QUERY_KEY, 'class', classId, params],
    queryFn: () => enrollmentsApi.getEnrollmentsByClass(classId, params),
    enabled: !!classId,
  });
}

/**
 * Get active enrollments by class ID.
 */
export function useActiveEnrollmentsByClass(
  classId: number,
  params: EnrollmentSearchParams = {}
) {
  return useQuery({
    queryKey: [ENROLLMENTS_QUERY_KEY, 'class', classId, 'ACTIVE', params],
    queryFn: () => enrollmentsApi.getEnrollmentsByClassAndStatus(classId, 'ACTIVE' as EnrollmentStatus, params),
    enabled: !!classId,
  });
}

/**
 * Enroll a single student into a class (GAP-1103).
 *
 * On success, invalidates the enrollment queries so the class roster + attendance
 * views refresh. Callers should surface success/error toasts (the dialog does).
 */
export function useCreateEnrollment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (req: CreateEnrollmentRequest) => enrollmentsApi.createEnrollment(req),
    onSuccess: (_data, variables) => {
      // Refresh roster + active-enrollment lists for the class.
      queryClient.invalidateQueries({ queryKey: [ENROLLMENTS_QUERY_KEY] });
      // GAP-1425: also refresh the class detail so the header sĩ số (current_enrolled)
      // updates immediately after enroll instead of staying stale until page reload.
      queryClient.invalidateQueries({ queryKey: ['classes', variables.classId] });
    },
  });
}
