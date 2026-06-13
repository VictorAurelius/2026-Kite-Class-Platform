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
  MyEnrollment,
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
   * List the calling student's OWN enrollments, enriched with class + course
   * names (GAP-1285).
   *
   * GET /api/v1/enrollments/me — self-scoped. The Gateway resolves the calling
   * STUDENT's {@code students.id} from the JWT (X-User-Reference-Id), so this
   * returns ONLY that student's enrollments. Used by the student shell to build
   * "Khóa học của tôi" + "Lớp của tôi" enrollment-scoped (replaces the catalog /
   * SHARED-READ workaround).
   */
  getMine: async (
    params: { page?: number; size?: number; sort?: string } = {}
  ): Promise<PaginatedResponse<MyEnrollment>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<MyEnrollment>>>(
      '/api/v1/enrollments/me',
      { params: { page: 0, size: 100, ...params } }
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
