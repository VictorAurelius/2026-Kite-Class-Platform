/**
 * Branding API module.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { apiClient } from '@/lib/api-client';
import type { Branding, UpdateBrandingRequest, UploadLogoResponse } from '@/types/branding';

const BASE_URL = '/api/v1/settings/branding';

export const brandingApi = {
  /**
   * Get current branding settings
   */
  get: async (): Promise<Branding> => {
    const { data } = await apiClient.get<Branding>(BASE_URL);
    return data;
  },

  /**
   * Update branding settings (admin only)
   */
  update: async (request: UpdateBrandingRequest): Promise<Branding> => {
    const { data } = await apiClient.put<Branding>(BASE_URL, request);
    return data;
  },

  /**
   * Upload logo (admin only)
   */
  uploadLogo: async (file: File): Promise<UploadLogoResponse> => {
    const formData = new FormData();
    formData.append('logo', file);

    const { data } = await apiClient.post<UploadLogoResponse>(
      `${BASE_URL}/logo`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return data;
  },
};
