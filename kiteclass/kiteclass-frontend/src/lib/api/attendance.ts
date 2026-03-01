/**
 * Attendance API functions.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

import { apiClient } from '@/lib/api-client';
import type {
  Attendance,
  CreateAttendanceRequest,
  BulkAttendanceRequest,
  UpdateAttendanceStatusRequest,
  AttendanceStatsResponse,
  AttendanceSearchParams,
} from '@/types/attendance';
import type { ApiResponse, PaginatedResponse } from '@/types/api';

export const attendanceApi = {
  /**
   * Mark single attendance.
   *
   * POST /api/v1/attendance
   */
  markAttendance: async (data: CreateAttendanceRequest): Promise<Attendance> => {
    const response = await apiClient.post<ApiResponse<Attendance>>(
      '/api/v1/attendance',
      data
    );
    return response.data.data!;
  },

  /**
   * Mark bulk attendance for a session.
   *
   * POST /api/v1/attendance/bulk
   */
  markBulkAttendance: async (data: BulkAttendanceRequest): Promise<Attendance[]> => {
    const response = await apiClient.post<ApiResponse<Attendance[]>>(
      '/api/v1/attendance/bulk',
      data
    );
    return response.data.data!;
  },

  /**
   * Get attendance by ID.
   *
   * GET /api/v1/attendance/:id
   */
  getAttendance: async (id: number): Promise<Attendance> => {
    const response = await apiClient.get<ApiResponse<Attendance>>(
      `/api/v1/attendance/${id}`
    );
    return response.data.data!;
  },

  /**
   * Get attendance by enrollment (student history).
   *
   * GET /api/v1/attendance/enrollment/:enrollmentId
   */
  getAttendanceByEnrollment: async (
    enrollmentId: number,
    params: AttendanceSearchParams = {}
  ): Promise<PaginatedResponse<Attendance>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Attendance>>>(
      `/api/v1/attendance/enrollment/${enrollmentId}`,
      { params }
    );
    return response.data.data!;
  },

  /**
   * Get attendance by session (class roster).
   *
   * GET /api/v1/attendance/session/:sessionId
   */
  getAttendanceBySession: async (
    sessionId: number,
    params: AttendanceSearchParams = {}
  ): Promise<PaginatedResponse<Attendance>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Attendance>>>(
      `/api/v1/attendance/session/${sessionId}`,
      { params }
    );
    return response.data.data!;
  },

  /**
   * Update attendance status.
   *
   * PUT /api/v1/attendance/:id
   */
  updateAttendanceStatus: async (
    id: number,
    data: UpdateAttendanceStatusRequest
  ): Promise<Attendance> => {
    const response = await apiClient.put<ApiResponse<Attendance>>(
      `/api/v1/attendance/${id}`,
      data
    );
    return response.data.data!;
  },

  /**
   * Delete attendance (soft delete).
   *
   * DELETE /api/v1/attendance/:id
   */
  deleteAttendance: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/v1/attendance/${id}`);
  },

  /**
   * Get student attendance statistics.
   *
   * GET /api/v1/attendance/stats/student/:studentId
   */
  getStudentStats: async (studentId: number): Promise<AttendanceStatsResponse> => {
    const response = await apiClient.get<ApiResponse<AttendanceStatsResponse>>(
      `/api/v1/attendance/stats/student/${studentId}`
    );
    return response.data.data!;
  },

  /**
   * Get class attendance statistics.
   *
   * GET /api/v1/attendance/stats/class/:classId
   */
  getClassStats: async (classId: number): Promise<AttendanceStatsResponse> => {
    const response = await apiClient.get<ApiResponse<AttendanceStatsResponse>>(
      `/api/v1/attendance/stats/class/${classId}`
    );
    return response.data.data!;
  },
};
