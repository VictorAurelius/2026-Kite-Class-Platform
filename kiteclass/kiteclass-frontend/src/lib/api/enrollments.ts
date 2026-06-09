/**
 * Enrollment API functions (simplified for attendance use).
 *
 * @author KiteClass Team
 * @since 2.6.0
 */

import { apiClient } from '@/lib/api-client';
import type {
  CreateEnrollmentRequest,
  Enrollment,
  EnrollmentSearchParams,
  EnrollmentStatus,
} from '@/types/enrollment';
import type { ApiResponse, PaginatedResponse } from '@/types/api';

export const enrollmentsApi = {
  /**
   * Enroll a single student into a class.
   *
   * POST /api/v1/enrollments
   */
  createEnrollment: async (
    req: CreateEnrollmentRequest
  ): Promise<Enrollment> => {
    const response = await apiClient.post<ApiResponse<Enrollment>>(
      '/api/v1/enrollments',
      req
    );
    return response.data.data!;
  },

  /**
   * Get enrollments by class ID.
   *
   * GET /api/v1/enrollments/class/:classId
   */
  getEnrollmentsByClass: async (
    classId: number,
    params: EnrollmentSearchParams = {}
  ): Promise<PaginatedResponse<Enrollment>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Enrollment>>>(
      `/api/v1/enrollments/class/${classId}`,
      { params }
    );
    return response.data.data!;
  },

  /**
   * Get enrollments by class ID and status.
   *
   * GET /api/v1/enrollments/class/:classId?status=ACTIVE
   */
  getEnrollmentsByClassAndStatus: async (
    classId: number,
    status: EnrollmentStatus,
    params: EnrollmentSearchParams = {}
  ): Promise<PaginatedResponse<Enrollment>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Enrollment>>>(
      `/api/v1/enrollments/class/${classId}`,
      { params: { ...params, status } }
    );
    return response.data.data!;
  },
};
