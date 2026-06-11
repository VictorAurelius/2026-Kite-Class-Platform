/**
 * Branding API module.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type {
  Branding,
  UpdateBrandingRequest,
  UploadLogoResponse,
  BannerUploadResponse,
} from '@/types/branding';

const BASE_URL = '/api/v1/settings/branding';

export const brandingApi = {
  /**
   * Get current branding settings
   */
  get: async (): Promise<Branding> => {
    const { data } = await apiClient.get<ApiResponse<Branding>>(BASE_URL);
    return data.data!; // Unwrap ApiResponse wrapper
  },

  /**
   * Update branding settings (admin only)
   */
  update: async (request: UpdateBrandingRequest): Promise<Branding> => {
    const { data } = await apiClient.put<ApiResponse<Branding>>(BASE_URL, request);
    return data.data!; // Unwrap ApiResponse wrapper
  },

  /**
   * Upload logo (admin only).
   *
   * Sends the raw image as multipart/form-data field `logo`. The browser sets
   * the multipart boundary automatically — do NOT hardcode the Content-Type
   * header (a manual value omits the boundary and breaks the upload).
   */
  uploadLogo: async (file: File): Promise<UploadLogoResponse> => {
    const formData = new FormData();
    formData.append('logo', file);

    const { data } = await apiClient.post<ApiResponse<UploadLogoResponse>>(
      `${BASE_URL}/logo`,
      formData
    );
    return data.data!; // Unwrap ApiResponse wrapper
  },

  /**
   * Upload favicon (admin only).
   *
   * Multipart/form-data field `favicon`. See uploadLogo for the Content-Type note.
   */
  uploadFavicon: async (file: File): Promise<UploadLogoResponse> => {
    const formData = new FormData();
    formData.append('favicon', file);

    const { data } = await apiClient.post<ApiResponse<UploadLogoResponse>>(
      `${BASE_URL}/favicon`,
      formData
    );
    return data.data!; // Unwrap ApiResponse wrapper
  },

  /**
   * Upload a landing banner (admin only) — GAP-1211.
   *
   * Multipart/form-data field `banner`. Unlike logo/favicon, each upload stores a
   * new image (no overwrite) and returns its renderable URL. See uploadLogo for the
   * Content-Type note (browser sets the multipart boundary — do not hardcode it).
   */
  uploadBanner: async (file: File): Promise<BannerUploadResponse> => {
    const formData = new FormData();
    formData.append('banner', file);

    const { data } = await apiClient.post<ApiResponse<BannerUploadResponse>>(
      `${BASE_URL}/banners`,
      formData
    );
    return data.data!; // Unwrap ApiResponse wrapper
  },
};
