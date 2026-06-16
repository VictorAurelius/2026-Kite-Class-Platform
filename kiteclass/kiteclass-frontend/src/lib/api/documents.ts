/**
 * Document generation API client (GAP-1434).
 *
 * Consumes the real BE document-gen controller (Sub-PR 5.5, ADR-019):
 *   - POST /api/v1/documents/{format}/download  (Content-Disposition: attachment)
 *   - POST /api/v1/documents/{format}/preview    (PDF inline preview)
 *
 * Tenant + branding are resolved server-side from the session; callers only
 * supply the template id + template-specific data map. Returns a Blob the
 * caller saves via an object URL. Branding is applied server-side, so the FE
 * stays thin (no PDF rendering in the browser).
 *
 * Wave 5 ships exactly one template: `invoice` (Vietnamese tax invoice / receipt).
 */

import { apiClient } from '@/lib/api-client';

/** Supported output formats per BE {@code DocumentFormat}. */
export type DocumentFormat = 'pdf' | 'xlsx' | 'docx';

export interface DocumentDownloadRequest {
  /** Template id registered server-side (e.g. `invoice`). */
  templateId: string;
  /** Template-specific data map; shape depends on the template. */
  data?: Record<string, unknown>;
}

export const documentsApi = {
  /**
   * Generate a branded document and return it as a downloadable Blob.
   *
   * @param format output format (`pdf` | `xlsx` | `docx`)
   * @param body   template id + data map
   * @returns the generated document bytes as a Blob
   */
  download: async (
    format: DocumentFormat,
    body: DocumentDownloadRequest,
  ): Promise<Blob> => {
    const response = await apiClient.post<Blob>(
      `/api/v1/documents/${format}/download`,
      body,
      { responseType: 'blob' },
    );
    return response.data;
  },
};
