/**
 * Onboarding API functions.
 *
 * @author KiteClass Team
 * @since 3.17.0
 */

import { apiClient } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';

/**
 * Result of the sample-data import.
 *
 * Mirrors the backend {@code SampleDataResponse} record.
 */
export interface SampleDataResult {
  /** True if sample data already existed (nothing created this call). */
  alreadyImported: boolean;
  teachersCreated: number;
  coursesCreated: number;
  classesCreated: number;
  studentsCreated: number;
  enrollmentsCreated: number;
}

export const onboardingApi = {
  /**
   * Seeds a minimal Vietnamese-edu demo data set (1 teacher + 1 class + 3 students)
   * for the current tenant. Idempotent — a second call returns alreadyImported=true.
   */
  importSampleData: async (): Promise<SampleDataResult> => {
    const response = await apiClient.post<ApiResponse<SampleDataResult>>(
      '/api/v1/onboarding/sample-data'
    );
    return response.data.data!;
  },
};
