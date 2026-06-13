/**
 * Assignment API — KiteClass per-class assignments (`/api/v1/assignments`, GAP-1113 Bucket D).
 *
 * Contract: kiteclass-core `AssignmentController`. Teacher give/grade ops require
 * `X-Teacher-Id`; student submit requires `X-User-Id`. The class-scoped teacher list
 * + pending-grading use `@authz.hasAccessToClass` (no extra header — JWT-derived).
 * The base {@link apiClient} injects Bearer + `X-Tenant-Id`.
 *
 * @author KiteClass Team
 * @since GAP-1113 (Wave RBAC-LMS-FE Increment A)
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type {
  Assignment,
  Submission,
  CreateAssignmentRequest,
  UpdateAssignmentRequest,
  GradeSubmissionRequest,
  SubmitAssignmentRequest,
} from '@/types/assignment';

function teacherHeader(teacherId: number) {
  return { headers: { 'X-Teacher-Id': String(teacherId) } };
}

export const assignmentsApi = {
  // ---- Teacher give (X-Teacher-Id) ----

  create: async (data: CreateAssignmentRequest, teacherId: number): Promise<Assignment> => {
    const res = await apiClient.post<ApiResponse<Assignment>>(
      '/api/v1/assignments',
      data,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  update: async (id: number, data: UpdateAssignmentRequest, teacherId: number): Promise<Assignment> => {
    const res = await apiClient.put<ApiResponse<Assignment>>(
      `/api/v1/assignments/${id}`,
      data,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  publish: async (id: number, teacherId: number): Promise<Assignment> => {
    const res = await apiClient.post<ApiResponse<Assignment>>(
      `/api/v1/assignments/${id}/publish`,
      undefined,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  close: async (id: number, teacherId: number): Promise<Assignment> => {
    const res = await apiClient.post<ApiResponse<Assignment>>(
      `/api/v1/assignments/${id}/close`,
      undefined,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  remove: async (id: number, teacherId: number): Promise<void> => {
    await apiClient.delete(`/api/v1/assignments/${id}`, teacherHeader(teacherId));
  },

  // ---- Reads ----

  /** Teacher view of a class's assignments (includes drafts). */
  getByClass: async (classId: number): Promise<Assignment[]> => {
    const res = await apiClient.get<ApiResponse<Assignment[]>>(
      `/api/v1/assignments/class/${classId}`,
    );
    return res.data.data ?? [];
  },

  /** Published assignments only (student view). */
  getPublishedByClass: async (classId: number): Promise<Assignment[]> => {
    const res = await apiClient.get<ApiResponse<Assignment[]>>(
      `/api/v1/assignments/class/${classId}/published`,
    );
    return res.data.data ?? [];
  },

  getById: async (id: number): Promise<Assignment> => {
    const res = await apiClient.get<ApiResponse<Assignment>>(`/api/v1/assignments/${id}`);
    return res.data.data!;
  },

  getSubmissions: async (assignmentId: number): Promise<Submission[]> => {
    const res = await apiClient.get<ApiResponse<Submission[]>>(
      `/api/v1/assignments/${assignmentId}/submissions`,
    );
    return res.data.data ?? [];
  },

  // ---- Teacher grade (X-Teacher-Id) ----

  grade: async (
    submissionId: number,
    data: GradeSubmissionRequest,
    teacherId: number,
  ): Promise<Submission> => {
    const res = await apiClient.post<ApiResponse<Submission>>(
      `/api/v1/assignments/submissions/${submissionId}/grade`,
      data,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  returnSubmission: async (submissionId: number, teacherId: number): Promise<Submission> => {
    const res = await apiClient.post<ApiResponse<Submission>>(
      `/api/v1/assignments/submissions/${submissionId}/return`,
      undefined,
      teacherHeader(teacherId),
    );
    return res.data.data!;
  },

  // ---- Student submit (X-User-Id) — thin stub, full surface gated KC-9 ----

  submit: async (data: SubmitAssignmentRequest, studentId: number): Promise<Submission> => {
    const res = await apiClient.post<ApiResponse<Submission>>(
      '/api/v1/assignments/submit',
      data,
      { headers: { 'X-User-Id': String(studentId) } },
    );
    return res.data.data!;
  },
};
