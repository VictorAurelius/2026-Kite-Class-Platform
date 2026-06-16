/**
 * Grade API functions.
 *
 * Contract: kiteclass-core {@code GradeController} (/api/v1/grades). Used by the
 * teacher gradebook (KC-6, GAP-1430) to read each student's real grade
 * components + persist entered scores via the live BE instead of mock data.
 *
 * Per-resource authz (@authz.hasAccessToClass / hasAccessToGrade) gates every
 * endpoint to the owning teacher (or admin/owner bypass) — GAP-996c.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type {
  CreateGradeComponentRequest,
  Grade,
} from '@/types/grade';

export const gradesApi = {
  /**
   * Get a student's grade (with components) for a class.
   *
   * GET /api/v1/grades/student/:studentId/class/:classId
   *
   * Returns {@code null} when the student has no grade row yet (BE 404 before
   * the first {@code initialize}) — callers treat that as "no scores entered".
   */
  getStudentGradeSafe: async (
    studentId: number,
    classId: number,
  ): Promise<Grade | null> => {
    try {
      const response = await apiClient.get<ApiResponse<Grade>>(
        `/api/v1/grades/student/${studentId}/class/${classId}`,
      );
      return response.data.data ?? null;
    } catch (err: unknown) {
      // 404 = grade not yet initialized for this student → no scores to load.
      const status = (err as { response?: { status?: number } })?.response
        ?.status;
      if (status === 404) return null;
      throw err;
    }
  },

  /**
   * Initialize (or fetch existing) grade for a student in a class.
   *
   * POST /api/v1/grades/initialize?studentId=&classId=
   *
   * Idempotent on the server: returns the existing grade if one already exists,
   * so it is safe to call before every component upsert to resolve the gradeId.
   */
  initializeGrade: async (
    studentId: number,
    classId: number,
  ): Promise<Grade> => {
    const response = await apiClient.post<ApiResponse<Grade>>(
      '/api/v1/grades/initialize',
      null,
      { params: { studentId, classId } },
    );
    return response.data.data!;
  },

  /**
   * Add or update a grade component (one assessment score for one student).
   *
   * POST /api/v1/grades/components — upsert key (gradeId, componentType,
   * componentRefId).
   */
  addOrUpdateComponent: async (
    req: CreateGradeComponentRequest,
  ): Promise<void> => {
    await apiClient.post<ApiResponse<unknown>>('/api/v1/grades/components', req);
  },

  /**
   * Recalculate the final score from all components.
   *
   * POST /api/v1/grades/:gradeId/calculate
   */
  calculateFinalScore: async (gradeId: number): Promise<Grade> => {
    const response = await apiClient.post<ApiResponse<Grade>>(
      `/api/v1/grades/${gradeId}/calculate`,
    );
    return response.data.data!;
  },
};
