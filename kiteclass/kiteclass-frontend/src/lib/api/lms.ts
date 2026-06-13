/**
 * LMS API functions — KiteClass content-delivery (`/api/v1/lms`).
 *
 * Contract: `documents/01-business/kiteclass/lms/api-contract.md`. The base
 * {@link apiClient} injects Bearer + `X-Tenant-Id`; teacher-authoring endpoints
 * additionally require `X-Teacher-Id` and student/structure reads use `X-User-Id`
 * — both passed per-call (mirrors the `attendance-period.ts` header pattern,
 * since the FE talks to kiteclass-core where the gateway is not in the loop).
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave RBAC-LMS-FE Increment A)
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type {
  CourseModuleDetail,
  CourseModuleSummary,
  LessonDetail,
  Lesson,
  LearningResource,
  CreateModuleRequest,
  UpdateModuleRequest,
  CreateLessonRequest,
  UpdateLessonRequest,
  CreateResourceRequest,
  ReorderRequest,
  PresignedUploadRequest,
  PresignedUploadResponse,
  CompletionRoster,
} from '@/types/lms';

/** X-Teacher-Id header for teacher-authoring endpoints (must be course owner). */
function teacherHeader(teacherId: number) {
  return { headers: { 'X-Teacher-Id': String(teacherId) } };
}

/** X-User-Id header for the structure/lesson reads (present = student/full view). */
function userHeader(userId: number) {
  return { headers: { 'X-User-Id': String(userId) } };
}

export const lmsApi = {
  // ---- Structure reads (dual-mode guest/student) ----

  /**
   * Course structure (modules + lessons). With `userId` → full lessons (student);
   * without → trial lessons only (guest). Teacher authoring passes its own user id
   * to enumerate the structure for editing.
   */
  getCourseStructure: async (
    courseId: number,
    userId?: number,
  ): Promise<CourseModuleDetail[]> => {
    const res = await apiClient.get<ApiResponse<CourseModuleDetail[]>>(
      `/api/v1/lms/courses/${courseId}/modules`,
      userId != null ? userHeader(userId) : undefined,
    );
    return res.data.data ?? [];
  },

  /** Lesson detail (dual-mode). `userId` present → full content if enrolled. */
  getLesson: async (lessonId: number, userId?: number): Promise<LessonDetail> => {
    const res = await apiClient.get<ApiResponse<LessonDetail>>(
      `/api/v1/lms/lessons/${lessonId}`,
      userId != null ? userHeader(userId) : undefined,
    );
    return res.data.data!;
  },

  // ---- Teacher module CRUD (X-Teacher-Id) ----

  getModuleForManage: async (moduleId: number, teacherId: number): Promise<CourseModuleDetail> => {
    const res = await apiClient.get<ApiResponse<CourseModuleDetail>>(
      `/api/v1/lms/modules/${moduleId}`,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  createModule: async (
    courseId: number,
    data: CreateModuleRequest,
    teacherId: number,
  ): Promise<CourseModuleSummary> => {
    const res = await apiClient.post<ApiResponse<CourseModuleSummary>>(
      `/api/v1/lms/courses/${courseId}/modules`,
      data,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  updateModule: async (
    moduleId: number,
    data: UpdateModuleRequest,
    teacherId: number,
  ): Promise<CourseModuleSummary> => {
    const res = await apiClient.put<ApiResponse<CourseModuleSummary>>(
      `/api/v1/lms/modules/${moduleId}`,
      data,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  deleteModule: async (moduleId: number, teacherId: number): Promise<void> => {
    await apiClient.delete(`/api/v1/lms/modules/${moduleId}`, teacherHeader(teacherId));
  },

  reorderModules: async (
    courseId: number,
    data: ReorderRequest,
    teacherId: number,
  ): Promise<CourseModuleSummary[]> => {
    const res = await apiClient.put<ApiResponse<CourseModuleSummary[]>>(
      `/api/v1/lms/courses/${courseId}/modules/reorder`,
      data,
      teacherHeader(teacherId),
    );
    return res.data.data ?? [];
  },

  // ---- Teacher lesson CRUD (X-Teacher-Id) ----

  getLessonForManage: async (lessonId: number, teacherId: number): Promise<LessonDetail> => {
    const res = await apiClient.get<ApiResponse<LessonDetail>>(
      `/api/v1/lms/lessons/${lessonId}/manage`,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  createLesson: async (
    moduleId: number,
    data: CreateLessonRequest,
    teacherId: number,
  ): Promise<Lesson> => {
    const res = await apiClient.post<ApiResponse<Lesson>>(
      `/api/v1/lms/modules/${moduleId}/lessons`,
      data,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  updateLesson: async (
    lessonId: number,
    data: UpdateLessonRequest,
    teacherId: number,
  ): Promise<Lesson> => {
    const res = await apiClient.put<ApiResponse<Lesson>>(
      `/api/v1/lms/lessons/${lessonId}/manage`,
      data,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  deleteLesson: async (lessonId: number, teacherId: number): Promise<void> => {
    await apiClient.delete(`/api/v1/lms/lessons/${lessonId}/manage`, teacherHeader(teacherId));
  },

  reorderLessons: async (
    moduleId: number,
    data: ReorderRequest,
    teacherId: number,
  ): Promise<Lesson[]> => {
    const res = await apiClient.put<ApiResponse<Lesson[]>>(
      `/api/v1/lms/modules/${moduleId}/lessons/reorder`,
      data,
      teacherHeader(teacherId),
    );
    return res.data.data ?? [];
  },

  // ---- Teacher resource (X-Teacher-Id) ----

  createResource: async (
    lessonId: number,
    data: CreateResourceRequest,
    teacherId: number,
  ): Promise<LearningResource> => {
    const res = await apiClient.post<ApiResponse<LearningResource>>(
      `/api/v1/lms/lessons/${lessonId}/resources`,
      data,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  deleteResource: async (resourceId: number, teacherId: number): Promise<void> => {
    await apiClient.delete(`/api/v1/lms/resources/${resourceId}`, teacherHeader(teacherId));
  },

  /** Phase 1 of the 2-phase presigned resource upload (UC-LMS-10). */
  requestUploadUrl: async (
    lessonId: number,
    data: PresignedUploadRequest,
    teacherId: number,
  ): Promise<PresignedUploadResponse> => {
    const res = await apiClient.post<ApiResponse<PresignedUploadResponse>>(
      `/api/v1/lms/lessons/${lessonId}/resources/upload-url`,
      data,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  /** Phase 3 of the upload flow — confirm the file landed in storage. */
  confirmUpload: async (fileId: number): Promise<void> => {
    await apiClient.post(`/api/v1/storage/${fileId}/confirm`);
  },

  // ---- Completion roster (teacher, X-Teacher-Id) ----

  getCompletionRoster: async (
    courseId: number,
    teacherId: number,
  ): Promise<CompletionRoster> => {
    const res = await apiClient.get<ApiResponse<CompletionRoster>>(
      `/api/v1/lms/courses/${courseId}/completion-roster`,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },
};
