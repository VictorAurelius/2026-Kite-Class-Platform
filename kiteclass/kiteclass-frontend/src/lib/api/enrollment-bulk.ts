/**
 * Enrollment bulk-import API client (GAP-1104).
 *
 * Consumes BE endpoints:
 * - GET  /api/v1/enrollments/bulk-import/template  (download xlsx template)
 * - POST /api/v1/enrollments/bulk-import/preview   (resolve + validate, no DB writes)
 * - POST /api/v1/enrollments/bulk-import/commit    (enroll valid rows)
 *
 * @author KiteClass Team
 * @since 3.x (Wave KC enrollment)
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type { EnrollmentBulkResult } from '@/types/enrollment-bulk';

/** Build multipart/form-data body with the file under the {@code file} field. */
function buildFormData(file: File): FormData {
  const fd = new FormData();
  fd.append('file', file, file.name);
  return fd;
}

function buildHeaders(file: File): Record<string, string> {
  return {
    'Content-Type': 'multipart/form-data',
    'X-File-Name': file.name,
  };
}

export const enrollmentBulkApi = {
  /**
   * Download the xlsx template. Returns a Blob the caller saves via object URL.
   */
  downloadTemplate: async (): Promise<Blob> => {
    const response = await apiClient.get<Blob>(
      '/api/v1/enrollments/bulk-import/template',
      { responseType: 'blob' },
    );
    return response.data;
  },

  /**
   * Preview (dry-run). No DB writes. Returns counts + first 10 errors.
   */
  preview: async (file: File): Promise<EnrollmentBulkResult> => {
    const response = await apiClient.post<ApiResponse<EnrollmentBulkResult>>(
      '/api/v1/enrollments/bulk-import/preview',
      buildFormData(file),
      { headers: buildHeaders(file) },
    );
    if (!response.data.data) {
      throw new Error('Phản hồi xem trước rỗng');
    }
    return response.data.data;
  },

  /**
   * Commit. Enrolls valid rows; invalid/failed rows skipped + reported.
   */
  commit: async (file: File): Promise<EnrollmentBulkResult> => {
    const response = await apiClient.post<ApiResponse<EnrollmentBulkResult>>(
      '/api/v1/enrollments/bulk-import/commit',
      buildFormData(file),
      { headers: buildHeaders(file) },
    );
    if (!response.data.data) {
      throw new Error('Phản hồi xác nhận rỗng');
    }
    return response.data.data;
  },
};
