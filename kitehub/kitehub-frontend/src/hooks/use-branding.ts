import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { BrandingAsset, BrandingJob, LogoAnalysis, MarketingContent } from '@/types/branding';
import type { ApiResponse } from '@/types/api';

/**
 * Upload asset (logo, etc.)
 */
export function useUploadAsset() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      instanceId,
      type,
      file,
    }: {
      instanceId: string;
      type: string;
      file: File;
    }) => {
      const formData = new FormData();
      formData.append('file', file);

      // No manual Content-Type: a boundary-less 'multipart/form-data' breaks BE
      // @RequestPart parsing. The apiClient request interceptor drops Content-Type
      // for FormData so the browser sets multipart/form-data WITH the boundary
      // (GAP-1073 cross-flow sweep).
      const { data } = await apiClient.post<ApiResponse<BrandingAsset>>(
        endpoints.branding.uploadAsset(instanceId, type),
        formData
      );
      return data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['branding', 'assets'] });
    },
  });
}

/**
 * Analyze logo to extract colors and theme
 */
export function useAnalyzeLogo() {
  return useMutation({
    mutationFn: async (logoUrl: string) => {
      const { data } = await apiClient.post<ApiResponse<LogoAnalysis>>(
        endpoints.branding.analyzeLogo,
        { logoUrl }
      );
      return data.data;
    },
  });
}

/**
 * Create branding job
 */
export function useCreateBrandingJob() {
  return useMutation({
    mutationFn: async (request: {
      instanceId: string;
      logoUrl: string;
      analysis: LogoAnalysis;
    }) => {
      const { data } = await apiClient.post<ApiResponse<BrandingJob>>(
        endpoints.branding.jobs,
        request
      );
      return data.data;
    },
  });
}

/**
 * Get branding job with auto-polling for PROCESSING status
 * Auto-refreshes every 2s while job is PROCESSING
 */
export function useBrandingJob(jobId: string | undefined) {
  return useQuery({
    queryKey: ['branding', 'jobs', jobId],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<BrandingJob>>(
        endpoints.branding.jobById(jobId!)
      );
      return data.data;
    },
    enabled: !!jobId,
    // Auto-refetch every 2s if job is PROCESSING
    refetchInterval: (query) => {
      const job = query.state.data;
      return job?.status === 'PROCESSING' ? 2000 : false;
    },
    // Stop refetching when tab is not visible (save resources)
    refetchIntervalInBackground: false,
  });
}

/**
 * Get job assets
 */
export function useJobAssets(jobId: string | undefined) {
  return useQuery({
    queryKey: ['branding', 'jobs', jobId, 'assets'],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<BrandingAsset[]>>(
        endpoints.branding.jobAssets(jobId!)
      );
      return data.data;
    },
    enabled: !!jobId,
  });
}

/**
 * List all assets for instance
 */
export function useAssets(instanceId: string | undefined) {
  return useQuery({
    queryKey: ['branding', 'assets', instanceId],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<BrandingAsset[]>>(
        endpoints.branding.listAssets(instanceId!)
      );
      return data.data;
    },
    enabled: !!instanceId,
  });
}

/**
 * Generate marketing content
 */
export function useGenerateContent() {
  return useMutation({
    mutationFn: async (request: {
      instanceId: string;
      analysis: LogoAnalysis;
    }) => {
      const { data } = await apiClient.post<ApiResponse<MarketingContent>>(
        endpoints.branding.generateContent,
        request
      );
      return data.data;
    },
  });
}
