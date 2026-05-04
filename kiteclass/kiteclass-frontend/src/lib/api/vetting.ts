/**
 * Vetting workflow API client (RBAC: SAFEGUARDING_OFFICER).
 *
 * Phase 1B remainder (Wave 18b3 — GAP-322b): single-file LLTP upload.
 * Verify-queue browse + transition flows ship in Phase 1C (GAP-322c).
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b3 — GAP-322b Phase 1B remainder)
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';

/** Response from {@code POST /api/v1/vettings/{vettingId}/documents}. */
export interface VettingDocumentResponse {
  vettingId: number;
  storageKey: string;
  sizeBytes: number;
  contentType: string | null;
}

export const vettingApi = {
  /**
   * Upload a single LLTP / CCCD / police-check evidence document for a
   * vetting record.
   *
   * Server enforces 10MB cap + RBAC SAFEGUARDING_OFFICER (Gateway forwards
   * X-User-Roles header). Clients SHOULD validate size + type before posting
   * to avoid uploading then rejecting.
   *
   * @param vettingId target vetting record id
   * @param file      browser File from <input type="file">
   */
  uploadDocument: async (
    vettingId: number,
    file: File,
  ): Promise<VettingDocumentResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<ApiResponse<VettingDocumentResponse>>(
      `/api/v1/vettings/${vettingId}/documents`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      },
    );
    return response.data.data!;
  },
};
