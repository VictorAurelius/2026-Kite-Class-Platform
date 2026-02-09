/**
 * Course domain types.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

export interface Course {
  id: number;
  code: string;
  name: string;
  description?: string;
  credits?: number;
  teacherId: number;
  teacherName?: string;
  capacity?: number;
  enrolled?: number;
  startDate?: string;
  endDate?: string;
  status: CourseStatus;
  createdAt: string;
  updatedAt: string;
}

export enum CourseStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  ARCHIVED = 'ARCHIVED',
}

export interface CreateCourseRequest {
  code: string;
  name: string;
  description?: string;
  credits?: number;
  teacherId: number;
  capacity?: number;
  startDate?: string;
  endDate?: string;
}

export interface UpdateCourseRequest {
  code?: string;
  name?: string;
  description?: string;
  credits?: number;
  teacherId?: number;
  capacity?: number;
  startDate?: string;
  endDate?: string;
  status?: CourseStatus;
}

export interface CourseSearchParams {
  query?: string;
  status?: CourseStatus;
  teacherId?: number;
  page?: number;
  size?: number;
  sort?: string;
}
