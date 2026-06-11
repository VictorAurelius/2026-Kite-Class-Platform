/**
 * BannerLivePreview — presentational Step 7 banner preview (GAP-1143).
 *
 * Presentational ONLY: the parent (Step 7) owns fetching via
 * `useBannerPreview` and passes `bannerUrl` / `isLoading` / `error` down.
 *
 * Renders a ~1200×630 (1.905:1) preview surface:
 *  - isLoading              → skeleton/spinner
 *  - bannerUrl present      → <img>
 *  - error OR null bannerUrl → fallback (logoFallbackUrl OR neutral
 *    placeholder with Vietnamese empty-state copy)
 */

'use client';

import { ImageOff, Loader2 } from 'lucide-react';

import { cn } from '@/lib/utils';

export interface BannerLivePreviewProps {
  bannerUrl: string | null;
  isLoading: boolean;
  error?: unknown;
  /** Shown in the fallback surface when no banner is available. */
  logoFallbackUrl?: string | null;
  className?: string;
}

// 1200×630 → enforce the same aspect ratio on the preview surface.
const ASPECT = '1200 / 630';

export function BannerLivePreview({
  bannerUrl,
  isLoading,
  error,
  logoFallbackUrl,
  className,
}: BannerLivePreviewProps) {
  return (
    <div
      data-testid="banner-live-preview"
      className={cn(
        'relative w-full overflow-hidden rounded-lg border border-border bg-muted',
        className
      )}
      style={{ aspectRatio: ASPECT }}
    >
      {isLoading ? (
        <div
          data-testid="banner-live-preview-loading"
          role="status"
          aria-live="polite"
          className="absolute inset-0 flex animate-pulse items-center justify-center bg-muted"
        >
          <Loader2
            className="h-8 w-8 animate-spin text-muted-foreground"
            aria-hidden="true"
          />
          <span className="sr-only">Đang tạo banner xem trước…</span>
        </div>
      ) : bannerUrl && !error ? (
        // eslint-disable-next-line @next/next/no-img-element -- preview surface, dynamic remote URL
        <img
          data-testid="banner-live-preview-image"
          src={bannerUrl}
          alt="Banner xem trước"
          className="h-full w-full object-cover"
        />
      ) : (
        <div
          data-testid="banner-live-preview-fallback"
          className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-muted px-4 text-center"
        >
          {logoFallbackUrl ? (
            // eslint-disable-next-line @next/next/no-img-element -- fallback logo, dynamic remote URL
            <img
              data-testid="banner-live-preview-logo-fallback"
              src={logoFallbackUrl}
              alt="Logo"
              className="max-h-1/2 max-w-1/2 object-contain opacity-80"
            />
          ) : (
            <ImageOff
              className="h-8 w-8 text-muted-foreground"
              aria-hidden="true"
            />
          )}
          <p className="text-sm text-muted-foreground">
            Chưa tạo được banner xem trước
          </p>
        </div>
      )}
    </div>
  );
}

export default BannerLivePreview;
