/**
 * Course API functions.
 *
 * @author KiteClass Team
 * @since 3.6.0
 */

import { apiClient } from '@/lib/api-client';
import type {
  Course,
  CreateCourseRequest,
  UpdateCourseRequest,
  CourseSearchParams,
} from '@/types/course';
import type { ApiResponse, PaginatedResponse } from '@/types/api';

export const coursesApi = {
  getAll: async (
    params: CourseSearchParams = {}
  ): Promise<PaginatedResponse<Course>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Course>>>(
      '/api/v1/courses',
      { params }
    );
    return response.data.data!;
  },

  getById: async (id: number): Promise<Course> => {
    const response = await apiClient.get<ApiResponse<Course>>(`/api/v1/courses/${id}`);
    return response.data.data!;
  },

  create: async (data: CreateCourseRequest): Promise<Course> => {
    const response = await apiClient.post<ApiResponse<Course>>('/api/v1/courses', data);
    return response.data.data!;
  },

  update: async (id: number, data: UpdateCourseRequest): Promise<Course> => {
    const response = await apiClient.patch<ApiResponse<Course>>(
      `/api/v1/courses/${id}`,
      data
    );
    return response.data.data!;
  },

  publish: async (id: number): Promise<Course> => {
    const response = await apiClient.post<ApiResponse<Course>>(
      `/api/v1/courses/${id}/publish`
    );
    return response.data.data!;
  },

  archive: async (id: number): Promise<Course> => {
    const response = await apiClient.post<ApiResponse<Course>>(
      `/api/v1/courses/${id}/archive`
    );
    return response.data.data!;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/v1/courses/${id}`);
  },
};
