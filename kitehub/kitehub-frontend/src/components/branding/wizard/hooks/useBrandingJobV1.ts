/**
 * useBrandingJobV1 — Wave 34 v1 BrandingJob fetch (sub-GAP-272k).
 *
 * Schema: GET `/api/v1/branding/jobs/{jobId}` per
 * `documents/01-business/kitehub/ai-branding/api-contract.md`.
 *
 * Powers `usePreviewBrandColors` (derives `brandColors`) and any wizard
 * step that needs the canonical job state (status, templateId, audience,
 * tone). DISTINCT from the legacy `useBrandingJob` (`/api/platform/...`)
 * which only returns the older 4-state PROCESSING/COMPLETED shape.
 */

import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { BrandingJobResponse } from './types';

export function useBrandingJobV1(jobId: string | undefined) {
  return useQuery<BrandingJobResponse>({
    queryKey: ['brandingV1', 'jobs', jobId],
    queryFn: async () => {
      const { data } = await apiClient.get<BrandingJobResponse>(
        endpoints.brandingV1.jobById(jobId!)
      );
      return data;
    },
    enabled: !!jobId,
  });
}
