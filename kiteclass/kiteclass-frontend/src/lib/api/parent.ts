/**
 * Parent portal API client.
 *
 * Phase 1A (GAP-321): transcript read-only + reuse Wave 2 GAP-052a
 * `/me/children` endpoint for the children selector.
 *
 * @author KiteClass Team
 * @since 2.18.0 (Wave 18b1 — GAP-321 Phase 1A)
 */

import { apiClient } from '@/lib/api-client';
import type {
  ChildSummary,
  ParentProfile,
  TranscriptEntry,
} from '@/types/parent';
import type { ApiResponse } from '@/types/api';

export const parentApi = {
  /** GET /api/v1/parent/me — current parent's own profile (Wave 2 endpoint). */
  getMe: async (): Promise<ParentProfile> => {
    const response = await apiClient.get<ApiResponse<ParentProfile>>(
      '/api/v1/parent/me',
    );
    return response.data.data!;
  },

  /**
   * GET /api/v1/parent/me/children — list of linked children for the dashboard
   * selector (Wave 2 endpoint reused per BR-PARENT-PORTAL-007).
   */
  getMyChildren: async (): Promise<ChildSummary[]> => {
    const response = await apiClient.get<ApiResponse<ChildSummary[]>>(
      '/api/v1/parent/me/children',
    );
    return response.data.data ?? [];
  },

  /**
   * GET /api/v1/parent/children/{childId}/transcript — transcript list for one
   * linked child, newest semester first (Phase 1A — GAP-321).
   *
   * Server-side enforces BR-PARENT-PORTAL-001 scope guard via
   * `existsByParentIdAndStudentIdAndDeletedFalse` BEFORE any data access.
   * Returns 403 PARENT_NOT_LINKED for unlinked children — do NOT mask this on
   * the client; bubble up so downstream toast can flag IDOR probing attempts.
   */
  getChildTranscript: async (childId: number): Promise<TranscriptEntry[]> => {
    const response = await apiClient.get<ApiResponse<TranscriptEntry[]>>(
      `/api/v1/parent/children/${childId}/transcript`,
    );
    return response.data.data ?? [];
  },
};
