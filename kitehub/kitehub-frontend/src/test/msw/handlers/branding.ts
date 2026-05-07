/**
 * MSW handlers for AI Branding wizard endpoints (Wave 34).
 *
 * Bucket 0 Foundation ships this as an empty placeholder. Bucket D
 * (FE Refactor) will populate handlers matching the schema in
 * `documents/01-business/kitehub/ai-branding/api-contract.md` Wave 34
 * additions:
 *   - GET    /api/v1/branding/slug-availability
 *   - GET    /api/v1/branding/regenerate-quota
 *   - POST   /api/v1/branding/jobs/{jobId}/regenerate
 *   - GET    /api/v1/branding/jobs/{jobId}/deploy-stream  (SSE)
 *   - GET    /api/v1/branding/jobs/{jobId}/quality-score
 *   - GET    /api/v1/branding/jobs/{jobId}/preview        (HTML)
 *   - GET    /api/v1/branding/instances/{instanceId}/lifecycle/events
 *   - GET    /api/v1/branding/jobs/{jobId}                (extended w/ brandColors)
 *
 * @author KiteHub Team
 * @since Wave 34 Bucket 0
 */

import type { HttpHandler } from 'msw';

export const brandingHandlers: HttpHandler[] = [];
