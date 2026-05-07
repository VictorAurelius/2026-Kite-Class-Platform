/**
 * usePreviewBrandColors — Wave 34 derives wizard preview brand colors from
 * `BrandingJobResponse.brandColors` (GAP-272k).
 *
 * Replaces Wave 32 inline `TEMPLATE_TO_COLORS` constant. Underlying source
 * is `useBrandingJobV1` so the wrapper hook composes cleanly + the hook
 * caller doesn't have to know the wire shape.
 */

import { useMemo } from 'react';
import { useBrandingJobV1 } from './useBrandingJobV1';
import type { WizardBrandColors } from './types';

export interface UsePreviewBrandColorsResult {
  brandColors: WizardBrandColors | undefined;
  isLoading: boolean;
  isError: boolean;
}

export function usePreviewBrandColors(
  jobId: string | undefined
): UsePreviewBrandColorsResult {
  const query = useBrandingJobV1(jobId);

  const brandColors = useMemo(() => query.data?.brandColors, [query.data]);

  return {
    brandColors,
    isLoading: query.isLoading,
    isError: query.isError,
  };
}
