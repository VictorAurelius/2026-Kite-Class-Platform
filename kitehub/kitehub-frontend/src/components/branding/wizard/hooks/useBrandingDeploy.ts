/**
 * useBrandingDeploy — Wave deploy-pipeline (GAP-1021).
 *
 * Two mutations powering the AI Branding wizard Step 6 deploy flow:
 *   1. `useCreateBrandingJobV1` — POST `/api/v1/branding/jobs` creates a REAL
 *      BrandingJob so `wizardState.jobId` becomes non-empty (root cause: the
 *      legacy TemplateStep sentinel left jobId='' → useDeployStream never enabled
 *      → "Đang chờ log" forever).
 *   2. `useApproveBrandingJob` — POST `/api/v1/branding/jobs/{jobId}/approve`
 *      persists the approved theme + triggers backend MOCK provisioning (lifecycle
 *      NOT_STARTED→INITIALIZING→GENERATING→DEPLOYED). Returns 202; the SSE
 *      deploy-stream surfaces progress.
 *
 * Schema source-of-truth: `documents/01-business/kitehub/ai-branding/api-contract.md`.
 */

import { useMutation } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { BrandingJobResponse } from './types';

export interface CreateBrandingJobInput {
  slug?: string;
  organizationName?: string;
  language?: string;
  audience?: string | null;
  tone?: string | null;
  templateId?: string | null;
  logoUrl?: string | null;
  aiLogo?: boolean;
}

/** Create a wizard branding job; returns the persisted job (incl. jobId). */
export function useCreateBrandingJobV1() {
  return useMutation<BrandingJobResponse, unknown, CreateBrandingJobInput>({
    mutationFn: async (input) => {
      const { data } = await apiClient.post<BrandingJobResponse>(
        endpoints.brandingV1.jobs,
        input,
      );
      return data;
    },
  });
}

export interface ApproveBrandingJobInput {
  jobId: string;
  slug?: string;
  templateId?: string | null;
  approvedResources?: string[];
}

export interface ApproveBrandingJobResponse {
  jobId: string;
  status: string;
  frontendUrl: string;
  message: string;
}

/** Approve theme + trigger mock deploy provisioning (returns 202 immediately). */
export function useApproveBrandingJob() {
  return useMutation<ApproveBrandingJobResponse, unknown, ApproveBrandingJobInput>({
    mutationFn: async ({ jobId, slug, templateId, approvedResources }) => {
      const { data } = await apiClient.post<ApproveBrandingJobResponse>(
        endpoints.brandingV1.jobApprove(jobId),
        { slug, templateId, approvedResources },
      );
      return data;
    },
  });
}
