import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type {
  BrandingAsset,
  BrandingDeployStatus,
  BrandingJob,
  LogoAnalysis,
  MarketingContent,
} from '@/types/branding';

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
      // GAP-1082: kitehub-branding returns a BARE <T> body (no ApiResponse
      // wrapper anywhere in the service), so read `data` directly — NOT data.data.
      const { data } = await apiClient.post<BrandingAsset>(
        endpoints.branding.uploadAsset(instanceId, type),
        formData
      );
      return data;
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
      // GAP-1082: bare <T> body (AIBrandingController returns ResponseEntity.ok(result)).
      const { data } = await apiClient.post<LogoAnalysis>(
        endpoints.branding.analyzeLogo,
        { logoUrl }
      );
      return data;
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
      // GAP-1082: bare <T> body (BrandingJobController returns ResponseEntity.status(201).body(job)).
      const { data } = await apiClient.post<BrandingJob>(
        endpoints.branding.jobs,
        request
      );
      return data;
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
      // GAP-1082: bare <T> body (BrandingJobController @GetMapping returns ResponseEntity.ok(job)).
      const { data } = await apiClient.get<BrandingJob>(
        endpoints.branding.jobById(jobId!)
      );
      return data;
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
      // GAP-1082: bare body (BrandingJobController.getJobAssets returns
      // ResponseEntity.ok(job.getAssetsGenerated())). NOTE: BE serves the raw
      // assetsGenerated JSON string, not a parsed array — separate BE-side quirk
      // flagged in GAP-1082 report (out of FE scope). Shape read is `data`, not data.data.
      const { data } = await apiClient.get<BrandingAsset[]>(
        endpoints.branding.jobAssets(jobId!)
      );
      return data;
    },
    enabled: !!jobId,
  });
}

/**
 * Post-deploy status summary for an instance (GAP-1108).
 *
 * Drives the deploy-success card on `/branding`: state (DEPLOYED) +
 * `frontendUrl` landing link + last-deploy summary. Bare `<T>` body
 * (LifecycleEventsController returns ResponseEntity.ok(dto)) — read `data`.
 */
export function useBrandingDeployStatus(instanceId: string | undefined) {
  return useQuery({
    queryKey: ['branding', 'deploy-status', instanceId],
    queryFn: async () => {
      const { data } = await apiClient.get<BrandingDeployStatus>(
        endpoints.brandingV1.instanceDeployStatus(instanceId!)
      );
      return data;
    },
    enabled: !!instanceId,
  });
}

/**
 * List all assets for instance
 */
export function useAssets(instanceId: string | undefined) {
  return useQuery({
    queryKey: ['branding', 'assets', instanceId],
    queryFn: async () => {
      // GAP-1082: bare List<BrandingAsset> body (AssetStorageController.getAssets
      // returns ResponseEntity.ok(assets)).
      const { data } = await apiClient.get<BrandingAsset[]>(
        endpoints.branding.listAssets(instanceId!)
      );
      return data;
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
      // GAP-1082: bare body (ContentGenerationController returns ResponseEntity.ok(content)).
      const { data } = await apiClient.post<MarketingContent>(
        endpoints.branding.generateContent,
        request
      );
      return data;
    },
  });
}
