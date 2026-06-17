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
   * GAP-1426: BE exposes the bulk mark under the class+session path
   * (POST /api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance) —
   * there is no /attendance/bulk mapping, so the old path 405'd.
   */
  markBulkAttendance: async (
    classId: number,
    data: BulkAttendanceRequest
  ): Promise<Attendance[]> => {
    const response = await apiClient.post<ApiResponse<Attendance[]>>(
      `/api/v1/attendance/classes/${classId}/sessions/${data.sessionId}/attendance`,
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
    // GAP-1476: BE getAttendanceBySessionId returns ResponseEntity<Page<...>>
    // UNWRAPPED (no { success, data } envelope, unlike the classes API). The old
    // `response.data.data!` was therefore `undefined`, so callers that aggregate
    // (getAttendanceByClass → result.content) threw → attendance reports showed
    // "Không thể tải dữ liệu". `response.data` IS the page.
    const response = await apiClient.get<PaginatedResponse<Attendance>>(
      `/api/v1/attendance/session/${sessionId}`,
      { params }
    );
    return response.data;
  },

  /**
   * Update attendance status.
   *
   * PATCH /api/v1/attendance/:id
   * GAP-1429: BE exposes @PatchMapping("/{id}"), FE must use PATCH (was PUT → 405).
   */
  updateAttendanceStatus: async (
    id: number,
    data: UpdateAttendanceStatusRequest
  ): Promise<Attendance> => {
    const response = await apiClient.patch<ApiResponse<Attendance>>(
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

  /**
   * Get all attendance records for a class (across all sessions).
   * This is a workaround since backend doesn't have a direct endpoint.
   * It fetches all sessions for the class, then fetches attendance for each session.
   */
  getAttendanceByClass: async (
    classId: number,
    params: AttendanceSearchParams = {}
  ): Promise<PaginatedResponse<Attendance>> => {
    // First, fetch all sessions for this class
    const { classesApi } = await import('./classes');
    const sessions = await classesApi.getSessions(classId);

    if (!sessions || sessions.length === 0) {
      return {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: params.size || 20,
        page: params.page || 0,
        first: true,
        last: true,
      };
    }

    // Fetch attendance for all sessions in parallel
    const attendancePromises = sessions.map((session) =>
      attendanceApi.getAttendanceBySession(session.id, { page: 0, size: 1000 })
    );

    const results = await Promise.all(attendancePromises);

    // Merge all attendance records
    const allAttendance = results.flatMap((result) => result.content);

    // Apply pagination
    const page = params.page || 0;
    const size = params.size || 20;
    const start = page * size;
    const end = start + size;
    const paginatedContent = allAttendance.slice(start, end);

    return {
      content: paginatedContent,
      totalElements: allAttendance.length,
      totalPages: Math.ceil(allAttendance.length / size),
      size,
      page,
      first: page === 0,
      last: page === Math.ceil(allAttendance.length / size) - 1,
    };
  },
};
