/**
 * Landing-page admin API (GAP-826).
 *
 * Read + update the current tenant's landing page. The tenant id is taken from the
 * authenticated session (JWT `tenantId` claim) and put in the path; the gateway
 * validates it against the JWT, so callers never spoof another tenant.
 *
 * Used by the branding settings "Banner landing" card to manage the hero banner
 * carousel (heroImages). A partial PUT (only the fields supplied) updates the row.
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';

export interface LandingPageData {
  heroImageUrl?: string | null;
  /** Ordered hero banner carousel image URLs (GAP-826). */
  heroImages?: string[] | null;
  [key: string]: unknown;
}

export interface UpdateLandingRequest {
  heroImages?: string[];
  heroImageUrl?: string;
  [key: string]: unknown;
}

const url = (tenantId: string) => `/api/v1/tenants/${tenantId}/landing`;

export const landingApi = {
  /** Get the current tenant's landing page content. */
  get: async (tenantId: string): Promise<LandingPageData> => {
    const { data } = await apiClient.get<ApiResponse<LandingPageData>>(url(tenantId));
    return data.data!;
  },

  /** Partial update of the current tenant's landing page (admin/teacher). */
  update: async (tenantId: string, request: UpdateLandingRequest): Promise<LandingPageData> => {
    const { data } = await apiClient.put<ApiResponse<LandingPageData>>(url(tenantId), request);
    return data.data!;
  },
};
