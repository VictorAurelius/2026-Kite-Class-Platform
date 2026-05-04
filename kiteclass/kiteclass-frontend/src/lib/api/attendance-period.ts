/**
 * Period Attendance API client (Phase 1B v1, GAP-323b).
 *
 * Wraps the K-12 per-tiết attendance endpoints from PR #769:
 * - POST /api/v1/attendance/periods            — idempotent batch upsert
 * - PATCH /api/v1/attendance/periods/{id}      — single-row update with optimistic lock
 * - GET /api/v1/attendance/periods/classes/{classId}?date=  — daily roster
 *
 * Sends X-Tenant-Id (already injected by api-client interceptor) and
 * X-Teacher-Id headers per `documents/01-business/kiteclass/period-attendance/api-contract.md`.
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

import { apiClient } from '@/lib/api-client';

/** Period attendance status values (mirrors backend enum). */
export type AttendancePeriodStatus =
  | 'PRESENT'
  | 'ABSENT'
  | 'LATE'
  | 'EXCUSED'
  | 'MAKEUP';

/** A single recorded period attendance row. */
export interface AttendancePeriodResponse {
  id: number;
  studentId: number;
  classId: number;
  subjectSectionId: number;
  periodNo: number;
  date: string; // ISO YYYY-MM-DD
  status: AttendancePeriodStatus;
  recordedBy: number;
  recordedAt: string; // ISO datetime
  notes: string | null;
  version?: number;
  createdAt: string;
  updatedAt: string | null;
}

/** Single entry in the batch upsert payload. */
export interface AttendancePeriodBatchEntry {
  studentId: number;
  classId: number;
  subjectSectionId: number;
  periodNo: number;
  date: string;
  status: AttendancePeriodStatus;
  notes?: string | null;
}

/** Body for POST /api/v1/attendance/periods (≤60 entries). */
export interface AttendancePeriodBatchCreateRequest {
  entries: AttendancePeriodBatchEntry[];
}

/** Body for PATCH /api/v1/attendance/periods/{id}. */
export interface AttendancePeriodUpdateRequest {
  status: AttendancePeriodStatus;
  notes?: string | null;
  version: number;
}

/**
 * Some KC endpoints unwrap a `data` envelope, others return the payload
 * directly. The Phase 1B v1 backend (#769) returns the period payloads
 * raw, but we tolerate both shapes for forward compatibility with the
 * platform-wide ApiResponse wrapper used elsewhere.
 */
function unwrap<T>(payload: unknown): T {
  if (
    payload !== null &&
    typeof payload === 'object' &&
    'data' in payload &&
    (payload as { data?: unknown }).data !== undefined
  ) {
    return (payload as { data: T }).data;
  }
  return payload as T;
}

interface AttendancePeriodApiOptions {
  /** Resolved teacher (user) ID. Sent as `X-Teacher-Id`. */
  teacherId: number;
}

export const attendancePeriodApi = {
  /**
   * Fetch the daily roster (all student × period rows) for one class on one date.
   */
  getDailyRoster: async (
    classId: number,
    date: string,
  ): Promise<AttendancePeriodResponse[]> => {
    const response = await apiClient.get(
      `/api/v1/attendance/periods/classes/${classId}`,
      { params: { date } },
    );
    return unwrap<AttendancePeriodResponse[]>(response.data);
  },

  /**
   * Idempotent batch upsert. Backend dedupes by V50 unique tuple
   * `(studentId, subjectSectionId, date, periodNo)` per tenant.
   */
  upsertBatch: async (
    body: AttendancePeriodBatchCreateRequest,
    opts: AttendancePeriodApiOptions,
  ): Promise<AttendancePeriodResponse[]> => {
    const response = await apiClient.post(
      '/api/v1/attendance/periods',
      body,
      { headers: { 'X-Teacher-Id': String(opts.teacherId) } },
    );
    return unwrap<AttendancePeriodResponse[]>(response.data);
  },

  /**
   * Update status / notes for a single recorded row with optimistic-lock check.
   * 409 → caller refreshes the row and re-attempts.
   */
  updateOne: async (
    id: number,
    body: AttendancePeriodUpdateRequest,
    opts: AttendancePeriodApiOptions,
  ): Promise<AttendancePeriodResponse> => {
    const response = await apiClient.patch(
      `/api/v1/attendance/periods/${id}`,
      body,
      { headers: { 'X-Teacher-Id': String(opts.teacherId) } },
    );
    return unwrap<AttendancePeriodResponse>(response.data);
  },
};
