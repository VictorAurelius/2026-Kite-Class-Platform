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
    const response = await apiClient.get<any>(
      '/api/v1/students',
      { params }
    );
    console.log('DEBUG students.ts - Raw response:', response.data);
    console.log('DEBUG students.ts - Unwrapped data:', response.data.data);
    console.log('DEBUG students.ts - Content field:', response.data.data?.content);
    return response.data.data; // Unwrap ApiResponse wrapper
  },

  /**
   * Get student by ID.
   */
  getStudent: async (id: number): Promise<Student> => {
    const response = await apiClient.get<any>(`/api/v1/students/${id}`);
    return response.data.data; // Unwrap ApiResponse wrapper
  },

  /**
   * Create new student.
   */
  createStudent: async (data: CreateStudentRequest): Promise<Student> => {
    const response = await apiClient.post<any>('/api/v1/students', data);
    return response.data.data; // Unwrap ApiResponse wrapper
  },

  /**
   * Update existing student.
   */
  updateStudent: async (
    id: number,
    data: UpdateStudentRequest
  ): Promise<Student> => {
    const response = await apiClient.patch<any>(
      `/api/v1/students/${id}`,
      data
    );
    return response.data.data; // Unwrap ApiResponse wrapper
  },

  /**
   * Soft delete student.
   */
  deleteStudent: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/v1/students/${id}`);
  },
};
