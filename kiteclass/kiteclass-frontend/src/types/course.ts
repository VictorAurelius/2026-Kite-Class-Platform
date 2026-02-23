/**
 * Course domain types.
 * Matches backend CourseResponse and CreateCourseRequest DTOs.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

export interface Course {
  id: number;
  name: string;
  code: string;
  description?: string;
  syllabus?: string;
  objectives?: string;
  prerequisites?: string;
  targetAudience?: string;
  teacherId?: number;
  durationWeeks?: number;
  totalSessions?: number;
  price?: number;
  status: CourseStatus;
  coverImageUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export enum CourseStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED',
}

export interface CreateCourseRequest {
  name: string;
  code: string;
  description?: string;
  syllabus?: string;
  objectives?: string;
  prerequisites?: string;
  targetAudience?: string;
  teacherId?: number;
  durationWeeks?: number;
  totalSessions?: number;
  price?: number;
  coverImageUrl?: string;
}

export interface UpdateCourseRequest {
  name?: string;
  code?: string;
  description?: string;
  syllabus?: string;
  objectives?: string;
  prerequisites?: string;
  targetAudience?: string;
  teacherId?: number;
  durationWeeks?: number;
  totalSessions?: number;
  price?: number;
  coverImageUrl?: string;
}

export interface CourseSearchParams {
  query?: string;
  status?: CourseStatus;
  teacherId?: number;
  page?: number;
  size?: number;
  sort?: string;
}
