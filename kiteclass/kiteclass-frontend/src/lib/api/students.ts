/**
 * Student API functions.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { apiClient } from '@/lib/api-client';
import type {
  Student,
  CreateStudentRequest,
  UpdateStudentRequest,
  StudentSearchParams,
} from '@/types/student';
import type { PaginatedResponse } from '@/types/api';

export const studentsApi = {
  /**
   * Get paginated list of students with search/filter.
   */
  getStudents: async (
    params: StudentSearchParams = {}
  ): Promise<PaginatedResponse<Student>> => {
    const response = await apiClient.get<PaginatedResponse<Student>>(
      '/api/v1/students',
      { params }
    );
    return response.data;
  },

  /**
   * Get student by ID.
   */
  getStudent: async (id: number): Promise<Student> => {
    const response = await apiClient.get<Student>(`/api/v1/students/${id}`);
    return response.data;
  },

  /**
   * Create new student.
   */
  createStudent: async (data: CreateStudentRequest): Promise<Student> => {
    const response = await apiClient.post<Student>('/api/v1/students', data);
    return response.data;
  },

  /**
   * Update existing student.
   */
  updateStudent: async (
    id: number,
    data: UpdateStudentRequest
  ): Promise<Student> => {
    const response = await apiClient.patch<Student>(
      `/api/v1/students/${id}`,
      data
    );
    return response.data;
  },

  /**
   * Soft delete student.
   */
  deleteStudent: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/v1/students/${id}`);
  },
};
