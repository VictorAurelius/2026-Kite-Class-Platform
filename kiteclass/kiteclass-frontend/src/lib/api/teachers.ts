/**
 * Teacher API functions.
 *
 * @author KiteClass Team
 * @since 3.5.0
 */

import { apiClient } from '@/lib/api-client';
import type {
  Teacher,
  CreateTeacherRequest,
  UpdateTeacherRequest,
  TeacherSearchParams,
} from '@/types/teacher';
import type { ApiResponse, PaginatedResponse } from '@/types/api';

export const teachersApi = {
  /**
   * Get paginated list of teachers with search/filter.
   */
  getTeachers: async (
    params: TeacherSearchParams = {}
  ): Promise<PaginatedResponse<Teacher>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Teacher>>>(
      '/api/v1/teachers',
      { params }
    );
    return response.data.data!;
  },

  /**
   * Get teacher by ID.
   */
  getTeacher: async (id: number): Promise<Teacher> => {
    const response = await apiClient.get<ApiResponse<Teacher>>(`/api/v1/teachers/${id}`);
    return response.data.data!;
  },

  /**
   * Create new teacher.
   */
  createTeacher: async (data: CreateTeacherRequest): Promise<Teacher> => {
    const response = await apiClient.post<ApiResponse<Teacher>>('/api/v1/teachers', data);
    return response.data.data!;
  },

  /**
   * Update existing teacher.
   */
  updateTeacher: async (
    id: number,
    data: UpdateTeacherRequest
  ): Promise<Teacher> => {
    const response = await apiClient.patch<ApiResponse<Teacher>>(
      `/api/v1/teachers/${id}`,
      data
    );
    return response.data.data!;
  },

  /**
   * Soft delete teacher.
   */
  deleteTeacher: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/v1/teachers/${id}`);
  },
};
