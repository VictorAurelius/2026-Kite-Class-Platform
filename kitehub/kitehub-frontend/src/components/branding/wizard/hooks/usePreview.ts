/**
 * usePreview — Wave 34 wizard preview iframe URL composer (GAP-272j).
 *
 * Replaces Wave 32 inline `buildMockPreviewUrl()` (`data:text/html` URI)
 * with a stable backend route. Endpoint per `api-contract.md` Wave 34:
 *   GET `/api/v1/branding/jobs/{jobId}/preview`
 * Returns `text/html` with X-Frame-Options: SAMEORIGIN — safe to embed
 * via `<iframe sandbox="allow-same-origin">`.
 *
 * Hook returns the URL string only — the iframe issues the HTTP request
 * itself (auth via cookies/Authorization on same-origin). Returns
 * `undefined` while jobId is missing so the iframe can render a
 * placeholder.
 */

import { useMemo } from 'react';
import { endpoints } from '@/lib/api/endpoints';

/**
 * Compose the iframe-safe preview URL for a given job.
 *
 * @param jobId BrandingJob id; undefined returns undefined so the
 *              wizard renders a placeholder while the job is being created.
 */
export function usePreview(jobId: string | undefined): string | undefined {
  return useMemo(() => {
    if (!jobId) return undefined;
    return endpoints.brandingV1.jobPreview(jobId);
  }, [jobId]);
}
