/**
 * useRegenerateQuota — Wave 34 regenerate quota + dispatch (GAP-272d).
 *
 * Two endpoints per `api-contract.md` Wave 34:
 *   GET  `/api/v1/branding/regenerate-quota`              → tier + used + limit
 *   POST `/api/v1/branding/jobs/{jobId}/regenerate`       → BrandingJob (returned)
 *
 * Replaces Wave 32 inline tier-quota constants in `RegenerateCounter`
 * callers.
 *
 * Per `ai-branding-guidelines.md` §4.3 the quota cap is tier-driven
 * (FREE=3 / PRO=10 / PREMIUM=30 / ENTERPRISE=-1). The hook does NOT
 * hardcode caps — it surfaces server-returned `limit` only.
 *
 * Idempotency:
 *   POST /regenerate REQUIRES an `Idempotency-Key` header (UUID v4).
 *   The hook generates a fresh UUID per call via `crypto.randomUUID()`
 *   so identical user clicks within the 10-minute server window resolve
 *   to the same job (server convention per contract).
 *
 * Response-shape inconsistency note (Bucket A vs Bucket B):
 *   POST /regenerate (Bucket A) currently returns the underlying
 *   `BrandingJob` entity (no `brandColors` field).
 *   GET /jobs/{id}    (Bucket B) returns the `BrandingJobResponse`
 *   wrapper (WITH `brandColors`).
 *   The mutation typing here is loose so consumers don't crash on the
 *   missing `brandColors` field. Follow-up gap will align Bucket A
 *   on Bucket B's wrapper — see PR description.
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type {
  BrandingJobResponse,
  RegenerateQuotaResponse,
} from './types';

/** Generate an Idempotency-Key. Falls back when crypto.randomUUID is missing. */
function newIdempotencyKey(): string {
  if (
    typeof globalThis !== 'undefined' &&
    globalThis.crypto &&
    typeof globalThis.crypto.randomUUID === 'function'
  ) {
    return globalThis.crypto.randomUUID();
  }
  // RFC 4122 v4-ish fallback (test/SSR only; production uses native crypto).
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export function useRegenerateQuota() {
  const queryClient = useQueryClient();

  const quotaQuery = useQuery<RegenerateQuotaResponse>({
    queryKey: ['brandingV1', 'regenerateQuota'],
    queryFn: async () => {
      const { data } = await apiClient.get<RegenerateQuotaResponse>(
        endpoints.brandingV1.regenerateQuota
      );
      return data;
    },
  });

  const regenerateMutation = useMutation<
    BrandingJobResponse,
    Error,
    { jobId: string; instanceId: string }
  >({
    mutationFn: async ({ jobId, instanceId }) => {
      // GAP-1145: BrandingWizardController.regenerate returns 400 MISSING_INSTANCE_ID
      // when X-Instance-Id is absent — the gateway maps tenantId→X-Tenant-Id but
      // never sets X-Instance-Id, so the FE MUST send it explicitly (the deploy
      // tenant claim from wizardState.instanceId).
      const { data } = await apiClient.post<BrandingJobResponse>(
        endpoints.brandingV1.jobRegenerate(jobId),
        undefined,
        {
          headers: {
            'Idempotency-Key': newIdempotencyKey(),
            'X-Instance-Id': instanceId,
          },
        }
      );
      return data;
    },
    onSuccess: (data) => {
      // Quota counter and the v1 job both shifted server-side; invalidate.
      queryClient.invalidateQueries({ queryKey: ['brandingV1', 'regenerateQuota'] });
      if (data.jobId) {
        queryClient.invalidateQueries({ queryKey: ['brandingV1', 'jobs', data.jobId] });
      }
    },
  });

  return {
    /** Quota query — fields per `RegenerateQuotaResponse`. */
    quota: quotaQuery,
    /** POST /regenerate mutation — call `mutate({ jobId })`. */
    regenerate: regenerateMutation,
  };
}
