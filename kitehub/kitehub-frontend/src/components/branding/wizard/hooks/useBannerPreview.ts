/**
 * useBannerPreview — on-demand Step 7 banner live-preview (GAP-1143).
 *
 * Calls `POST /api/v1/branding/jobs/preview-banner` via the shared
 * `apiClient` (Authorization + X-Tenant-Id injected by the request
 * interceptor). Preview is on-demand — a parent (Step 7) triggers
 * `generate(request)` on mount and on relevant change, then reads
 * `bannerUrl`.
 *
 * Phase 1 BETA: backend renders a TEMPLATE-mode composed banner (no
 * full AI), so `mode` is always `'TEMPLATE'` and `bannerUrl` may be
 * null when compose has nothing to render yet.
 *
 * GAP-1082: kitehub-branding returns a BARE `<T>` body (no ApiResponse
 * wrapper) — read `data` directly, NOT `data.data`.
 */

import { useMutation } from '@tanstack/react-query';

import apiClient from '@/lib/api/client';

/** Five-slot brand palette fed to the banner composer. */
export interface PreviewBannerColours {
  primary: string;
  secondary: string;
  accent: string;
  neutral: string;
  background: string;
}

export interface PreviewBannerRequest {
  organizationName: string;
  copy?: string;
  logoUrl?: string | null;
  portraitUrls?: string[];
  themeIcon?: string;
  colours: PreviewBannerColours;
  /**
   * GAP-1147 — requested generation mode. Omitted / `'TEMPLATE'` = free preview
   * (never burns quota). `'FULL_AI'` = PREMIUM/ENTERPRISE on-demand AI banner;
   * the BACKEND tier-gates + meters it and falls back to TEMPLATE when the caller
   * is ineligible or out of quota (see {@link PreviewBannerResponse.fallbackReason}).
   */
  mode?: 'TEMPLATE' | 'FULL_AI';
}

export interface PreviewBannerResponse {
  /** Composed banner URL, or null when nothing renderable yet. */
  bannerUrl: string | null;
  /** Effective generation mode the backend resolved (after tier/quota gating). */
  mode: 'TEMPLATE' | 'FULL_AI';
  /**
   * GAP-1147 — present only when a FULL_AI request was downgraded to TEMPLATE:
   * `'TIER_NOT_ELIGIBLE'` (FREE/BASIC) or `'QUOTA_EXHAUSTED'` (PREMIUM cap spent).
   */
  fallbackReason?: 'TIER_NOT_ELIGIBLE' | 'QUOTA_EXHAUSTED';
}

// Relative path (matches the brandingV1 endpoint convention); baseURL is
// applied by apiClient (gateway :9000 in dev, NEXT_PUBLIC_API_URL in prod).
const PREVIEW_BANNER_ENDPOINT = '/api/v1/branding/jobs/preview-banner';

export interface UseBannerPreviewResult {
  /** Latest composed banner URL (null until a successful generate). */
  bannerUrl: string | null;
  /** Generation mode of the latest response (null before first generate). */
  mode: 'TEMPLATE' | 'FULL_AI' | null;
  /** True while a preview request is in flight (React Query v5 mutation). */
  isPending: boolean;
  /** Alias of {@link isPending} for call-site ergonomics. */
  isLoading: boolean;
  /** Mutation error (unknown shape — surfaced to BannerLivePreview). */
  error: unknown;
  /**
   * Trigger a preview; resolves with the response so callers can await. Pass
   * `tier` (subscription tier) for FULL_AI requests so the backend can gate +
   * meter — it is sent as the `X-Subscription-Tier` header (GAP-1147).
   */
  generate: (
    req: PreviewBannerRequest,
    tier?: string
  ) => Promise<PreviewBannerResponse>;
  /** Reset back to the idle state (clears bannerUrl + error). */
  reset: () => void;
}

interface GenerateVars {
  req: PreviewBannerRequest;
  tier?: string;
}

/**
 * On-demand banner preview hook. Ergonomic surface: call
 * `generate(request)` and read `bannerUrl` / `isLoading` / `error`.
 */
export function useBannerPreview(): UseBannerPreviewResult {
  const mutation = useMutation<PreviewBannerResponse, unknown, GenerateVars>({
    mutationKey: ['branding', 'preview-banner'],
    mutationFn: async ({ req, tier }: GenerateVars) => {
      const { data } = await apiClient.post<PreviewBannerResponse>(
        PREVIEW_BANNER_ENDPOINT,
        req,
        // GAP-1147: FULL_AI requests carry the tier so the BE can enforce the
        // PREMIUM/ENTERPRISE gate + quota; TEMPLATE previews omit it.
        tier ? { headers: { 'X-Subscription-Tier': tier } } : undefined
      );
      return data;
    },
  });

  return {
    bannerUrl: mutation.data?.bannerUrl ?? null,
    mode: mutation.data?.mode ?? null,
    isPending: mutation.isPending,
    isLoading: mutation.isPending,
    error: mutation.error,
    generate: (req: PreviewBannerRequest, tier?: string) =>
      mutation.mutateAsync({ req, tier }),
    reset: mutation.reset,
  };
}
