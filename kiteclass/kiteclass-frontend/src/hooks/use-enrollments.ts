/**
 * React Query hooks for enrollment operations (simplified for attendance).
 *
 * @author KiteClass Team
 * @since 2.6.0
 */

'use client';

import { useQuery } from '@tanstack/react-query';
import { enrollmentsApi } from '@/lib/api/enrollments';
import type { EnrollmentSearchParams, EnrollmentStatus } from '@/types/enrollment';

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
