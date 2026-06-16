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
  BrandingVersion,
  BrandingVersionPage,
} from '@/types/branding';

const BASE_URL = '/api/v1/settings/branding';

// GAP-1446: version-history + rollback live under a DIFFERENT base path than
// the settings/branding CRUD endpoints — they are tenant-scoped via the path
// instanceId (matches BrandingVersionController @RequestMapping).
const VERSION_BASE_URL = '/api/v1/branding';

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

  /**
   * List branding version history (admin/owner only) — GAP-1446.
   *
   * Returns a raw Spring {@code Page<BrandingVersion>} (newest first). Unlike the
   * settings/branding endpoints, the version controller does NOT wrap the body in
   * {@code ApiResponse}, so the response is read directly (no {@code .data.data}).
   */
  listVersions: async (
    instanceId: string,
    page = 0,
    size = 20
  ): Promise<BrandingVersionPage> => {
    const { data } = await apiClient.get<BrandingVersionPage>(
      `${VERSION_BASE_URL}/${instanceId}/versions`,
      { params: { page, size } }
    );
    return data;
  },

  /**
   * Roll branding back to a specific version (admin/owner only) — GAP-1446.
   *
   * Creates a NEW append-only version entry that restores the target snapshot and
   * returns it. Returns the raw {@code BrandingVersion} (no {@code ApiResponse} wrapper).
   */
  rollback: async (
    instanceId: string,
    versionNumber: number
  ): Promise<BrandingVersion> => {
    const { data } = await apiClient.post<BrandingVersion>(
      `${VERSION_BASE_URL}/${instanceId}/versions/${versionNumber}/rollback`
    );
    return data;
  },
};
