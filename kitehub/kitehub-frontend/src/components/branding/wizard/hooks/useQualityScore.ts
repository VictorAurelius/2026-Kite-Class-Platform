/**
 * useQualityScore — Wave 34 §5 Quality Gate fetch (GAP-272c).
 *
 * Schema: GET `/api/v1/branding/jobs/{jobId}/quality-score` per
 * `ai-branding-guidelines.md` §5 + `api-contract.md` Wave 34.
 *
 * Replaces Wave 32 inline placeholder ("Đang chấm điểm…" stub in
 * Step6Preview). The hook does NOT auto-poll: backend computes score after
 * REVIEW step completes and emits a `quality-score-computed` lifecycle
 * event; consumers re-fetch on that signal (or SSE event in
 * `useDeployStream`).
 */

import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { QualityScoreResponse } from './types';

export function useQualityScore(jobId: string | undefined) {
  return useQuery<QualityScoreResponse>({
    queryKey: ['brandingV1', 'qualityScore', jobId],
    queryFn: async () => {
      const { data } = await apiClient.get<QualityScoreResponse>(
        endpoints.brandingV1.jobQualityScore(jobId!)
      );
      return data;
    },
    enabled: !!jobId,
    // 409 QUALITY_SCORE_NOT_READY is treated as a soft failure — the
    // consumer renders the placeholder and lets the user wait for the
    // SSE state-change to refetch.
    retry: false,
  });
}
