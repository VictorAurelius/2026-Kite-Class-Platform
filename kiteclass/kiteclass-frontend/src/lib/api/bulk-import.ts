/**
 * Bulk Import API client.
 *
 * Consumes BE endpoints (Wave 1 GAP-051):
 * - POST /api/v1/students/bulk-import/preview  (parse + validate, no DB writes)
 * - POST /api/v1/students/bulk-import/commit   (parse + validate + create)
 * - POST /api/v1/students/bulk-import/jobs/{id}/errors  (xlsx error report)
 * - GET  /api/v1/students/bulk-import/template (blank import template, GAP-1102)
 *
 * @author KiteClass Team
 * @since 3.60.0
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type { BulkImportResult } from '@/types/bulk-import';

/**
 * Build multipart/form-data body with the uploaded file under the {@code file}
 * field name (matches BE {@code @RequestParam("file")}).
 */
function buildFormData(file: File): FormData {
  const fd = new FormData();
  fd.append('file', file, file.name);
  return fd;
}

/**
 * Some jsdom + MSW combos lose `File.name` after multipart serialization,
 * so we also stamp the filename as a custom header. Real BE ignores this
 * header (Spring binds the multipart file by its original filename).
 */
function buildHeaders(file: File): Record<string, string> {
  return {
    'Content-Type': 'multipart/form-data',
    'X-File-Name': file.name,
  };
}

export const bulkImportApi = {
  /**
   * Preview (dry-run). No DB writes. Returns counts + first 10 errors.
   */
  preview: async (file: File): Promise<BulkImportResult> => {
    const response = await apiClient.post<ApiResponse<BulkImportResult>>(
      '/api/v1/students/bulk-import/preview',
      buildFormData(file),
      {
        headers: buildHeaders(file),
      },
    );
    if (!response.data.data) {
      throw new Error('Phản hồi preview rỗng');
    }
    return response.data.data;
  },

  /**
   * Commit. Creates valid students + returns jobId. Invalid rows skipped.
   */
  commit: async (file: File): Promise<BulkImportResult> => {
    const response = await apiClient.post<ApiResponse<BulkImportResult>>(
      '/api/v1/students/bulk-import/commit',
      buildFormData(file),
      {
        headers: buildHeaders(file),
      },
    );
    if (!response.data.data) {
      throw new Error('Phản hồi commit rỗng');
    }
    return response.data.data;
  },

  /**
   * Download the blank import template (GAP-1102). Static + tenant-agnostic —
   * no file/jobId/tenant needed. Returns a Blob the caller saves via object URL
   * so users grab the exact columns BEFORE filling in their data.
   */
  downloadTemplate: async (): Promise<Blob> => {
    const response = await apiClient.get<Blob>(
      '/api/v1/students/bulk-import/template',
      {
        responseType: 'blob',
      },
    );
    return response.data;
  },

  /**
   * Download xlsx error report. Stateless MVP — server re-validates the
   * original file. Returns a Blob the caller saves via object URL.
   */
  downloadErrorReport: async (jobId: number, file: File): Promise<Blob> => {
    const response = await apiClient.post<Blob>(
      `/api/v1/students/bulk-import/jobs/${jobId}/errors`,
      buildFormData(file),
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        responseType: 'blob',
      },
    );
    return response.data;
  },
};
