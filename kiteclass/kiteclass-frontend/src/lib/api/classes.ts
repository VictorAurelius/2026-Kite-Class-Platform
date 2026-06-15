/**
 * Class API functions.
 *
 * @author KiteClass Team
 * @since 3.7.0
 */

import { apiClient } from '@/lib/api-client';
import type {
  Class,
  CreateClassRequest,
  UpdateClassRequest,
  CancelClassRequest,
  RescheduleClassRequest,
  ClassSession,
  ClassCodeResponse,
  CreateScheduleRequest,
  GenerateClassCodeRequest,
  ClassSearchCriteria,
  RecurrenceRule,
} from '@/types/class';
import type { ApiResponse, PaginatedResponse } from '@/types/api';

export const classesApi = {
  /**
   * List all classes in the current tenant (paginated, SHARED READ).
   *
   * Tenant-scoped (Hibernate tenantFilter) + readable by any authenticated member
   * (per ClassController OWASP A01 note). Tenant-wide scope — for the student's
   * OWN enrolled classes use `enrollmentsApi.getMine()` (`/api/v1/enrollments/me`,
   * GAP-1285) instead; the student shell no longer relies on this SHARED READ.
   */
  list: async (
    params: { page?: number; size?: number; sort?: string } = {}
  ): Promise<PaginatedResponse<Class>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Class>>>(
      '/api/v1/classes',
      { params: { page: 0, size: 100, ...params } }
    );
    return response.data.data!;
  },

  /**
   * Get classes for a course (paginated)
   */
  getByCourse: async (
    courseId: number,
    params: Omit<ClassSearchCriteria, 'courseId'> = {}
  ): Promise<PaginatedResponse<Class>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Class>>>(
      `/api/v1/courses/${courseId}/classes`,
      { params }
    );
    return response.data.data!;
  },

  /**
   * Get class by ID
   */
  getById: async (id: number): Promise<Class> => {
    const response = await apiClient.get<ApiResponse<Class>>(`/api/v1/classes/${id}`);
    return response.data.data!;
  },

  /**
   * Create a new class within a course
   */
  create: async (courseId: number, data: CreateClassRequest): Promise<Class> => {
    const response = await apiClient.post<ApiResponse<Class>>(
      `/api/v1/courses/${courseId}/classes`,
      data
    );
    return response.data.data!;
  },

  /**
   * Update a class (partial update)
   */
  update: async (id: number, data: UpdateClassRequest): Promise<Class> => {
    const response = await apiClient.patch<ApiResponse<Class>>(
      `/api/v1/classes/${id}`,
      data
    );
    return response.data.data!;
  },

  /**
   * Delete a class (SCHEDULED with 0 enrollments only)
   */
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/v1/classes/${id}`);
  },

  // =========================================================================
  // Lifecycle actions
  // =========================================================================

  /**
   * Start a class (SCHEDULED → IN_PROGRESS)
   */
  start: async (id: number): Promise<Class> => {
    const response = await apiClient.post<ApiResponse<Class>>(
      `/api/v1/classes/${id}/start`
    );
    return response.data.data!;
  },

  /**
   * Complete a class (IN_PROGRESS → COMPLETED)
   */
  complete: async (id: number): Promise<Class> => {
    const response = await apiClient.post<ApiResponse<Class>>(
      `/api/v1/classes/${id}/complete`
    );
    return response.data.data!;
  },

  /**
   * Cancel a class (SCHEDULED/IN_PROGRESS → CANCELLED)
   */
  cancel: async (id: number, data: CancelClassRequest): Promise<Class> => {
    const response = await apiClient.post<ApiResponse<Class>>(
      `/api/v1/classes/${id}/cancel`,
      data
    );
    return response.data.data!;
  },

  /**
   * Reschedule a class — preserves status SCHEDULED, mutates start/end dates,
   * writes audit log, publishes Outbox event (Wave beta-readiness-4 Bucket D — GAP-291).
   *
   * Per cross-bucket LOCKED decision §3.6: reasonCategory MANDATORY (dropdown);
   * reasonNotes optional. Notification classification = OPERATIONAL.
   */
  reschedule: async (id: number, data: RescheduleClassRequest): Promise<Class> => {
    const response = await apiClient.post<ApiResponse<Class>>(
      `/api/v1/classes/${id}/reschedule`,
      data
    );
    return response.data.data!;
  },

  // =========================================================================
  // Class code
  // =========================================================================

  /**
   * Generate or regenerate class enrollment code
   */
  generateCode: async (
    id: number,
    data: GenerateClassCodeRequest = {}
  ): Promise<ClassCodeResponse> => {
    const response = await apiClient.post<ApiResponse<ClassCodeResponse>>(
      `/api/v1/classes/${id}/generate-code`,
      data
    );
    return response.data.data!;
  },

  // =========================================================================
  // Schedule & Sessions
  // =========================================================================

  /**
   * Create class schedule and generate sessions
   */
  createSchedule: async (
    id: number,
    data: CreateScheduleRequest
  ): Promise<ClassSession[]> => {
    const response = await apiClient.post<ApiResponse<ClassSession[]>>(
      `/api/v1/classes/${id}/schedule`,
      data
    );
    return response.data.data!;
  },

  /**
   * Get all sessions for a class
   */
  getSessions: async (id: number): Promise<ClassSession[]> => {
    const response = await apiClient.get<ApiResponse<ClassSession[]>>(
      `/api/v1/classes/${id}/sessions`
    );
    return response.data.data!;
  },

  /**
   * Generate ClassSession entries from a recurrence rule (GAP-290 Wave 18a).
   *
   * Idempotent on edit — preserves attended/past sessions, regenerates future
   * SCHEDULED ones from the new rule.
   *
   * @param id   class ID
   * @param rule recurrence rule (Phase 1: WEEKLY only)
   * @returns merged session list (preserved + new) ordered by sessionNumber
   */
  generateSessionsFromRecurrence: async (
    id: number,
    rule: RecurrenceRule
  ): Promise<ClassSession[]> => {
    // BE RecurrenceRuleDto binds snake_case keys via explicit @JsonProperty
    // (by_day / start_time / end_time / exclude_dates) — camelCase keys arrive as
    // null and fail @NotNull/@NotEmpty (400). Map to the wire contract here.
    const body = {
      freq: rule.freq,
      by_day: rule.byDay,
      start_time: rule.startTime,
      end_time: rule.endTime,
      until: rule.until,
      ...(rule.excludeDates && rule.excludeDates.length > 0
        ? { exclude_dates: rule.excludeDates }
        : {}),
    };
    const response = await apiClient.post<ApiResponse<ClassSession[]>>(
      `/api/v1/classes/${id}/sessions/generate-from-recurrence`,
      body
    );
    return response.data.data!;
  },
};
